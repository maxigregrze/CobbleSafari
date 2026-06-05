package maxigregrze.cobblesafari.csboss;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.block.csboss.CsBossTriggerBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossTriggerBlockEntity;
import maxigregrze.cobblesafari.config.CsBossSettings;
import maxigregrze.cobblesafari.data.CsBossSavedData;
import maxigregrze.cobblesafari.entity.csboss.CsBossBulletEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
 * Orchestrateur des combats de boss (plan 100 § 4/6). Sessions en mémoire, tick serveur,
 * activation, résolution et reprise après crash.
 */
public final class BossBattleManager {

    public enum Outcome { WIN, LOSS }

    private static final Map<Integer, BossBattleSession> SESSIONS = new LinkedHashMap<>();

    private BossBattleManager() {}

    /** Session active par id (utilisé par les entités d'attaque auto‑réplicantes, plan 107). */
    @org.jetbrains.annotations.Nullable
    public static BossBattleSession getSession(int id) {
        return SESSIONS.get(id);
    }

    // --- Activation ----------------------------------------------------------

    public static InteractionResult tryActivate(ServerPlayer sp, ServerLevel level, BlockPos pos,
                                                CsBossTriggerBlockEntity be) {
        BlockState state = level.getBlockState(pos);

        // 1. trigger déjà occupé ?
        if (be.getActiveSessionId() != 0
                || (state.hasProperty(CsBossTriggerBlock.ACTIVE) && state.getValue(CsBossTriggerBlock.ACTIVE))) {
            feedback(sp, "busy");
            return InteractionResult.CONSUME;
        }
        // 2. plafond serveur
        if (SESSIONS.size() >= CsBossSettings.get().getMaximumConcurrentFights()) {
            feedback(sp, "try_later");
            return InteractionResult.CONSUME;
        }
        // 3. résolution boss AVANT toute consommation
        CsBossDefinition def = CsBossRegistry.resolve(be.getBossRef());
        if (def == null) {
            feedback(sp, "no_boss");
            return InteractionResult.CONSUME;
        }
        // 4. item de coût
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
        // 5. participants figés
        int radius = be.effectivePlayerRadius();
        int yTol = CsBossSettings.get().getArenaYTolerance();
        Vec3 center = Vec3.atCenterOf(pos);
        List<ServerPlayer> participants = level.getPlayers(p ->
                p.isAlive() && withinArena(p.position(), center, radius, yTol));
        if (participants.isEmpty()) {
            feedback(sp, "no_players");
            return InteractionResult.CONSUME;
        }
        // 6/7. scan + bascule blocs réactifs
        List<BlockPos> reactive = ArenaBlockScanner.scan(level, pos, be.effectiveBlockRadius());
        ArenaBlockScanner.setBattleState(level, reactive, true);
        // 8. session id + boss
        CsBossSavedData data = CsBossSavedData.get(level.getServer());
        int id = data.allocateSessionId();
        CsBossEntity boss = CsBossEntity.spawnAbove(level, pos, def, id);
        // 9. durée
        int duration = DifficultyScaling.computeDuration(def, participants);
        // 10. session
        List<UUID> uuids = new ArrayList<>();
        for (ServerPlayer p : participants) {
            uuids.add(p.getUUID());
        }
        BossBattleSession session = new BossBattleSession(id, level.dimension(), pos, def, boss.getUUID(),
                new java.util.HashSet<>(uuids), reactive, radius, duration);
        SESSIONS.put(id, session);
        be.setActiveSessionId(id);
        if (state.hasProperty(CsBossTriggerBlock.ACTIVE)) {
            level.setBlock(pos, state.setValue(CsBossTriggerBlock.ACTIVE, true), Block.UPDATE_ALL);
        }
        for (ServerPlayer p : participants) {
            session.getBossBar().addPlayer(p);
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
                        case FINALIZE_WIN -> toFinalizeWin.add(s.getId());
                        case NEXT_PHASE -> toNextPhase.add(s.getId());
                        case CONTINUE -> { /* en cours */ }
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

    /** Phase d'entrée : le boss descend de ENTRANCE_HEIGHT et grandit de l'échelle 0 à sa taille. */
    private static void tickEntrance(MinecraftServer server, ServerLevel level, BossBattleSession s) {
        int t = s.tickPhase();
        float p = Math.min(1.0f, t / (float) BossBattleSession.ENTRANCE_TICKS);
        updateParticipants(server, level, s);
        s.getBossBar().setProgress(1.0f); // la barre reste pleine pendant l'entrée
        if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
            boss.setAnim(p);
            double standY = s.getTriggerPos().getY() + CsBossEntity.STAND_Y_OFFSET;
            double y = standY + CsBossEntity.ENTRANCE_HEIGHT * (1.0 - p);
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

    /** Phase active : compte à rebours, attaques, mouvement. Renvoie l'issue ou {@code null}. */
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

    // --- Pokémon combattants décoratifs (plan 112) ---------------------------

    private static final int FIGHTING_POKEMON_DELAY = 60; // 3 s après le début du combat
    private static final int FIGHTING_POKEMON_LOOP = 30;  // ré-déclenchement de l'animation d'attaque
    /** Diagonales NE / SE / SO / NO (offsets unitaires × distance). */
    private static final double[][] FIGHTING_DIAGONALS = {{1, -1}, {1, 1}, {-1, 1}, {-1, -1}};

    /** Fait apparaître (à 3 s) puis pilote les Pokémon décoratifs autour du boss. */
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
                m.faceTarget(boss.position()); // toujours orienté vers le boss
                if (loop) {
                    m.triggerAttackAnimation(); // boucle l'animation d'attaque
                }
            }
        }
    }

