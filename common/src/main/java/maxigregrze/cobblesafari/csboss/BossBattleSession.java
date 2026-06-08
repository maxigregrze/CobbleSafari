package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.csboss.attack.AttackScheduler;
import maxigregrze.cobblesafari.entity.csboss.CsBossBulletEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import maxigregrze.cobblesafari.config.CsBossSettings;
import maxigregrze.cobblesafari.data.CsBossSavedData;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime instance of a boss battle (plan 100 § 6.1).
 */
public class BossBattleSession {

    /** Session phases: entrance (delay), active combat, dying (death animation). */
    public enum Phase { ENTRANCE, ACTIVE, DYING }

    /** Entrance (2 s) and dying duration. */
    public static final int ENTRANCE_TICKS = 40;
    public static final int DEATH_TICKS = 60;

    private Phase phase = Phase.ENTRANCE;
    private int phaseTimer = 0;

    private final int id;
    /** Root boss id from activation (unchanged across chained phases). */
    private final String rootBossId;
    private final ResourceKey<Level> dimension;
    private final BlockPos triggerPos;
    private final Vec3 arenaCenter;
    private final int playerRadius;
    private final UUID bossUuid;

    private final ServerBossEvent bossBar;
    private final Map<UUID, ParticipantState> participants = new LinkedHashMap<>();
    private final List<BlockPos> changedBlocks;
    private final Set<UUID> activeBullets = new java.util.HashSet<>();
    private final Set<UUID> activeMinions = new java.util.HashSet<>();
    private final Set<UUID> activeAttackEntities = new java.util.HashSet<>();
    /** Decorative "fighting Pokémon" minions around the boss (plan 112). */
    private final Set<UUID> fightingPokemons = new java.util.HashSet<>();
    private boolean fightingPokemonsSpawned = false;
    private int activeTicks = 0;
    private final int maxBullets;

    /** Current phase definition (may change on second phase). */
    private CsBossDefinition def;
    private AttackScheduler scheduler;
    private int totalDuration;
    private int remaining;

    /** Boss Y at start of dying (for the +5 block rise). */
    private double deathOriginY;
    /** Resolved next phase definition, pending (mid-dying), or {@code null}. */
    @Nullable
    private CsBossDefinition pendingNextDef;

    public BossBattleSession(int id, ResourceKey<Level> dimension, BlockPos triggerPos,
                             CsBossDefinition def, UUID bossUuid, Set<UUID> participantUuids,
                             List<BlockPos> changedBlocks, int playerRadius, int totalDuration) {
        this.id = id;
        this.rootBossId = def.bossId();
        this.dimension = dimension;
        this.triggerPos = triggerPos;
        this.arenaCenter = Vec3.atCenterOf(triggerPos);
        this.playerRadius = playerRadius;
        this.def = def;
        this.bossUuid = bossUuid;
        this.changedBlocks = new ArrayList<>(changedBlocks);
        this.totalDuration = Math.max(1, totalDuration);
        this.remaining = this.totalDuration;
        this.maxBullets = CsBossSettings.get().getMaxConcurrentBulletsPerSession();
        this.scheduler = new AttackScheduler(def);
        for (UUID uuid : participantUuids) {
            this.participants.put(uuid, new ParticipantState());
        }
        this.bossBar = new ServerBossEvent(
                Component.literal(def.effectiveDisplayName()),
                def.healthColor(), def.healthStyle());
        this.bossBar.setProgress(1.0F);
    }

