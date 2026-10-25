package maxigregrze.cobblesafari.objectives;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import kotlin.Unit;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.compat.CobblemonFishingInterop;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.data.DungeonInstanceRecord;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.data.HyperspaceZoneSavedData;
import maxigregrze.cobblesafari.dungeon.hyperspace.HyperspaceZone;
import maxigregrze.cobblesafari.data.ObjectivesSavedData;
import maxigregrze.cobblesafari.network.ObjectivesHudSyncPayload;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side façade for the dimensional objectives system: event hooks, rolling,
 * per-tick context detection, daily reset, instance purge, and reward/HUD syncing.
 */
public final class ObjectivesManager {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final int OBJECTIVE_COUNT = 3;
    /** Distortion dungeon dimension, for the "Shadow Hunter" advancement. */
    private static final ResourceLocation DISTORTION_DIM =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_distortion");
    private static final String STAR = "★";

    /** Last known assignment key per player (absent ⇒ no objectives context). */
    private static final Map<UUID, String> CURRENT_KEY = new ConcurrentHashMap<>();

    /** Sentinel stored when a player's context resolution threw, so we don't retry it every tick. */
    private static final String MARKER_FAILED = "\0failed";

    /** Failsafe sites already logged, to avoid per-tick log spam. */
    private static final Set<String> LOGGED_SITES = ConcurrentHashMap.newKeySet();

    private ObjectivesManager() {}

    /**
     * Logs the first error per {@code site} only. The objectives system must never crash a server
     * tick or a Cobblemon event because a dimension/instance has no resolvable task set.
     */
    private static void logOnce(String site, Exception e) {
        if (LOGGED_SITES.add(site)) {
            CobbleSafari.LOGGER.error("[Objectives] failsafe caught an error in {} (further occurrences suppressed)", site, e);
        }
    }

    // --- Registration ---------------------------------------------------------