    private static void spawnFightingPokemons(ServerLevel level, BossBattleSession s, CsBossEntity boss) {
        List<ServerPlayer> players = new ArrayList<>(s.aliveParticipants(level));
        if (players.isEmpty()) {
            return;
        }
        // ≤4 : tous ; >4 : 4 au hasard (mélange + on s'arrête à 4 emplacements).
        java.util.Collections.shuffle(players);
        // Distance = bord de la hitbox du boss + 1 bloc ; projetée sur la diagonale (composante / √2).
        double distance = boss.getBbWidth() / 2.0 + 1.0;
        double d = distance / Math.sqrt(2.0);
        int triggerY = s.getTriggerPos().getY();
        int idx = 0;
        for (ServerPlayer p : players) {
            if (idx >= FIGHTING_DIAGONALS.length) {
                break;
            }
            Pokemon first = firstPartyPokemon(p);
            if (first == null) {
                continue; // équipe vide : on saute ce joueur
            }
            double x = boss.getX() + FIGHTING_DIAGONALS[idx][0] * d;
            double z = boss.getZ() + FIGHTING_DIAGONALS[idx][1] * d;
            // Au sol : surface trouvée via l'util de scan (sinon niveau du boss).
            net.minecraft.core.BlockPos surface = maxigregrze.cobblesafari.csboss.attack.CsBossSurfaceScanner
                    .findSurfaceColumn(level, (int) Math.floor(x), (int) Math.floor(z), triggerY,
                            maxigregrze.cobblesafari.csboss.attack.CsBossSurfaceScanner.DEFAULT_Y_TOLERANCE);
            double y = surface != null ? surface.getY() + 1.0 : boss.getY();
            CsBossMinionEntity m = CsBossMinionEntity.spawn(level, x, y, z,
                    pokemonModelLine(first), 1, s.getId());
            m.faceTarget(boss.position());
            m.triggerAttackAnimation();
            s.getActiveMinions().add(m.getUUID());   // nettoyé en fin de combat avec les autres minions
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
     * Ligne {@code PokemonProperties} reproduisant le modèle (espèce + forme + aspects + shiny + genre).
     * On exclut nickname/pokeball : un surnom à espaces casserait le parse séparé par espaces.
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
            // repli sur l'espèce seule
        }
        return mon.getSpecies().getResourceIdentifier().toString();
    }