    // --- Phase ---------------------------------------------------------------

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
        this.phaseTimer = 0;
    }

    public int tickPhase() {
        return ++phaseTimer;
    }

    public int getPhaseTimer() {
        return phaseTimer;
    }

    public double getDeathOriginY() {
        return deathOriginY;
    }

    public void setDeathOriginY(double y) {
        this.deathOriginY = y;
    }

    @Nullable
    public CsBossDefinition getPendingNextDef() {
        return pendingNextDef;
    }

    public void setPendingNextDef(@Nullable CsBossDefinition next) {
        this.pendingNextDef = next;
    }

    /**
     * Switches the session to a new definition (second phase): new duration, new
     * attack scheduler, and boss bar update (name/color/style).
     */
    public void startPhase(CsBossDefinition newDef, int newDuration) {
        this.def = newDef;
        this.scheduler = new AttackScheduler(newDef);
        this.totalDuration = Math.max(1, newDuration);
        this.remaining = this.totalDuration;
        this.pendingNextDef = null;
        // Reset decorative Pokémon: they respawn 3 s after the new phase starts.
        this.fightingPokemonsSpawned = false;
        this.activeTicks = 0;
        this.fightingPokemons.clear();
        this.bossBar.setName(Component.literal(newDef.effectiveDisplayName()));
        this.bossBar.setColor(newDef.healthColor());
        this.bossBar.setOverlay(newDef.healthStyle());
        this.bossBar.setProgress(1.0F);
    }

    // --- Access ----------------------------------------------------------------

    public int getId() {
        return id;
    }

    public String getRootBossId() {
        return rootBossId;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getTriggerPos() {
        return triggerPos;
    }

    public Vec3 getArenaCenter() {
        return arenaCenter;
    }

    public int getPlayerRadius() {
        return playerRadius;
    }

    public CsBossDefinition getDefinition() {
        return def;
    }

    public UUID getBossUuid() {
        return bossUuid;
    }

    public ServerBossEvent getBossBar() {
        return bossBar;
    }

    public Map<UUID, ParticipantState> getParticipants() {
        return participants;
    }

    public List<BlockPos> getChangedBlocks() {
        return changedBlocks;
    }

    public Set<UUID> getActiveBullets() {
        return activeBullets;
    }

    public AttackScheduler getScheduler() {
        return scheduler;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public int getRemaining() {
        return remaining;
    }

    public void decrementRemaining() {
        if (remaining > 0) {
            remaining--;
        }
    }

    /**
     * Removes a percentage of total duration from the countdown (= boss health bar).
     * Used by a thrown balm impact. {@code percent} is clamped to [0, 100].
     */
    public void reduceRemainingByPercent(int percent) {
        int pct = Math.max(0, Math.min(100, percent));
        int reduction = Math.round(totalDuration * (pct / 100.0F));
        remaining = Math.max(0, remaining - reduction);
    }

    public float progress() {
        return Math.max(0.0F, remaining / (float) totalDuration);
    }

    public boolean allDiscarded() {
        return participants.values().stream().allMatch(p -> p.discarded);
    }

    public boolean withinArena(Vec3 pos, int yTolerance) {
        double dx = pos.x - arenaCenter.x;
        double dz = pos.z - arenaCenter.z;
        double dy = Math.abs(pos.y - arenaCenter.y);
        return (dx * dx + dz * dz) <= (double) playerRadius * playerRadius && dy <= yTolerance;
    }

    // --- Bullets -------------------------------------------------------------

    /** Spawns a bullet if the cap is not reached (plan 100 § 12.3). */
    public boolean trySpawnBullet(ServerLevel level, Vec3 origin, Vec3 velocity) {
        if (activeBullets.size() >= maxBullets) {
            return false;
        }
        CsBossBulletEntity bullet = new CsBossBulletEntity(ModEntities.CSBOSS_BULLET, level);
        bullet.moveTo(origin.x, origin.y, origin.z, 0.0F, 0.0F);
        bullet.configure(id, participants.keySet(), velocity, 16.0F);
        level.addFreshEntity(bullet);
        activeBullets.add(bullet.getUUID());
        return true;
    }

    /** Purges vanished bullets (world-side removal list is derived from tracking). */
    public void pruneBullets(ServerLevel level) {
        activeBullets.removeIf(uuid -> level.getEntity(uuid) == null);
    }

    // --- Minions -------------------------------------------------------------

    public Set<UUID> getActiveMinions() {
        return activeMinions;
    }

    /**
     * Spawns a minion for this boss at the data model {@code minion} species
     * (otherwise the boss species). Tracked for cleanup at fight end.
     */
    public CsBossMinionEntity spawnMinion(ServerLevel level, Vec3 pos, int size) {
        CsBossMinionEntity minion = CsBossMinionEntity.spawn(
                level, pos.x, pos.y, pos.z, def.effectiveMinionSpecie(), size, id);
        activeMinions.add(minion.getUUID());
        return minion;
    }

    public CsBossMinionEntity spawnMinion(ServerLevel level, Vec3 pos) {
        return spawnMinion(level, pos, def.size());
    }

    public void pruneMinions(ServerLevel level) {
        activeMinions.removeIf(uuid -> level.getEntity(uuid) == null);
    }

    // --- Custom attack entities (shadows, meteorites, stems) -------------------

    /** Generic attack-entity tracking (plan 107) for cleanup at fight end. */
    public Set<UUID> getActiveAttackEntities() {
        return activeAttackEntities;
    }

    /** Registers an attack entity for cleanup and returns it (chaining). */
    public <T extends net.minecraft.world.entity.Entity> T trackAttackEntity(T entity) {
        activeAttackEntities.add(entity.getUUID());
        return entity;
    }

    public void pruneAttackEntities(ServerLevel level) {
        activeAttackEntities.removeIf(uuid -> level.getEntity(uuid) == null);
    }

    // --- Decorative fighting Pokémon (plan 112) ------------------------------

    public Set<UUID> getFightingPokemons() {
        return fightingPokemons;
    }

    public boolean isFightingPokemonsSpawned() {
        return fightingPokemonsSpawned;
    }

    public void setFightingPokemonsSpawned(boolean spawned) {
        this.fightingPokemonsSpawned = spawned;
    }

    public int getActiveTicks() {
        return activeTicks;
    }

    public int incActiveTicks() {
        return ++activeTicks;
    }

    /**
     * Spawns a minion at an explicit species (e.g. forced Voltorb for {@code base_electric_1}),
     * independent of the data model {@code minion} field. Tracked for cleanup at fight end.
     */
    public CsBossMinionEntity spawnFixedMinion(ServerLevel level, Vec3 pos, String specie, int size) {
        CsBossMinionEntity minion = CsBossMinionEntity.spawn(level, pos.x, pos.y, pos.z, specie, size, id);
        activeMinions.add(minion.getUUID());
        return minion;
    }

    // --- Living participants ---------------------------------------------------

    public List<ServerPlayer> aliveParticipants(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (Map.Entry<UUID, ParticipantState> e : participants.entrySet()) {
            if (e.getValue().discarded) {
                continue;
            }
            if (level.getPlayerByUUID(e.getKey()) instanceof ServerPlayer p && p.isAlive()) {
                out.add(p);
            }
        }
        return out;
    }

    // --- Persistent snapshot -------------------------------------------------

    public CsBossSavedData.Snapshot snapshot() {
        return new CsBossSavedData.Snapshot(id, dimension, triggerPos, bossUuid,
                new ArrayList<>(participants.keySet()), new ArrayList<>(changedBlocks));
    }

    public BossEvent.BossBarColor color() {
        return def.healthColor();
    }
}
