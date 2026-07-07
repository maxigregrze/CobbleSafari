package maxigregrze.cobblesafari.csboss;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.advancement.ModCriteria;
import maxigregrze.cobblesafari.block.csboss.CsBossTriggerBlock;
import maxigregrze.cobblesafari.init.ModStats;
import maxigregrze.cobblesafari.block.csboss.CsBossTriggerBlockEntity;
import maxigregrze.cobblesafari.config.CsBossSettings;
import maxigregrze.cobblesafari.data.CsBossSavedData;
import maxigregrze.cobblesafari.entity.csboss.CsBossBulletEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.CsBossPortalEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.CsBossSpawnProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates boss battles. In-memory sessions, server tick,
 * activation, resolution, and recovery after a crash.
 */
public final class BossBattleManager {

    public enum Outcome { WIN, LOSS }

    private static final Map<Integer, BossBattleSession> SESSIONS = new LinkedHashMap<>();

    /** Activation knockback: horizontal radius and strength (≥ 8 blocks of pushback). */
    private static final double ACTIVATION_KNOCKBACK_RADIUS = 6.0;
    private static final double ACTIVATION_KNOCKBACK_STRENGTH = 2.0;

    private BossBattleManager() {}

    /** Active session by id (used by self-replicating attack entities). */
    @org.jetbrains.annotations.Nullable
    public static BossBattleSession getSession(int id) {
        return SESSIONS.get(id);
    }

    // --- Activation ----------------------------------------------------------

    public static InteractionResult tryActivate(ServerPlayer sp, ServerLevel level, BlockPos pos,
                                                CsBossTriggerBlockEntity be) {
        BlockState state = level.getBlockState(pos);

        // 1. trigger already busy?
        if (be.getActiveSessionId() != 0
                || (state.hasProperty(CsBossTriggerBlock.ACTIVE) && state.getValue(CsBossTriggerBlock.ACTIVE))) {
            feedback(sp, "busy");
            return InteractionResult.CONSUME;
        }
        // 2. server cap
        if (SESSIONS.size() >= CsBossSettings.get().getMaximumConcurrentFights()) {
            feedback(sp, "try_later");
            return InteractionResult.CONSUME;
        }
        // 3. resolve boss BEFORE any consumption
        CsBossDefinition def = CsBossRegistry.resolve(be.getBossRef());
        if (def == null) {
            feedback(sp, "no_boss");
            return InteractionResult.CONSUME;
        }
        // 4. cost item
        if (!be.getCostItemId().isBlank()) {
            ResourceLocation costLoc = ResourceLocation.tryParse(be.getCostItemId());
            Item cost = costLoc == null ? null : BuiltInRegistries.ITEM.getOptional(costLoc).orElse(null);
            if (cost == null) {
                feedback(sp, "bad_cost");
                return InteractionResult.CONSUME;
            }
            if (!sp.isCreative() && !consumeOne(sp, cost)) {
                feedback(sp, "need_item", Component.translatable(cost.getDescriptionId()));
                return InteractionResult.CONSUME;
            }
        }
        // 5. participants locked in
        int radius = be.effectivePlayerRadius();
        int yTol = CsBossSettings.get().getArenaYTolerance();
        Vec3 center = Vec3.atCenterOf(pos);
        List<ServerPlayer> participants = level.getPlayers(p ->
                p.isAlive() && withinArena(p.position(), center, radius, yTol));
        if (participants.isEmpty()) {
            feedback(sp, "no_players");
            return InteractionResult.CONSUME;
        }
        // 6/7. scan + toggle reactive blocks
        List<BlockPos> reactive = ArenaBlockScanner.scan(level, pos, be.effectiveBlockRadius());
        ArenaBlockScanner.setBattleState(level, reactive, true);
        // purge Cobblemon entities in the block-detection square
        double blockHalf = be.effectiveBlockRadius() * 16.0;
        int purgeMinY = Math.max(level.getMinBuildHeight(), pos.getY() - 8);
        int purgeMaxY = Math.min(level.getMaxBuildHeight() - 1, pos.getY() + 24);
        net.minecraft.world.phys.AABB purgeArea = new net.minecraft.world.phys.AABB(
                pos.getX() - blockHalf, purgeMinY, pos.getZ() - blockHalf,
                pos.getX() + blockHalf + 1, purgeMaxY, pos.getZ() + blockHalf + 1);
        for (PokemonEntity mon : level.getEntitiesOfClass(PokemonEntity.class, purgeArea)) {
            // Only clear wild Pokémon; never remove a player's (or NPC's) Pokémon from the arena.
            if (mon.getPokemon().isWild()) {
                mon.discard();
            }
        }
        // 8. session id + boss
        CsBossSavedData data = CsBossSavedData.get(level.getServer());
        int id = data.allocateSessionId();
        CsBossEntity boss = CsBossEntity.spawnAbove(level, pos, def, id);
        // 9. duration
        int duration = DifficultyScaling.computeDuration(def, participants);
        // 10. session
        List<UUID> uuids = new ArrayList<>();
        for (ServerPlayer p : participants) {
            uuids.add(p.getUUID());
        }
        double blockRadiusBlocks = be.effectiveBlockRadius() * 16.0;
        BossBattleSession session = new BossBattleSession(id, level.dimension(), pos, def, boss.getUUID(),
                new java.util.HashSet<>(uuids), reactive, radius, duration,
                def.portalDistance(), blockRadiusBlocks);
        SESSIONS.put(id, session);
        be.setActiveSessionId(id);
        if (state.hasProperty(CsBossTriggerBlock.ACTIVE)) {
            level.setBlock(pos, state.setValue(CsBossTriggerBlock.ACTIVE, true), Block.UPDATE_ALL);
        }
        // Summon projectile above the anchor + knockback for nearby players.
        Vec3 center2 = session.getArenaCenter();
        CsBossSpawnProjectileEntity proj = CsBossSpawnProjectileEntity.spawn(
                level, center2.x, pos.getY() + 1.0, center2.z, id);
        session.setSummonProjectileUuid(proj.getUUID());
        knockbackNearbyPlayers(level, pos);
        for (ServerPlayer p : participants) {
            session.getBossBar().addPlayer(p);
            ModStats.award(p, ModStats.CSBOSS_BATTLES_ATTEMPTED);
            maxigregrze.cobblesafari.objectives.ObjectivesManager.onBossStart(p);
        }
        data.putSnapshot(session.snapshot());
        if (def.music() != null && !def.music().isBlank()) {
            maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onBossStart(participants, def.music());
        }
        return InteractionResult.CONSUME;
    }

