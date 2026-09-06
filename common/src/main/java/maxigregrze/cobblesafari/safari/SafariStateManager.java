package maxigregrze.cobblesafari.safari;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.effect.RedShackledEffects;
import maxigregrze.cobblesafari.init.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SafariStateManager {

    private static final Map<UUID, SafariPokemonState> STATES = new ConcurrentHashMap<>();
    private static final List<ScheduledTask> SCHEDULED_TASKS = Collections.synchronizedList(new ArrayList<>());
    private static long currentServerTick = 0;

    /** Floor applied to the grace period replanned once a shackle expires, so the window stays playable. */
    private static final int MIN_RESUMED_GRACE_TICKS = 20;

    private SafariStateManager() {}

    public static void onServerTick(MinecraftServer server) {
        currentServerTick++;

        List<ScheduledTask> tasksToExecute = null;
        synchronized (SCHEDULED_TASKS) {
            Iterator<ScheduledTask> iterator = SCHEDULED_TASKS.iterator();
            while (iterator.hasNext()) {
                ScheduledTask task = iterator.next();
                if (currentServerTick >= task.executionTick) {
                    if (tasksToExecute == null) {
                        tasksToExecute = new ArrayList<>();
                    }
                    tasksToExecute.add(task);
                    iterator.remove();
                }
            }
        }

        if (tasksToExecute != null) {
            for (ScheduledTask task : tasksToExecute) {
                task.action.run();
            }
        }

        // Once a second is ample for a 30s rage, and keeps the per-tick cost off the state map.
        if (currentServerTick % 20 == 0) {
            tickRageDeadlines(server);
        }

        // Drop state for Pokémon whose entity is gone (caught / despawned / unloaded) so the
        // map cannot grow without bound on long-running servers.
        if (currentServerTick % 1200 == 0 && !STATES.isEmpty()) {
            STATES.keySet().removeIf(id -> !isEntityPresent(server, id));
        }
    }

    private static boolean isEntityPresent(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(id) != null) {
                return true;
            }
        }
        return false;
    }

    private static class ScheduledTask {
        final long executionTick;
        final Runnable action;

        ScheduledTask(long executionTick, Runnable action) {
            this.executionTick = executionTick;
            this.action = action;
        }
    }

    public static SafariPokemonState getOrCreate(UUID entityId) {
        return STATES.computeIfAbsent(entityId, SafariPokemonState::new);
    }

    public static SafariPokemonState getState(UUID entityId) {
        return STATES.get(entityId);
    }

    public static void remove(UUID entityId) {
        STATES.remove(entityId);
    }

    public static boolean hasState(UUID entityId) {
        return STATES.containsKey(entityId);
    }

    public static boolean isInSafariDimension(PokemonEntity pokemonEntity) {
        if (!(pokemonEntity.level() instanceof ServerLevel serverLevel)) return false;
        ResourceLocation currentDimension = serverLevel.dimension().location();
        return currentDimension.equals(SafariTimerConfig.getSafariDimensionRL());
    }

    public static void applyMudBall(PokemonEntity pokemonEntity) {
        SafariPokemonState state = getOrCreate(pokemonEntity.getUUID());
        int applied = state.applyMudBall();
        if (applied > 0 && pokemonEntity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    pokemonEntity.getX(), pokemonEntity.getY() + pokemonEntity.getBbHeight() + 0.5,
                    pokemonEntity.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }
    }

    public static void applyBait(PokemonEntity pokemonEntity) {
        SafariPokemonState state = getOrCreate(pokemonEntity.getUUID());
        int applied = state.applyBait();
        if (applied > 0 && pokemonEntity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    pokemonEntity.getX(), pokemonEntity.getY() + pokemonEntity.getBbHeight() + 0.5,
                    pokemonEntity.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }
    }

    public static void tryFlee(PokemonEntity pokemonEntity) {
        if (!isInSafariDimension(pokemonEntity)) return;
        if (isShackleProtected(pokemonEntity)) return;

        // An alpha does not run. Mud balls and failed captures make it angry instead.
        if (canEnrage(pokemonEntity)) {
            tryEnrage(pokemonEntity);
            return;
        }

        UUID entityId = pokemonEntity.getUUID();
        SafariPokemonState state = getOrCreate(entityId);
        if (state.isFleeing()) return;

        if (pokemonEntity.getPokemon().getShiny() && !SafariConfig.canShinyFlee()) return;

        int actualFleeRate = state.getActualFleeRate();
        int roll = pokemonEntity.getRandom().nextInt(255);
        
        if (roll > actualFleeRate) return;

        startFleeing(pokemonEntity, state);
    }

    public static void triggerImmediateDespawn(PokemonEntity pokemonEntity) {
        if (isShackleProtected(pokemonEntity)) return;
        if (!(pokemonEntity.level() instanceof ServerLevel serverLevel)) return;

        SafariPokemonState state = getState(pokemonEntity.getUUID());
        if (state != null && state.hasFled()) return;
        int token = state != null ? state.getFleeToken() : 0;
        if (state != null) {
            state.markFled();
        }

        pokemonEntity.getPokemon().getCustomProperties().add(
                UncatchableProperty.INSTANCE.uncatchable()
        );

        Component fledMessage = Component.translatable(
                "cobblesafari.safari.pokemon_fled",
                pokemonEntity.getExposedSpecies().getTranslatedName()
        );
        sendSubtitleToNearbyPlayers(pokemonEntity, fledMessage);
        serverLevel.playSound(null, pokemonEntity.getX(), pokemonEntity.getY(), pokemonEntity.getZ(),
                ModSounds.FLEE_SOUND, SoundSource.NEUTRAL, 1.0f, 1.0f);

        startFadeAndDespawn(pokemonEntity, token);
    }

    public static void pauseFleeTimer(UUID entityId) {
        SafariPokemonState state = getState(entityId);
        if (state != null) {
            state.incrementFleeToken();
            state.setFleeDeadlineTick(0);
        }
    }

    private static boolean isShackleProtected(PokemonEntity pokemonEntity) {
        return SafariConfig.redShackledSuspendsFlee() && RedShackledEffects.isShackled(pokemonEntity);
    }

    // ------------------------------------------------------------------ alpha rage

    /**
     * Whether this Pokémon answers provocation with violence rather than flight.
     *
     * <p>Two hard fallbacks send an alpha back to the ordinary flee path, because in both cases it
     * would otherwise become neither catchable by attrition nor losable:
     * <ul>
     *   <li><b>Peaceful difficulty</b> — both {@code AttackAngryAtTask} and the {@code switch_to_fight}
     *       task bail out on {@link Difficulty#PEACEFUL}, so no attack would ever land.</li>
     *   <li><b>{@code fightsMelee == false}</b> — the alpha would enter the fight activity and give
     *       chase without ever being able to strike.</li>
     * </ul>
     */
    private static boolean canEnrage(PokemonEntity pokemonEntity) {
        if (!pokemonEntity.getPokemon().isAlpha()) {
            return false;
        }
        if (!(pokemonEntity.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (serverLevel.getCurrentDifficultyAt(pokemonEntity.blockPosition()).getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        return pokemonEntity.getPokemon().getForm().getBehaviour().getCombat().getFightsMelee();
    }

    /**
     * Rolls the same 255-sided die the flee path uses, but resolves it as rage. Mud balls therefore
     * still "push" an alpha and bait still soothes it — what changes is the outcome, not the odds.
     */
    private static void tryEnrage(PokemonEntity pokemonEntity) {
        UUID entityId = pokemonEntity.getUUID();
        SafariPokemonState state = getOrCreate(entityId);
        if (state.isEnraged()) {
            // Already angry: refresh the timer rather than stacking a second rage.
            armRage(pokemonEntity, state);
            return;
        }

        int roll = pokemonEntity.getRandom().nextInt(255);
        if (roll > state.getActualFleeRate()) {
            return;
        }

        ServerPlayer target = resolveLastInteractingPlayer(pokemonEntity, state);
        if (target == null) {
            return;
        }

        armRage(pokemonEntity, state);
        // Cobblemon's own breakFree() puts an AVOID_TARGET on the thrower for 5-20s. Left in place
        // the alpha would want to flee and attack at the same time, so it has to go.
        pokemonEntity.getBrain().eraseMemory(MemoryModuleType.AVOID_TARGET);
        // AttackAngryAtTask only requires ANGRY_AT to be present; it does not care how it got there,
        // so no damage is needed. It then sets ATTACK_TARGET and switch_to_fight takes over.
        pokemonEntity.getBrain().setMemoryWithExpiry(
                MemoryModuleType.ANGRY_AT, target.getUUID(), SafariConfig.getAlphaRageDurationTicks());

        Component message = Component.translatable(
                "cobblesafari.safari.alpha_enraged",
                pokemonEntity.getExposedSpecies().getTranslatedName());
        sendSubtitleToNearbyPlayers(pokemonEntity, message);
        pokemonEntity.cry();
    }

    private static void armRage(PokemonEntity pokemonEntity, SafariPokemonState state) {
        state.setEnraged(true);
        state.setRageDeadlineTick(currentServerTick + SafariConfig.getAlphaRageDurationTicks());
    }

    /**
     * Attempts to soothe an enraged alpha with a bait. Succeeds with
     * {@link SafariConfig#getAlphaRageBaitCalmChance()} probability and, unlike an ordinary bait,
     * leaves the mood gauge untouched on failure.
     *
     * @return {@code true} when the bait was consumed by the rage system, i.e. the caller must not
     *         apply the normal bait / flee handling.
     */
    public static boolean tryCalmWithBait(PokemonEntity pokemonEntity) {
        SafariPokemonState state = getState(pokemonEntity.getUUID());
        if (state == null || !state.isEnraged()) {
            return false;
        }
        if (pokemonEntity.getRandom().nextFloat() < SafariConfig.getAlphaRageBaitCalmChance()) {
            calmRage(pokemonEntity, state, true);
        }
        return true;
    }

    /**
     * Ends a rage. Erasing {@code ANGRY_AT} alone is not enough: {@code AttackAngryAtTask} has
     * already written {@code ATTACK_TARGET} and never runs again while it is present, so the alpha
     * would keep attacking. Both memories go together.
     *
     * @param viaBait when {@code true}, pins the mood to its calmest end — the real cost of
     *                de-escalating with bait instead of surviving the timer.
     */
    private static void calmRage(PokemonEntity pokemonEntity, SafariPokemonState state, boolean viaBait) {
        pokemonEntity.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        pokemonEntity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        state.setEnraged(false);
        state.setRageDeadlineTick(0);
        if (viaBait) {
            state.forceMoodToCalmest();
        }

        Component message = Component.translatable(
                "cobblesafari.safari.alpha_calmed",
                pokemonEntity.getExposedSpecies().getTranslatedName());
        sendSubtitleToNearbyPlayers(pokemonEntity, message);
    }

    private static void tickRageDeadlines(MinecraftServer server) {
        if (STATES.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, SafariPokemonState> entry : STATES.entrySet()) {
            SafariPokemonState state = entry.getValue();
            if (!state.isEnraged() || currentServerTick < state.getRageDeadlineTick()) {
                continue;
            }
            PokemonEntity entity = findPokemon(server, entry.getKey());
            if (entity == null) {
                state.setEnraged(false);
                state.setRageDeadlineTick(0);
                continue;
            }
            calmRage(entity, state, false);
        }
    }

    private static PokemonEntity findPokemon(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(entityId) instanceof PokemonEntity pokemonEntity) {
                return pokemonEntity;
            }
        }
        return null;
    }

    private static ServerPlayer resolveLastInteractingPlayer(PokemonEntity pokemonEntity, SafariPokemonState state) {
        UUID playerId = state.getLastInteractingPlayer();
        if (playerId == null || !(pokemonEntity.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(playerId);
    }

    /**
     * Freezes an ongoing flee countdown while the Pokémon is red-shackled. The armed flee task is
     * invalidated through the flee token and the remaining grace is stored so
     * {@link #resumeFleeAfterShackle(PokemonEntity)} can replan exactly what was left.
     */
    public static void suspendFleeForShackle(PokemonEntity pokemonEntity) {
        if (!SafariConfig.redShackledSuspendsFlee()) return;
        if (!isInSafariDimension(pokemonEntity)) return;

        SafariPokemonState state = getState(pokemonEntity.getUUID());
        if (state == null || !state.isFleeing() || state.hasFled() || state.isFleeSuspended()) return;

        int grace = SafariConfig.getFleeGracePeriodTicks();
        long deadline = state.getFleeDeadlineTick();
        int remaining = deadline > currentServerTick ? (int) (deadline - currentServerTick) : grace;
        remaining = Math.clamp(remaining, MIN_RESUMED_GRACE_TICKS, grace);

        state.incrementFleeToken();
        state.setFleeSuspended(true);
        state.setFleeRemainingGraceTicks(remaining);
        state.setFleeDeadlineTick(0);

        announceFleeSuspended(pokemonEntity);
    }

    /**
     * Resumes a flee countdown frozen by {@link #suspendFleeForShackle(PokemonEntity)}. Runs even when
     * the config toggle has been turned off in the meantime, otherwise a suspended Pokémon would stay
     * stuck in a flee that never resolves.
     */
    public static void resumeFleeAfterShackle(PokemonEntity pokemonEntity) {
        SafariPokemonState state = getState(pokemonEntity.getUUID());
        if (state == null || !state.isFleeSuspended()) return;

        state.setFleeSuspended(false);
        if (!state.isFleeing() || state.hasFled()) return;
        if (!pokemonEntity.isAlive() || !isInSafariDimension(pokemonEntity)) {
            remove(pokemonEntity.getUUID());
            return;
        }

        announceFleeResumed(pokemonEntity);
        scheduleFleeSequence(pokemonEntity, state.getFleeRemainingGraceTicks());
    }

    private static void announceFleeSuspended(PokemonEntity pokemonEntity) {
        sendSubtitleToNearbyPlayers(pokemonEntity, Component.translatable(
                "cobblesafari.safari.flee_suspended",
                pokemonEntity.getExposedSpecies().getTranslatedName()
        ));

        if (pokemonEntity.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, pokemonEntity.getX(), pokemonEntity.getY(), pokemonEntity.getZ(),
                    SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0f, 0.6f);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    pokemonEntity.getX(), pokemonEntity.getY() + pokemonEntity.getBbHeight() * 0.5,
                    pokemonEntity.getZ(), 12, 0.35, 0.35, 0.35, 0.05);
        }
    }

    private static void announceFleeResumed(PokemonEntity pokemonEntity) {
        sendSubtitleToNearbyPlayers(pokemonEntity, Component.translatable(
                "cobblesafari.safari.flee_resumed",
                pokemonEntity.getExposedSpecies().getTranslatedName()
        ));
    }

    private static void startFleeing(PokemonEntity pokemonEntity, SafariPokemonState state) {
        state.setFleeing(true);

        if (pokemonEntity.level() instanceof ServerLevel serverLevel) {
            state.setFleeStartTick(serverLevel.getServer().getTickCount());

            UUID culprit = state.getLastInteractingPlayer();
            if (culprit != null) {
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(culprit);
                if (player != null) {
                    int total = maxigregrze.cobblesafari.init.ModStats.awardAndGet(
                            player, maxigregrze.cobblesafari.init.ModStats.POKEMON_FLED_SAFARI);
                    maxigregrze.cobblesafari.advancement.ModCriteria.POKEMON_FLED.trigger(player, total);
                }
            }

            Component fleeMessage = Component.translatable(
                    "cobblesafari.safari.flee_warning",
                    pokemonEntity.getExposedSpecies().getTranslatedName()
            );
            sendSubtitleToNearbyPlayers(pokemonEntity, fleeMessage);

            int graceTicks = SafariConfig.getFleeGracePeriodTicks();
            scheduleFleeSequence(pokemonEntity, graceTicks);
        }
    }

    private static void scheduleFleeSequence(PokemonEntity pokemonEntity, int graceTicks) {
        UUID entityId = pokemonEntity.getUUID();
        SafariPokemonState state = getState(entityId);
        if (state == null) return;
        int token = state.getFleeToken();
        long executionTick = currentServerTick + graceTicks;
        state.setFleeDeadlineTick(executionTick);

        SCHEDULED_TASKS.add(new ScheduledTask(executionTick, () -> {
            SafariPokemonState current = getState(entityId);
            if (current == null || !current.isFleeing() || current.getFleeToken() != token) return;
            if (current.hasFled()) return;
            if (!pokemonEntity.isAlive()) {
                remove(entityId);
                return;
            }

            current.markFled();
            pokemonEntity.getPokemon().getCustomProperties().add(
                    UncatchableProperty.INSTANCE.uncatchable()
            );

            Component fledMessage = Component.translatable(
                    "cobblesafari.safari.pokemon_fled",
                    pokemonEntity.getExposedSpecies().getTranslatedName()
            );
            sendSubtitleToNearbyPlayers(pokemonEntity, fledMessage);

            if (pokemonEntity.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, pokemonEntity.getX(), pokemonEntity.getY(), pokemonEntity.getZ(),
                        ModSounds.FLEE_SOUND, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }

            startFadeAndDespawn(pokemonEntity, token);
        }));
    }

    private static void startFadeAndDespawn(PokemonEntity pokemonEntity, int token) {
        UUID entityId = pokemonEntity.getUUID();

        int[][] fadePattern = {
            {3, 0},   {4, 1},
            {6, 0},   {8, 1},
            {9, 0},   {10, 1},
            {11, 0},  {12, 1},
            {13, 0},  {14, 1},
            {15, 0}
        };

        for (int[] step : fadePattern) {
            boolean invisible = (step[1] == 1);
            long executionTick = currentServerTick + step[0];
            SCHEDULED_TASKS.add(new ScheduledTask(executionTick, () -> {
                SafariPokemonState current = getState(entityId);
                if (current != null && current.getFleeToken() != token) return;
                if (pokemonEntity.isAlive()) {
                    pokemonEntity.setInvisible(invisible);
                }
            }));
        }

        long despawnTick = currentServerTick + 20;
        SCHEDULED_TASKS.add(new ScheduledTask(despawnTick, () -> {
            SafariPokemonState current = getState(entityId);
            if (current != null && current.getFleeToken() != token) return;
            if (pokemonEntity.isAlive()) {
                pokemonEntity.setInvisible(true);
                pokemonEntity.discard();
            }
            remove(entityId);
        }));
    }

    public static void scheduleTickDelay(int ticks, Runnable action) {
        if (ticks <= 0) {
            action.run();
            return;
        }
        long executionTick = currentServerTick + ticks;
        SCHEDULED_TASKS.add(new ScheduledTask(executionTick, action));
    }

    public static void sendSubtitleToPlayer(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(2, 20, 5));
        player.connection.send(new ClientboundSetSubtitleTextPacket(message));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
    }

    private static void sendSubtitleToNearbyPlayers(PokemonEntity pokemon, Component message) {
        if (!(pokemon.level() instanceof ServerLevel serverLevel)) return;
        AABB area = pokemon.getBoundingBox().inflate(48.0);
        List<ServerPlayer> players = serverLevel.getPlayers(p -> area.contains(p.getX(), p.getY(), p.getZ()));
        for (ServerPlayer player : players) {
            sendSubtitleToPlayer(player, message);
        }
    }

    public static void cleanup() {
        STATES.clear();
    }
}