    /**
     * Phase d'agonie : fondu du modèle + remontée de +5 blocs (sommet à mi‑animation) + effet
     * « mort du dragon ». S'il y a une phase suivante en attente, on s'arrête à mi‑animation.
     */
    private static DyingResult tickDying(ServerLevel level, BossBattleSession s) {
        int t = s.tickPhase();
        int half = BossBattleSession.DEATH_TICKS / 2;
        float p = Math.min(1.0f, t / (float) BossBattleSession.DEATH_TICKS);
        if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
            boss.setAnim(p);
            double riseFrac = Math.min(1.0, t / (double) half); // remontée +5, sommet atteint à mi‑animation
            boss.setPos(boss.getX(), s.getDeathOriginY() + CsBossEntity.ENTRANCE_HEIGHT * riseFrac, boss.getZ());
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
                // Mort « lâche » : un participant écarté revenu vivant dans l'arène.
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
                    // Musique de boss réservée aux vivants : coupure sèche pour ce participant écarté.
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

    // --- Résolution ----------------------------------------------------------

    /** Défaite (ou arrêt forcé) : coupure sèche, retrait immédiat, aucune récompense. */
    private static void resolveLoss(MinecraftServer server, BossBattleSession s) {
        if (s == null) {
            return;
        }
        SESSIONS.remove(s.getId());
        ServerLevel level = server.getLevel(s.getDimension());
        if (level != null) {
            stopBossMusic(server, s);
            removeEntity(level, s.getBossUuid());
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
     * Victoire de la phase : démarre l'agonie. S'il existe une {@code secondPhase}, c'est une
     * transition (récompenses optionnelles, musique/barre conservées) ; sinon c'est la mort
     * définitive (récompenses + outro + barre retirée).
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
                // Mort définitive.
                RewardService.grant(level, s);
                maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onBossWin(s.aliveParticipants(level));
                s.getBossBar().removeAllPlayers();
            } else if (current.giveRewardsBeforeSecondPhase()) {
                // Transition : récompenses de cette phase avant la suivante (musique/barre conservées).
                RewardService.grant(level, s);
            }
        }
    }

    /**
     * Mi‑agonie d'une phase à {@code secondPhase} : bascule sur le boss suivant (même session,
     * même entité) et relance l'animation d'entrée.
     */
    private static void beginNextPhase(MinecraftServer server, BossBattleSession s) {
        if (s == null) {
            return;
        }
        CsBossDefinition nextDef = s.getPendingNextDef();
        ServerLevel level = server.getLevel(s.getDimension());
        if (nextDef == null || level == null) {
            finalizeWin(server, s); // sécurité : pas de phase suivante exploitable
            return;
        }
        List<ServerPlayer> alive = s.aliveParticipants(level);
        int newDuration = DifficultyScaling.computeDuration(nextDef, alive);
        s.startPhase(nextDef, newDuration);
        s.setPhase(BossBattleSession.Phase.ENTRANCE);

        if (level.getEntity(s.getBossUuid()) instanceof CsBossEntity boss) {
            boss.setSpecie(nextDef.specie());
            boss.setSize(nextDef.size());
            boss.setStaticBoss(nextDef.isStatic());
            boss.setPhase(CsBossEntity.PHASE_ENTERING);
            boss.setAnim(0.0f);
            double standY = s.getTriggerPos().getY() + CsBossEntity.STAND_Y_OFFSET;
            boss.setPos(s.getArenaCenter().x, standY + CsBossEntity.ENTRANCE_HEIGHT, s.getArenaCenter().z);
            boss.setDeltaMovement(Vec3.ZERO);
        }
        if (nextDef.music() != null && !nextDef.music().isBlank()) {
            maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onBossStart(alive, nextDef.music());
        }
        for (ServerPlayer p : alive) {
            s.getBossBar().addPlayer(p);
        }
    }

    /** Fin de l'agonie : explosion finale, retrait du boss, restauration de l'arène. */
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

    /** Effet de mort « dragon » : explosions + éclairs autour du boss à chaque tick d'agonie. */
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
        Entity e = level.getEntity(uuid);
        if (e != null) {
            e.discard();
        }
    }

    // --- Reprise après crash -------------------------------------------------

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
            if (e instanceof CsBossBulletEntity || e instanceof CsBossMinionEntity) {
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

    // --- Impact baume --------------------------------------------------------

    /**
     * Un baume lancé a touché ce boss : retire de son compte à rebours le pourcentage configuré
     * ({@code balmBossDamagePercent}). Ignoré hors phase active.
     */
    public static void onBalmHit(CsBossEntity boss) {
        BossBattleSession s = SESSIONS.get(boss.getSessionId());
        if (s == null || s.getPhase() != BossBattleSession.Phase.ACTIVE) {
            return;
        }
        s.reduceRemainingByPercent(CsBossSettings.get().getBalmBossDamagePercent());
        s.getBossBar().setProgress(s.progress());
    }

    // --- API commandes -------------------------------------------------------

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

    private static boolean withinArena(Vec3 pos, Vec3 center, int radius, int yTol) {
        double dx = pos.x - center.x;
        double dz = pos.z - center.z;
        double dy = Math.abs(pos.y - center.y);
        return (dx * dx + dz * dz) <= (double) radius * radius && dy <= yTol;
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