    // --- Tick ----------------------------------------------------------------

    public static void onServerTick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        List<Integer> toStartDeath = new ArrayList<>();
        List<Integer> toFinalizeWin = new ArrayList<>();
        List<Integer> toNextPhase = new ArrayList<>();
        List<Integer> toResolveLoss = new ArrayList<>();
        for (BossBattleSession s : SESSIONS.values()) {
            ServerLevel level = server.getLevel(s.getDimension());
            if (level == null) {
                continue;
            }
            switch (s.getPhase()) {
                case SUMMON_RISE -> {
                    tickSummonRise(server, level, s);
                    if (s.allDiscarded()) {
                        toResolveLoss.add(s.getId());
                    }
                }
                case PORTAL_OPENING -> {
                    tickPortalOpening(server, level, s);
                    if (s.allDiscarded()) {
                        toResolveLoss.add(s.getId());
                    }
                }
                case ENTRANCE -> {
                    tickEntrance(server, level, s);
                    if (s.allDiscarded()) {
                        toResolveLoss.add(s.getId());
                    }
                }
                case ACTIVE -> {
                    Outcome o = tickActive(server, level, s);
                    if (o == Outcome.LOSS) {
                        toResolveLoss.add(s.getId());
                    } else if (o == Outcome.WIN) {
                        toStartDeath.add(s.getId());
                    }
                }
                case DYING -> {
                    switch (tickDying(level, s)) {
                        case FINALIZE_WIN -> s.setPhase(BossBattleSession.Phase.PORTAL_CLOSING);
                        case NEXT_PHASE -> toNextPhase.add(s.getId());
                        case CONTINUE -> { /* in progress */ }
                    }
                }
                case PORTAL_CLOSING -> {
                    if (tickPortalClosing(level, s)) {
                        toFinalizeWin.add(s.getId());
                    }
                }
            }
        }
        for (int id : toStartDeath) {
            startDeathSequence(server, SESSIONS.get(id));
        }
        for (int id : toNextPhase) {
            beginNextPhase(server, SESSIONS.get(id));
        }
        for (int id : toFinalizeWin) {
            finalizeWin(server, SESSIONS.get(id));
        }
        for (int id : toResolveLoss) {
            resolveLoss(server, SESSIONS.get(id));
        }
    }

    private enum DyingResult { CONTINUE, FINALIZE_WIN, NEXT_PHASE }

    // --- Summon: projectile + portal -----------------------------

    /**
     * Phase {@code SUMMON_RISE}: the anchor projectile rises quickly to the boss spawn height.
     * At the end: damageless explosion, projectile removal, portal spawn (scale 0).
     */
    private static void tickSummonRise(MinecraftServer server, ServerLevel level, BossBattleSession s) {
        int t = s.tickPhase();
        updateParticipants(server, level, s);
        s.getBossBar().setProgress(1.0f);
        Vec3 c = s.getArenaCenter();
        double startY = s.getTriggerPos().getY() + 1.0;
        double targetY = s.getTriggerPos().getY() + CsBossEntity.STAND_Y_OFFSET + s.getEntranceHeight();
        float p = Math.min(1.0f, t / (float) BossBattleSession.SUMMON_RISE_TICKS);
        double y = startY + (targetY - startY) * p;
        if (level.getEntity(s.getSummonProjectileUuid()) instanceof CsBossSpawnProjectileEntity proj) {
            proj.setPos(c.x, y, c.z);
            proj.setDeltaMovement(Vec3.ZERO);
        }
        parkBoss(level, s); // boss invisible (scale 0) until the portal is open
        if (t >= BossBattleSession.SUMMON_RISE_TICKS) {
            // Damageless explosion (sound + visual).
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, targetY, c.z, 1, 0, 0, 0, 0);
            level.playSound(null, c.x, targetY, c.z, SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 1.0f, 1.0f);
            removeEntity(level, s.getSummonProjectileUuid());
            s.setSummonProjectileUuid(null);
            CsBossPortalEntity portal = CsBossPortalEntity.spawn(level, c.x, targetY, c.z,
                    s.getId(), s.getDefinition().portalType(), (float) s.getDefinition().portalSize());
            s.setPortalUuid(portal.getUUID());
            s.setPhase(BossBattleSession.Phase.PORTAL_OPENING);
        }
    }

    /** Phase {@code PORTAL_OPENING}: portal scales from 0 to 1 over {@code PORTAL_OPEN_TICKS}. */
    private static void tickPortalOpening(MinecraftServer server, ServerLevel level, BossBattleSession s) {
        int t = s.tickPhase();
        updateParticipants(server, level, s);
        s.getBossBar().setProgress(1.0f);
        float p = Math.min(1.0f, t / (float) BossBattleSession.PORTAL_OPEN_TICKS);
        if (level.getEntity(s.getPortalUuid()) instanceof CsBossPortalEntity portal) {
            portal.setAnim(p);
        }
        parkBoss(level, s);
        if (t >= BossBattleSession.PORTAL_OPEN_TICKS) {
            if (level.getEntity(s.getPortalUuid()) instanceof CsBossPortalEntity portal) {
                portal.setAnim(1.0f);
            }
            s.setPhase(BossBattleSession.Phase.ENTRANCE); // triggers the boss's existing entrance
        }
    }

    /**
     * Phase {@code PORTAL_CLOSING}: after the boss's final death, the portal shrinks to 0
     * over {@code PORTAL_CLOSE_TICKS}. Returns {@code true} once finished.
     */
    private static boolean tickPortalClosing(ServerLevel level, BossBattleSession s) {
        int t = s.tickPhase();
        float p = Math.min(1.0f, t / (float) BossBattleSession.PORTAL_CLOSE_TICKS);
        if (level.getEntity(s.getPortalUuid()) instanceof CsBossPortalEntity portal) {
            portal.setAnim(1.0f - p);
        }
        return t >= BossBattleSession.PORTAL_CLOSE_TICKS;
    }

    /** Keeps the boss parked and invisible (scale 0, immobile) during pre-phases. */
    private static void parkBoss(ServerLevel level, BossBattleSession s) {
        if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
            boss.setAnim(0.0f);
            boss.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Entrance phase: boss descends from ENTRANCE_HEIGHT and scales from 0 to its size. */
    private static void tickEntrance(MinecraftServer server, ServerLevel level, BossBattleSession s) {
        int t = s.tickPhase();
        float p = Math.min(1.0f, t / (float) BossBattleSession.ENTRANCE_TICKS);
        updateParticipants(server, level, s);
        s.getBossBar().setProgress(1.0f); // bar stays full during entrance
        if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
            boss.setAnim(p);
            double standY = s.getTriggerPos().getY() + CsBossEntity.STAND_Y_OFFSET;
            double y = standY + s.getEntranceHeight() * (1.0 - p);
            boss.setPos(s.getArenaCenter().x, y, s.getArenaCenter().z);
            boss.setDeltaMovement(Vec3.ZERO);
            boss.faceTarget(nearestParticipantPos(level, s, boss));
        }
        if (t >= BossBattleSession.ENTRANCE_TICKS) {
            if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
                boss.setPhase(CsBossEntity.PHASE_ACTIVE);
                boss.setAnim(1.0f);
            }
            s.setPhase(BossBattleSession.Phase.ACTIVE);
        }
    }

    /** Active phase: countdown, attacks, movement. Returns the outcome or {@code null}. */
    @Nullable
    private static Outcome tickActive(MinecraftServer server, ServerLevel level, BossBattleSession s) {
        s.decrementRemaining();
        updateParticipants(server, level, s);
        s.getBossBar().setProgress(s.progress());
        s.pruneBullets(level);
        s.pruneAttackEntities(level);
        s.incActiveTicks();
        if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
            s.getScheduler().tick(level, s, boss);
            Vec3 nearest = nearestParticipantPos(level, s, boss);
            if (!s.getScheduler().attackControlsRotation()) {
                boss.faceTarget(nearest);
            }
            if (s.getScheduler().isAttacking()) {
                boss.setDeltaMovement(0.0, boss.getDeltaMovement().y, 0.0);
            } else {
                boss.driveTowards(nearest, s.getArenaCenter(), s.getPlayerRadius());
            }
            manageFightingPokemons(level, s, boss);
        }
        if (s.allDiscarded()) {
            return Outcome.LOSS;
        }
        if (s.getRemaining() <= 0) {
            return Outcome.WIN;
        }
        return null;
    }

    // --- Decorative fighting Pokémon ------------------------------

    private static final int FIGHTING_POKEMON_DELAY = 60; // 3 s after fight start
    private static final int FIGHTING_POKEMON_LOOP = 30; // re-trigger attack animation
    /** NE / SE / SW / NW diagonals (unit offsets × distance). */
    private static final double[][] FIGHTING_DIAGONALS = {{1, -1}, {1, 1}, {-1, 1}, {-1, -1}};

    /** Spawns (at 3 s) then drives decorative Pokémon around the boss. */
    private static void manageFightingPokemons(ServerLevel level, BossBattleSession s, CsBossEntity boss) {
        if (!s.isFightingPokemonsSpawned() && s.getActiveTicks() >= FIGHTING_POKEMON_DELAY) {
            if (CsBossSettings.get().isShowFightingPokemons()) {
                spawnFightingPokemons(level, s, boss);
            }
            s.setFightingPokemonsSpawned(true);
        }
        if (s.getFightingPokemons().isEmpty()) {
            return;
        }
        boolean loop = s.getActiveTicks() % FIGHTING_POKEMON_LOOP == 0;
        for (UUID uuid : s.getFightingPokemons()) {
            if (level.getEntity(uuid) instanceof CsBossMinionEntity m && m.isAlive()) {
                m.faceTarget(boss.position()); // always facing the boss
                if (loop) {
                    m.triggerAttackAnimation(); // loop attack animation
                }
            }
        }
    }

    private static void spawnFightingPokemons(ServerLevel level, BossBattleSession s, CsBossEntity boss) {
        List<ServerPlayer> players = new ArrayList<>(s.aliveParticipants(level));
        if (players.isEmpty()) {
            return;
        }
        // ≤4: all; >4: 4 at random (shuffle + stop at 4 slots).
        java.util.Collections.shuffle(players);
        // Distance = boss hitbox edge + 2.5 blocks; projected on diagonal (component / √2).
        double distance = boss.getBbWidth() / 2.0 + 2.5;
        double d = distance / Math.sqrt(2.0);
        int triggerY = s.getTriggerPos().getY();
        int idx = 0;
        for (ServerPlayer p : players) {
            if (idx >= FIGHTING_DIAGONALS.length) {
                break;
            }
            Pokemon first = firstPartyPokemon(p);
            if (first == null) {
                continue; // empty party: skip this player
            }
            double x = boss.getX() + FIGHTING_DIAGONALS[idx][0] * d;
            double z = boss.getZ() + FIGHTING_DIAGONALS[idx][1] * d;
            // On ground: surface from scan util (otherwise boss level).
            net.minecraft.core.BlockPos surface = maxigregrze.cobblesafari.csboss.attack.CsBossSurfaceScanner
                    .findSurfaceColumn(level, (int) Math.floor(x), (int) Math.floor(z), triggerY,
                            maxigregrze.cobblesafari.csboss.attack.CsBossSurfaceScanner.DEFAULT_Y_TOLERANCE);
            double y = surface != null ? surface.getY() + 1.0 : boss.getY();
            CsBossMinionEntity m = CsBossMinionEntity.spawn(level, x, y, z,
                    pokemonModelLine(first), 1, s.getId());
            m.faceTarget(boss.position());
            m.triggerAttackAnimation();
            s.getActiveMinions().add(m.getUUID()); // cleaned up at fight end with other minions
            s.getFightingPokemons().add(m.getUUID());
            idx++;
        }
    }

    @Nullable
    private static Pokemon firstPartyPokemon(ServerPlayer player) {
        try {
            return Cobblemon.INSTANCE.getStorage().getParty(player).get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * {@code PokemonProperties} line matching the model (species + form + aspects + shiny + gender).
     * Nickname/pokeball excluded: a spaced nickname would break space-separated parsing.
     */
    private static final List<PokemonPropertyExtractor> MODEL_EXTRACTORS = List.of(
            PokemonPropertyExtractor.SPECIES, PokemonPropertyExtractor.FORM,
            PokemonPropertyExtractor.ASPECTS, PokemonPropertyExtractor.SHINY,
            PokemonPropertyExtractor.GENDER);

    private static String pokemonModelLine(Pokemon mon) {
        try {
            String line = mon.createPokemonProperties(MODEL_EXTRACTORS).asString(" ");
            if (line != null && !line.isBlank()) {
                return line;
            }
        } catch (Exception ignored) {
            // fall back to species only
        }
        return mon.getSpecies().getResourceIdentifier().toString();
    }

    /**
     * Dying phase: model fade + rise of +5 blocks (peak at mid-animation) + "dragon death"
     * effect. If a next phase is pending, stop at mid-animation.
     */
    private static DyingResult tickDying(ServerLevel level, BossBattleSession s) {
        int t = s.tickPhase();
        int half = BossBattleSession.DEATH_TICKS / 2;
        float p = Math.min(1.0f, t / (float) BossBattleSession.DEATH_TICKS);
        if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
            boss.setAnim(p);
            double riseFrac = Math.min(1.0, t / (double) half); // +5 rise, peak at mid-animation
            boss.setPos(boss.getX(), s.getDeathOriginY() + s.getEntranceHeight() * riseFrac, boss.getZ());
            boss.setDeltaMovement(Vec3.ZERO);
            spawnDeathParticles(level, boss);
        }
        if (s.getPendingNextDef() != null) {
            return t >= half ? DyingResult.NEXT_PHASE : DyingResult.CONTINUE;
        }
        return t >= BossBattleSession.DEATH_TICKS ? DyingResult.FINALIZE_WIN : DyingResult.CONTINUE;
    }

    private static void updateParticipants(MinecraftServer server, ServerLevel level, BossBattleSession s) {
        int yTol = CsBossSettings.get().getArenaYTolerance();
        for (Map.Entry<UUID, ParticipantState> e : s.getParticipants().entrySet()) {
            ParticipantState st = e.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());

            if (st.discarded) {
                // Coward death: a discarded participant returned alive to the arena.
                if (player != null && player.isAlive()
                        && player.level().dimension() == s.getDimension()
                        && s.withinArena(player.position(), yTol)) {
                    player.hurt(CsBossDamage.cowardice(level), Float.MAX_VALUE);
                }
                if (player != null) {
                    s.getBossBar().removePlayer(player);
                }
                continue;
            }
            if (player == null || !player.isAlive() || player.level().dimension() != s.getDimension()) {
                st.discarded = true;
                if (player != null) {
                    s.getBossBar().removePlayer(player);
                    // Boss music for living participants only: hard cut for this discarded player.
                    maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onBossLossOrLeave(player);
                }
                continue;
            }
            st.alive = true;
            st.inRadius = s.withinArena(player.position(), yTol);
            if (st.inRadius) {
                s.getBossBar().addPlayer(player);
            } else {
                s.getBossBar().removePlayer(player);
            }
        }
    }

    @Nullable
    private static Vec3 nearestParticipantPos(ServerLevel level, BossBattleSession s, CsBossEntity boss) {
        Vec3 nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : s.aliveParticipants(level)) {
            double d = p.position().distanceToSqr(boss.position());
            if (d < best) {
                best = d;
                nearest = p.position();
            }
        }
        return nearest;
    }

    // --- Resolution ----------------------------------------------------------

    /** Defeat (or forced stop): hard cut, immediate removal, no rewards. */
    private static void resolveLoss(MinecraftServer server, BossBattleSession s) {
        if (s == null) {
            return;
        }
        SESSIONS.remove(s.getId());
        ServerLevel level = server.getLevel(s.getDimension());
        if (level != null) {
            stopBossMusic(server, s);
            removeEntity(level, s.getBossUuid());
            removeEntity(level, s.getSummonProjectileUuid());
            removeEntity(level, s.getPortalUuid()); // hard cut, no closing animation
            for (UUID u : new ArrayList<>(s.getActiveBullets())) {
                removeEntity(level, u);
            }
            for (UUID u : new ArrayList<>(s.getActiveMinions())) {
                removeEntity(level, u);
            }
            for (UUID u : new ArrayList<>(s.getActiveAttackEntities())) {
                removeEntity(level, u);
            }
            ArenaBlockScanner.setBattleState(level, s.getChangedBlocks(), false);
            restoreTrigger(level, s.getTriggerPos());
        }
        s.getBossBar().removeAllPlayers();
        CsBossSavedData.get(server).removeSnapshot(s.getId());
    }

    /**
     * Phase victory: starts dying. If a {@code secondPhase} exists, it is a transition
     * (optional rewards, music/bar kept); otherwise final death (rewards + outro + bar removed).
     */
    private static void startDeathSequence(MinecraftServer server, BossBattleSession s) {
        if (s == null || s.getPhase() == BossBattleSession.Phase.DYING) {
            return;
        }
        s.setPhase(BossBattleSession.Phase.DYING);

        CsBossDefinition current = s.getDefinition();
        CsBossDefinition nextDef = current.hasSecondPhase()
                ? CsBossRegistry.resolve(current.secondPhase()) : null;
        s.setPendingNextDef(nextDef);

        ServerLevel level = server.getLevel(s.getDimension());
        if (level != null) {
            for (UUID u : new ArrayList<>(s.getActiveBullets())) {
                removeEntity(level, u);
            }
            for (UUID u : new ArrayList<>(s.getActiveMinions())) {
                removeEntity(level, u);
            }
            for (UUID u : new ArrayList<>(s.getActiveAttackEntities())) {
                removeEntity(level, u);
            }
            if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
                boss.setPhase(CsBossEntity.PHASE_DYING);
                boss.setAnim(0.0f);
                boss.setDeltaMovement(Vec3.ZERO);
                s.setDeathOriginY(boss.getY());
            }
            if (nextDef == null) {
                // Final death.
                RewardService.grant(level, s);
                triggerWinAdvancements(level, s);
                maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onBossWin(s.aliveParticipants(level));
                s.getBossBar().removeAllPlayers();
            } else if (current.giveRewardsBeforeSecondPhase()) {
                // Transition: this phase's rewards before the next (music/bar kept).
                RewardService.grant(level, s);
            }
        }
    }

    /**
     * Mid-dying of a {@code secondPhase} phase: switch to the next boss (same session,
     * same entity) and restart the entrance animation.
     */
    private static void beginNextPhase(MinecraftServer server, BossBattleSession s) {
        if (s == null) {
            return;
        }
        CsBossDefinition nextDef = s.getPendingNextDef();
        ServerLevel level = server.getLevel(s.getDimension());
        if (nextDef == null || level == null) {
            finalizeWin(server, s); // safety: no usable next phase
            return;
        }
        List<ServerPlayer> alive = s.aliveParticipants(level);
        int newDuration = DifficultyScaling.computeDuration(nextDef, alive);
        s.startPhase(nextDef, newDuration);
        s.setPhase(BossBattleSession.Phase.ENTRANCE);

        if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
            boss.setSpecie(nextDef.specie());
            boss.setSize((float) nextDef.size());
            boss.setStaticBoss(nextDef.isStatic());
            boss.setPhase(CsBossEntity.PHASE_ENTERING);
            boss.setAnim(0.0f);
            double standY = s.getTriggerPos().getY() + CsBossEntity.STAND_Y_OFFSET;
            boss.setPos(s.getArenaCenter().x, standY + s.getEntranceHeight(), s.getArenaCenter().z);
            boss.setDeltaMovement(Vec3.ZERO);
        }
        if (nextDef.music() != null && !nextDef.music().isBlank()) {
            maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onBossStart(alive, nextDef.music());
        }
        for (ServerPlayer p : alive) {
            s.getBossBar().addPlayer(p);
        }
    }

    private static void triggerWinAdvancements(ServerLevel level, BossBattleSession session) {
        int yTol = CsBossSettings.get().getArenaYTolerance();
        for (ServerPlayer player : session.aliveParticipants(level)) {
            if (session.withinArena(player.position(), yTol)) {
                ModCriteria.CSBOSS_WIN.trigger(player, session.getDimension(), session.getRootBossId());
                ModStats.award(player, ModStats.CSBOSS_BATTLES_WON);
                maxigregrze.cobblesafari.objectives.ObjectivesManager.onBossWin(player);
            }
        }
    }

    /** End of dying: final explosion, boss removal, arena restoration. */
    private static void finalizeWin(MinecraftServer server, BossBattleSession s) {
        if (s == null) {
            return;
        }
        SESSIONS.remove(s.getId());
        ServerLevel level = server.getLevel(s.getDimension());
        if (level != null) {
            if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        boss.getX(), boss.getY() + boss.getBbHeight() * 0.5, boss.getZ(), 1, 0, 0, 0, 0);
            }
            removeEntity(level, s.getBossUuid());
            removeEntity(level, s.getSummonProjectileUuid());
            removeEntity(level, s.getPortalUuid()); // portal closed (scale 0)
            for (UUID u : new ArrayList<>(s.getActiveBullets())) {
                removeEntity(level, u);
            }
            for (UUID u : new ArrayList<>(s.getActiveMinions())) {
                removeEntity(level, u);
            }
            for (UUID u : new ArrayList<>(s.getActiveAttackEntities())) {
                removeEntity(level, u);
            }
            ArenaBlockScanner.setBattleState(level, s.getChangedBlocks(), false);
            restoreTrigger(level, s.getTriggerPos());
        }
        s.getBossBar().removeAllPlayers();
        CsBossSavedData.get(server).removeSnapshot(s.getId());
    }

    /** "Dragon death" effect: explosions + lightning around the boss each dying tick. */
    private static void spawnDeathParticles(ServerLevel level, CsBossEntity boss) {
        double cx = boss.getX();
        double cy = boss.getY() + boss.getBbHeight() * 0.5;
        double cz = boss.getZ();
        double r = Math.max(1.0, boss.getBbWidth());
        for (int i = 0; i < 2; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 2.0 * r;
            double oy = (level.random.nextDouble() - 0.5) * boss.getBbHeight();
            double oz = (level.random.nextDouble() - 0.5) * 2.0 * r;
            level.sendParticles(ParticleTypes.EXPLOSION, cx + ox, cy + oy, cz + oz, 1, 0, 0, 0, 0);
        }
        if (level.getGameTime() % 5 == 0) {
            level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 1, 0, 0, 0, 0);
        }
    }

    private static void stopBossMusic(MinecraftServer server, BossBattleSession s) {
        for (UUID uuid : s.getParticipants().keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onBossLossOrLeave(p);
            }
        }
    }

    private static void restoreTrigger(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CsBossTriggerBlock && state.hasProperty(CsBossTriggerBlock.ACTIVE)) {
            level.setBlock(pos, state.setValue(CsBossTriggerBlock.ACTIVE, false), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(pos) instanceof CsBossTriggerBlockEntity be) {
            be.setActiveSessionId(0);
        }
    }

    private static void removeEntity(ServerLevel level, UUID uuid) {
        if (uuid == null) {
            return;
        }
        Entity e = level.getEntity(uuid);
        if (e != null) {
            e.discard();
        }
    }

    // --- Recovery after crash ------------------------------------------------

    public static void recoverAll(MinecraftServer server) {
        SESSIONS.clear();
        CsBossSavedData data = CsBossSavedData.get(server);
        List<CsBossSavedData.Snapshot> snapshots = data.getSnapshots();
        for (CsBossSavedData.Snapshot snap : snapshots) {
            ServerLevel level = server.getLevel(snap.dimension());
            if (level != null) {
                ArenaBlockScanner.setBattleState(level, snap.changedBlocks(), false);
                restoreTrigger(level, snap.triggerPos());
                removeEntity(level, snap.bossUuid());
                removeStrayBullets(level);
            }
        }
        data.clearSnapshots();
        if (!snapshots.isEmpty()) {
            maxigregrze.cobblesafari.CobbleSafari.LOGGER.info(
                    "[CSBoss] cancelled {} in-progress fight(s) after restart", snapshots.size());
        }
    }

    private static void removeStrayBullets(ServerLevel level) {
        List<Entity> stray = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof CsBossBulletEntity || e instanceof CsBossMinionEntity
                    || e instanceof CsBossSpawnProjectileEntity || e instanceof CsBossPortalEntity) {
                stray.add(e);
            }
        }
        for (Entity e : stray) {
            e.discard();
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        UUID uuid = player.getUUID();
        for (BossBattleSession s : SESSIONS.values()) {
            ParticipantState st = s.getParticipants().get(uuid);
            if (st != null && !st.discarded) {
                st.discarded = true;
                s.getBossBar().removePlayer(player);
            }
        }
    }

    // --- Balm impact ---------------------------------------------------------

    /**
     * A thrown balm hit this boss: removes the configured percentage from its countdown
     * ({@code balmBossDamagePercent}). Ignored outside the active phase.
     */
    public static void onBalmHit(CsBossEntity boss) {
        BossBattleSession s = SESSIONS.get(boss.getSessionId());
        if (s == null || s.getPhase() != BossBattleSession.Phase.ACTIVE) {
            return;
        }
        s.reduceRemainingByPercent(CsBossSettings.get().getBalmBossDamagePercent());
        s.getBossBar().setProgress(s.progress());
    }

    // --- Command API ---------------------------------------------------------

    public static List<BossBattleSession> sessions() {
        return new ArrayList<>(SESSIONS.values());
    }

    public static BossBattleSession session(int id) {
        return SESSIONS.get(id);
    }

    public static boolean forceWin(MinecraftServer server, int id) {
        BossBattleSession s = SESSIONS.get(id);
        if (s == null) {
            return false;
        }
        startDeathSequence(server, s);
        return true;
    }

    public static boolean forceStop(MinecraftServer server, int id) {
        BossBattleSession s = SESSIONS.get(id);
        if (s == null) {
            return false;
        }
        resolveLoss(server, s);
        return true;
    }

    // --- Helpers -------------------------------------------------------------

    /** Horizontally pushes all players near the anchor outward. */
    private static void knockbackNearbyPlayers(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        double r2 = ACTIVATION_KNOCKBACK_RADIUS * ACTIVATION_KNOCKBACK_RADIUS;
        for (ServerPlayer p : level.getPlayers(pl -> {
            if (!pl.isAlive()) {
                return false;
            }
            double dx = pl.getX() - cx;
            double dz = pl.getZ() - cz;
            return dx * dx + dz * dz <= r2;
        })) {
            // knockback(strength, x, z) pushes opposite to (x, z): push away from the anchor.
            double dirX = cx - p.getX();
            double dirZ = cz - p.getZ();
            if (dirX * dirX + dirZ * dirZ < 1.0e-4) {
                dirX = 1.0; // player exactly on anchor: fallback direction
                dirZ = 0.0;
            }
            p.knockback(ACTIVATION_KNOCKBACK_STRENGTH, dirX, dirZ);
            p.hurtMarked = true; // force velocity sync to the client
        }
    }

    private static boolean withinArena(Vec3 pos, Vec3 center, int radius, int yTol) {
        double dx = pos.x - center.x;
        double dz = pos.z - center.z;
        double dy = Math.abs(pos.y - center.y);
        return Math.abs(dx) <= radius && Math.abs(dz) <= radius && dy <= yTol;
    }

    private static boolean consumeOne(ServerPlayer sp, Item cost) {
        ItemStack main = sp.getMainHandItem();
        if (main.is(cost) && !main.isEmpty()) {
            main.shrink(1);
            return true;
        }
        return false;
    }

    private static void feedback(ServerPlayer sp, String key) {
        sp.sendSystemMessage(Component.translatable("cobblesafari.csboss." + key));
    }

    private static void feedback(ServerPlayer sp, String key, Component arg) {
        sp.sendSystemMessage(Component.translatable("cobblesafari.csboss." + key, arg));
    }
}