    public static void register() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            try {
                onCapture(event);
            } catch (Exception e) {
                logOnce("onCapture", e);
            }
            return Unit.INSTANCE;
        });
        CatchContextTracker.register();
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            try {
                onBattleVictory(event);
            } catch (Exception e) {
                logOnce("onBattleVictory", e);
            }
            return Unit.INSTANCE;
        });
        CobbleSafari.LOGGER.info("CobbleSafari >> Dimensional objectives manager registered!");
    }

    // --- Context detection (entry into a dimension/instance) -------------------

    /** Per-tick recompute of every player's assignment key; fires on change. */
    public static void tickContext(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                String newKey = computeCurrentKey(player);
                String old = CURRENT_KEY.get(player.getUUID());
                if (!Objects.equals(newKey, old)) {
                    if (newKey == null) {
                        CURRENT_KEY.remove(player.getUUID());
                    } else {
                        CURRENT_KEY.put(player.getUUID(), newKey);
                    }
                    onContextEntered(player, newKey);
                }
            } catch (Exception e) {
                // A single player/instance with no resolvable task set must never stall the tick.
                CURRENT_KEY.put(player.getUUID(), MARKER_FAILED);
                logOnce("tickContext", e);
            }
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        CURRENT_KEY.remove(uuid);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        String key = computeCurrentKey(player);
        if (key == null) {
            // CURRENT_KEY is empty on join, so the V4 sticky net cannot help a player who logged out
            // outside the resolvable footprint. The persisted assignments still answer the question.
            key = recoverKeyOnJoin(player);
        }
        if (key != null) {
            CURRENT_KEY.put(player.getUUID(), key);
        }
        onContextEntered(player, key);
    }

    /**
     * Join-time context recovery (action plan 179, § 4.2): adopt the player's stored assignment for
     * this dimension when <em>exactly one</em> of them still points at a live instance. Two live
     * candidates mean the player visited two simultaneous instances — picking one would credit
     * progress to the wrong dungeon, so we give up instead.
     */
    private static String recoverKeyOnJoin(ServerPlayer player) {
        ResourceLocation dim = player.level().dimension().location();
        DimensionalObjectivesDefinition def = DimensionalObjectivesRegistry.get(dim);
        if (def == null || !def.isInstanciated()) {
            return null;
        }
        DungeonPositionSavedData positions = DungeonPositionSavedData.get(player.getServer());
        String dimensionId = dim.toString();
        String found = null;
        for (ObjectiveAssignment assignment : ObjectivesSavedData.get(player.getServer())
                .getForPlayer(player.getUUID())) {
            if (assignment.instanceId() == null || !dimensionId.equals(assignment.dimensionId())) {
                continue;
            }
            DungeonInstanceRecord live = positions.getInstance(assignment.instanceId());
            if (live == null || live.pendingDeletion()) {
                continue;
            }
            if (found != null) {
                return null; // ambiguous: two live instances for this player in this dimension
            }
            found = assignment.key();
        }
        return found;
    }

    private static void onContextEntered(ServerPlayer player, String newKey) {
        if (newKey == null) {
            Services.PLATFORM.sendPayloadToPlayer(player, ObjectivesHudSyncPayload.hidden());
            return;
        }
        ObjectivesSavedData data = ObjectivesSavedData.get(player.getServer());
        ObjectiveAssignment assignment = data.get(player.getUUID(), newKey);
        if (assignment == null) {
            assignment = rollAssignment(player, newKey);
            if (assignment == null) {
                Services.PLATFORM.sendPayloadToPlayer(player, ObjectivesHudSyncPayload.hidden());
                return;
            }
            data.put(player.getUUID(), assignment);
            announceHyperspaceZone(player, assignment);
        }
        sync(player, assignment);
    }

    /** Resolves the player's current assignment key, or {@code null} (no objectives here). */
    private static String computeCurrentKey(ServerPlayer player) {
        ResourceLocation dim = player.level().dimension().location();
        DimensionalObjectivesDefinition def = DimensionalObjectivesRegistry.get(dim);
        if (def == null) {
            return null;
        }
        if (!def.isInstanciated()) {
            return ObjectiveAssignment.keyOf(dim.toString(), null);
        }
        DungeonPositionSavedData data = DungeonPositionSavedData.get(player.getServer());
        DungeonInstanceRecord record = data.findInstanceByPosition(dim.toString(), player.blockPosition());
        if (record != null) {
            return ObjectiveAssignment.keyOf(dim.toString(), record.instanceId());
        }
        return stickyKey(player, dim, data);
    }

    /**
     * Failsafe (action plan 179, V4): while the player has not left the dimension and their last known
     * instance is still live, keep the previous key even though the position no longer resolves.
     * Losing the key silently voids progress, so a stale-but-live key is strictly better than none.
     */
    private static String stickyKey(ServerPlayer player, ResourceLocation dim, DungeonPositionSavedData data) {
        String last = CURRENT_KEY.get(player.getUUID());
        if (last == null || MARKER_FAILED.equals(last)) {
            return null;
        }
        int sep = last.indexOf(ObjectiveAssignment.INSTANCE_SEP);
        if (sep < 0 || !dim.toString().equals(last.substring(0, sep))) {
            return null; // different dimension (or a non-instanced key): never sticky
        }
        UUID instanceId;
        try {
            instanceId = UUID.fromString(last.substring(sep + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
        DungeonInstanceRecord live = data.getInstance(instanceId);
        return (live != null && !live.pendingDeletion()) ? last : null;
    }

    private static ObjectiveAssignment activeAssignment(ServerPlayer player) {
        String key = computeCurrentKey(player);
        if (key == null) {
            return null;
        }
        return ObjectivesSavedData.get(player.getServer()).get(player.getUUID(), key);
    }

    // --- Rolling --------------------------------------------------------------

    private static ObjectiveAssignment rollAssignment(ServerPlayer player, String key) {
        int sep = key.indexOf(ObjectiveAssignment.INSTANCE_SEP);
        String dimensionId = sep < 0 ? key : key.substring(0, sep);
        UUID instanceId = null;
        if (sep >= 0) {
            try {
                instanceId = UUID.fromString(key.substring(sep + 1));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        ResourceLocation dim = ResourceLocation.tryParse(dimensionId);
        if (dim == null) {
            return null;
        }
        DimensionalObjectivesDefinition def = DimensionalObjectivesRegistry.get(dim);
        if (def == null || def.tasks().isEmpty()) {
            return null;
        }
        try {
            HyperspaceZone zone = instanceId == null ? null
                    : HyperspaceZoneSavedData.get(player.getServer()).get(instanceId);
            // An entry aimed at the instance's roll cannot be satisfied without one (plan 196, § 7.4).
            List<DimensionalObjectivesDefinition.TaskPoolEntry> pool = new ArrayList<>();
            for (DimensionalObjectivesDefinition.TaskPoolEntry entry : def.tasks()) {
                if (zone != null || !entry.needsInstanceContext()) {
                    pool.add(entry);
                }
            }
            if (pool.isEmpty()) {
                return null;
            }
            RandomSource rng = player.serverLevel().getRandom();
            long now = player.serverLevel().getGameTime();
            List<ObjectiveTask> tasks = new ArrayList<>(OBJECTIVE_COUNT);
            for (int i = 0; i < OBJECTIVE_COUNT; i++) {
                ObjectiveTask rolled = rollOne(pool, zone, rng, now);
                if (rolled == null) {
                    return null;
                }
                tasks.add(rolled);
            }
            return new ObjectiveAssignment(dimensionId, instanceId, tasks);
        } catch (Exception e) {
            logOnce("rollAssignment", e);
            return null;
        }
    }

    private static ObjectiveTask rollOne(List<DimensionalObjectivesDefinition.TaskPoolEntry> pool,
                                         HyperspaceZone zone, RandomSource rng, long now) {
        DimensionalObjectivesDefinition.TaskPoolEntry entry = pickWeighted(pool, rng);
        if (entry == null || entry.type() == null) {
            return null;
        }
        TaskType type = entry.type();

        int count = type.usesCount()
                ? entry.countMin() + rng.nextInt(entry.countMax() - entry.countMin() + 1)
                : 1;
        String species = type.usesSpecies() ? rollSpecies(entry, zone, rng) : "";
        int typeIndex = type.usesType() ? rollTypeIndex(entry, zone, rng) : -1;
        String reward = entry.taskReward() != null ? entry.taskReward().toString() : "";

        ObjectiveTask task = new ObjectiveTask(type.id(), count, species, typeIndex, reward);
        if (guaranteesSpawn(type)) {
            // Armed at once: the next Pokemon to spawn for the player in this instance (plan 196, section 7.8).
            task.armSpawnGuarantee(now);
        }
        return task;
    }

    private static DimensionalObjectivesDefinition.TaskPoolEntry pickWeighted(
            List<DimensionalObjectivesDefinition.TaskPoolEntry> pool, RandomSource rng) {
        if (pool.isEmpty()) {
            return null;
        }
        int total = 0;
        for (DimensionalObjectivesDefinition.TaskPoolEntry e : pool) {
            total += e.weight();
        }
        int roll = rng.nextInt(Math.max(1, total));
        int sum = 0;
        for (DimensionalObjectivesDefinition.TaskPoolEntry e : pool) {
            sum += e.weight();
            if (roll < sum) {
                return e;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private static String rollSpecies(DimensionalObjectivesDefinition.TaskPoolEntry entry,
                                      HyperspaceZone zone, RandomSource rng) {
        String chosen = entry.allowedSpecies().get(rng.nextInt(entry.allowedSpecies().size()));
        if (DimensionalObjectivesDefinition.TaskPoolEntry.INSTANCE_SENTINEL.equalsIgnoreCase(chosen)) {
            List<String> targets = zone == null ? List.of() : zone.targetSpecies();
            return targets.isEmpty() ? "" : targets.get(rng.nextInt(targets.size()));
        }
        if ("random".equalsIgnoreCase(chosen)) {
            List<Species> implemented = new ArrayList<>(PokemonSpecies.INSTANCE.getImplemented());
            if (implemented.isEmpty()) {
                return "";
            }
            return implemented.get(rng.nextInt(implemented.size())).getResourceIdentifier().toString();
        }
        return normalizeSpeciesId(chosen);
    }

    private static int rollTypeIndex(DimensionalObjectivesDefinition.TaskPoolEntry entry,
                                     HyperspaceZone zone, RandomSource rng) {
        if (entry.typesFromInstance() && zone != null && !zone.types().isEmpty()) {
            return DimensionalObjectivesDataLoader.typeIndexFromName(zone.types().get(rng.nextInt(zone.types().size())));
        }
        return entry.allowedTypes().get(rng.nextInt(entry.allowedTypes().size()));
    }

    /** "Hyperspace Lumiose ★★★ — Fire / Water", once per freshly rolled assignment (plan 196, § 3.7). */
    private static void announceHyperspaceZone(ServerPlayer player, ObjectiveAssignment assignment) {
        if (assignment.instanceId() == null) {
            return;
        }
        HyperspaceZone zone = HyperspaceZoneSavedData.get(player.getServer()).get(assignment.instanceId());
        if (zone == null) {
            return;
        }
        MutableComponent types = Component.empty();
        for (int i = 0; i < zone.types().size(); i++) {
            if (i > 0) {
                types.append(" / ");
            }
            types.append(Component.translatable("cobblemon.type." + zone.types().get(i)));
        }
        player.sendSystemMessage(Component.translatable("cobblesafari.hyperspace.zone_info",
                STAR.repeat(zone.star()), types));
    }

    private static String normalizeSpeciesId(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return s.indexOf(':') >= 0 ? s : "cobblemon:" + s;
    }

    // --- Daily reset (non-instanced) ------------------------------------------

    public static void tickDailyScheduler(MinecraftServer server) {
        try {
            ObjectivesSavedData data = ObjectivesSavedData.get(server);
            long todayEpoch = LocalDate.now(ZONE).toEpochDay();
            long last = data.getLastDailyResetEpochDay();
            if (last < 0) {
                data.setLastDailyResetEpochDay(todayEpoch - 1);
                return;
            }
            int rh = MiscConfig.getDailySystemResetHour();
            boolean pastResetHour = LocalTime.now(ZONE).getHour() >= rh;
            boolean shouldRun = (todayEpoch > last && pastResetHour) || (todayEpoch > last + 1);
            if (!shouldRun) {
                return;
            }
            runDailyReset(server);
            data.setLastDailyResetEpochDay(todayEpoch);
        } catch (Exception e) {
            logOnce("tickDailyScheduler", e);
        }
    }

    /** Forces a full daily reset of the (non-instanced) dimensional objectives — admin command. */
    public static void forceDailyReset(MinecraftServer server) {
        try {
            runDailyReset(server);
        } catch (Exception e) {
            logOnce("forceDailyReset", e);
        }
    }

    private static void runDailyReset(MinecraftServer server) {
        ObjectivesSavedData data = ObjectivesSavedData.get(server);
        Set<String> kept = new HashSet<>();
        // Players currently in a non-instanced objectives dimension: delete + reroll instantly.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String key = computeCurrentKey(player);
            if (key == null || key.indexOf(ObjectiveAssignment.INSTANCE_SEP) >= 0) {
                continue;
            }
            data.remove(player.getUUID(), key);
            ObjectiveAssignment rolled = rollAssignment(player, key);
            if (rolled != null) {
                data.put(player.getUUID(), rolled);
                kept.add(player.getUUID() + "|" + key);
                sync(player, rolled);
            }
        }
        // All other non-instanced assignments: delete without reroll.
        for (ObjectivesSavedData.PlayerAssignment pa : data.getNonInstancedAssignments()) {
            String composite = pa.player() + "|" + pa.assignment().key();
            if (!kept.contains(composite)) {
                data.remove(pa.player(), pa.assignment().key());
            }
        }
    }

    /** Removes every player's assignment for a dungeon instance. */
    public static void purgeInstance(MinecraftServer server, String dimensionId, UUID instanceId) {
        String key = ObjectiveAssignment.keyOf(dimensionId, instanceId);
        ObjectivesSavedData.get(server).removeKeyForAll(key);
    }

    // --- Spawn guarantee consumption ------------------------------------------

    private static boolean guaranteesSpawn(TaskType type) {
        return type == TaskType.TASK_CATCH_SHINY || type == TaskType.TASK_CATCH_ALPHA
                || type == TaskType.TASK_CATCH_ALPHASHINY;
    }

    /**
     * Spends at most one pending guarantee on a fresh spawn. Bound to the active assignment, so it
     * only ever fires inside its own instance and simply waits while the player is elsewhere.
     */
    public static void tryConsumeSpawnGuarantee(ServerPlayer player, PokemonEntity pe) {
        try {
            consumeSpawnGuaranteeInternal(player, pe);
        } catch (Exception e) {
            logOnce("tryConsumeSpawnGuarantee", e);
        }
    }

    private static void consumeSpawnGuaranteeInternal(ServerPlayer player, PokemonEntity pe) {
        ObjectiveAssignment assignment = activeAssignment(player);
        if (assignment == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        for (ObjectiveTask task : assignment.tasks()) {
            if (task.isComplete() || task.type() == null || !guaranteesSpawn(task.type())
                    || !task.isSpawnGuaranteePending() || now < task.spawnGuaranteeTriggerGameTime()) {
                continue;
            }
            if (task.type() != TaskType.TASK_CATCH_ALPHA) {
                pe.getPokemon().setShiny(true);
            }
            if (task.type() != TaskType.TASK_CATCH_SHINY && !pe.getPokemon().isAlpha()) {
                CobblemonFishingInterop.alterAlphaAttempt(pe);
            }
            task.consumeSpawnGuarantee();
            ObjectivesSavedData.get(player.getServer()).setDirty();
            return;
        }
    }

    // --- Event hooks ----------------------------------------------------------

    private static void onCapture(PokemonCapturedEvent event) {
        ServerPlayer player = event.getPlayer();
        Pokemon mon = event.getPokemon();
        String species = mon.getSpecies().getResourceIdentifier().toString();
        boolean shiny = mon.getShiny();
        Boolean inBattle = CatchContextTracker.consume(mon.getUuid());
        runEvent(player, task -> captureMutation(task, mon, species, shiny, inBattle));
    }

    private static MutationResult captureMutation(ObjectiveTask task, Pokemon mon, String species, boolean shiny,
                                                  Boolean inBattle) {
        return switch (task.type()) {
            case TASK_CATCH_BASE -> new MutationResult(true, task.addProgress(1));
            case TASK_CATCH_SPECIE -> speciesMatches(species, task.speciesId())
                    ? new MutationResult(true, task.addProgress(1)) : MutationResult.NONE;
            case TASK_CATCH_TYPE -> PowerVariantRegistry.pokemonHasVariantType(mon, task.typeIndex())
                    ? new MutationResult(true, task.addProgress(1)) : MutationResult.NONE;
            case TASK_CATCH_UNIQUE -> uniqueMutation(task, species);
            case TASK_CATCH_SHINY -> shiny
                    ? new MutationResult(true, task.markBinaryComplete()) : MutationResult.NONE;
            case TASK_CATCH_ALPHA -> mon.isAlpha()
                    ? new MutationResult(true, task.markBinaryComplete()) : MutationResult.NONE;
            case TASK_CATCH_ALPHASHINY -> (mon.isAlpha() && shiny)
                    ? new MutationResult(true, task.markBinaryComplete()) : MutationResult.NONE;
            case TASK_CATCH_UNAWARE -> Boolean.FALSE.equals(inBattle)
                    ? new MutationResult(true, task.addProgress(1)) : MutationResult.NONE;
            case TASK_CATCH_BATTLE -> Boolean.TRUE.equals(inBattle)
                    ? new MutationResult(true, task.addProgress(1)) : MutationResult.NONE;
            default -> MutationResult.NONE;
        };
    }

    private static void onBattleVictory(BattleVictoryEvent event) {
        if (event.getWasWildCapture()) {
            return; // capture excluded from "beat" tasks
        }
        List<Pokemon> wild = new ArrayList<>();
        for (BattleActor loser : event.getLosers()) {
            if (loser.getType() == ActorType.WILD) {
                for (BattlePokemon bp : loser.getPokemonList()) {
                    wild.add(bp.getOriginalPokemon());
                }
            }
        }
        if (wild.isEmpty()) {
            return;
        }
        for (BattleActor winner : event.getWinners()) {
            if (winner.getType() != ActorType.PLAYER) {
                continue;
            }
            ServerPlayer player = resolvePlayer(winner);
            if (player != null) {
                runEvent(player, task -> fightMutation(task, wild));
                if (player.level().dimension().location().equals(DISTORTION_DIM)) {
                    int defeated = maxigregrze.cobblesafari.init.ModStats.addAndGet(
                            player, maxigregrze.cobblesafari.init.ModStats.DISTORTION_POKEMON_DEFEATED, wild.size());
                    maxigregrze.cobblesafari.advancement.ModCriteria.DISTORTION_DEFEAT.trigger(player, defeated);
                }
            }
        }
    }

    private static MutationResult fightMutation(ObjectiveTask task, List<Pokemon> wild) {
        switch (task.type()) {
            case TASK_FIGHT_BASE:
                return new MutationResult(true, task.addProgress(wild.size()));
            case TASK_FIGHT_SPECIE: {
                int n = 0;
                for (Pokemon p : wild) {
                    if (speciesMatches(idOf(p), task.speciesId())) {
                        n++;
                    }
                }
                return n > 0 ? new MutationResult(true, task.addProgress(n)) : MutationResult.NONE;
            }
            case TASK_FIGHT_TYPE: {
                int n = 0;
                for (Pokemon p : wild) {
                    if (PowerVariantRegistry.pokemonHasVariantType(p, task.typeIndex())) {
                        n++;
                    }
                }
                return n > 0 ? new MutationResult(true, task.addProgress(n)) : MutationResult.NONE;
            }
            case TASK_FIGHT_ALPHA: {
                int n = 0;
                for (Pokemon p : wild) {
                    if (p.isAlpha()) {
                        n++;
                    }
                }
                return n > 0 ? new MutationResult(true, task.addProgress(n)) : MutationResult.NONE;
            }
            case TASK_FIGHT_UNIQUE: {
                boolean anyNew = false;
                boolean justCompleted = false;
                for (Pokemon p : wild) {
                    String s = idOf(p);
                    if (!task.hasSeenSpecies(s)) {
                        anyNew = true;
                    }
                    if (task.recordUniqueSpecies(s)) {
                        justCompleted = true;
                    }
                }
                return anyNew ? new MutationResult(true, justCompleted) : MutationResult.NONE;
            }
            default:
                return MutationResult.NONE;
        }
    }

    private static MutationResult uniqueMutation(ObjectiveTask task, String species) {
        boolean wasNew = !task.hasSeenSpecies(species);
        boolean justCompleted = task.recordUniqueSpecies(species);
        return wasNew ? new MutationResult(true, justCompleted) : MutationResult.NONE;
    }

    public static void onPokeballOpened(ServerPlayer player) {
        simpleEvent(player, TaskType.TASK_LOOT, 1, false);
    }

    public static void onFloatingBallOpened(ServerPlayer player) {
        simpleEvent(player, TaskType.TASK_LOOT_FLOATING, 1, false);
    }

    /** One progress per use, however many obstacles the diamond cleared (plan 196, § 8.6). */
    public static void onObstacleCleared(ServerPlayer player, TaskType task) {
        simpleEvent(player, task, 1, false);
    }

    public static void onBossStart(ServerPlayer player) {
        simpleEvent(player, TaskType.TASK_CSBOSS_BASE, 0, true);
    }

    public static void onBossWin(ServerPlayer player) {
        simpleEvent(player, TaskType.TASK_CSBOSS_WIN, 0, true);
    }

    public static void onExcavationFinished(ServerPlayer player) {
        simpleEvent(player, TaskType.TASK_UNDERGROUND_BASE, 1, false);
    }

    public static void onExcavationPerfect(ServerPlayer player) {
        simpleEvent(player, TaskType.TASK_UNDERGROUND_PERFECT, 0, true);
    }

    public static void onCsTraderTrade(ServerPlayer player) {
        simpleEvent(player, TaskType.TASK_CSTRADER_BASE, 0, true);
    }

    public static void onGiratinaTrade(ServerPlayer player) {
        simpleEvent(player, TaskType.TASK_GIRATINACORE_BASE, 0, true);
    }

    private static void simpleEvent(ServerPlayer player, TaskType target, int amount, boolean binary) {
        runEvent(player, task -> {
            if (task.type() != target) {
                return MutationResult.NONE;
            }
            boolean justCompleted = binary ? task.markBinaryComplete() : task.addProgress(amount);
            return new MutationResult(true, justCompleted);
        });
    }

    // --- Core event application -----------------------------------------------

    private static void runEvent(ServerPlayer player, TaskMutator mutator) {
        try {
            runEventInternal(player, mutator);
        } catch (Exception e) {
            logOnce("runEvent", e);
        }
    }

    private static void runEventInternal(ServerPlayer player, TaskMutator mutator) {
        ObjectiveAssignment assignment = activeAssignment(player);
        if (assignment == null) {
            return;
        }
        boolean changed = false;
        List<ObjectiveTask> justCompleted = new ArrayList<>();
        for (ObjectiveTask task : assignment.tasks()) {
            // Skip already-complete tasks and any task whose id no longer maps to a known type
            // (e.g. a stale assignment from an older build) — never let a null type reach a switch.
            if (task.isComplete() || task.type() == null) {
                continue;
            }
            MutationResult result = mutator.apply(task);
            if (result.progressed()) {
                changed = true;
            }
            if (result.justCompleted()) {
                justCompleted.add(task);
            }
        }
        if (!changed) {
            return;
        }
        ObjectivesSavedData data = ObjectivesSavedData.get(player.getServer());
        for (ObjectiveTask task : justCompleted) {
            ObjectivesRewardService.giveTaskReward(player, task);
        }
        maybeFinalize(player, assignment);
        data.setDirty();
        sync(player, assignment);
    }

    private static void maybeFinalize(ServerPlayer player, ObjectiveAssignment assignment) {
        if (!assignment.allComplete() || assignment.isFinalRewardGiven()) {
            return;
        }
        ResourceLocation dim = ResourceLocation.tryParse(assignment.dimensionId());
        DimensionalObjectivesDefinition def = dim == null ? null : DimensionalObjectivesRegistry.get(dim);
        if (def != null) {
            ObjectivesRewardService.giveFinalRewards(player, def);
        }
        assignment.markFinalRewardGiven();
    }

    // --- HUD sync -------------------------------------------------------------

    private static void sync(ServerPlayer player, ObjectiveAssignment assignment) {
        List<ObjectivesHudSyncPayload.ObjectiveView> views = new ArrayList<>();
        for (ObjectiveTask task : assignment.tasks()) {
            views.add(new ObjectivesHudSyncPayload.ObjectiveView(
                    task.taskId(), task.targetCount(), task.progress(),
                    task.isComplete(), task.speciesId(), task.typeIndex()));
        }
        Services.PLATFORM.sendPayloadToPlayer(player, new ObjectivesHudSyncPayload(true, views));
    }

    // --- Helpers --------------------------------------------------------------

    private static ServerPlayer resolvePlayer(BattleActor actor) {
        if (actor instanceof EntityBackedBattleActor<?> backed && backed.getEntity() instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
    }

    private static String idOf(Pokemon pokemon) {
        return pokemon.getSpecies().getResourceIdentifier().toString();
    }

    private static boolean speciesMatches(String capturedId, String targetId) {
        if (targetId == null || targetId.isEmpty()) {
            return false;
        }
        return normalizeSpeciesId(capturedId).equalsIgnoreCase(normalizeSpeciesId(targetId));
    }

    private interface TaskMutator {
        MutationResult apply(ObjectiveTask task);
    }

    private record MutationResult(boolean progressed, boolean justCompleted) {
        private static final MutationResult NONE = new MutationResult(false, false);
    }
}
