package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.csboss.attack.AttackScheduler;
import maxigregrze.cobblesafari.entity.csboss.CsBossBulletEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Instance runtime d'un combat de boss (plan 100 § 6.1).
 */
public class BossBattleSession {

    /** Phases d'une session : entrée (temporisation), combat actif, agonie (animation de mort). */
    public enum Phase { ENTRANCE, ACTIVE, DYING }

    /** Durée de l'entrée (2 s) et de l'agonie. */
    public static final int ENTRANCE_TICKS = 40;
    public static final int DEATH_TICKS = 60;

    private Phase phase = Phase.ENTRANCE;
    private int phaseTimer = 0;

    private final int id;
    private final ResourceKey<Level> dimension;
    private final BlockPos triggerPos;
    private final Vec3 arenaCenter;
    private final int playerRadius;
    private final CsBossDefinition def;
    private final UUID bossUuid;

    private final ServerBossEvent bossBar;
    private final Map<UUID, ParticipantState> participants = new LinkedHashMap<>();
    private final List<BlockPos> changedBlocks;
    private final Set<UUID> activeBullets = new java.util.HashSet<>();
    private final AttackScheduler scheduler;
    private final int maxBullets;

    private final int totalDuration;
    private int remaining;

    public BossBattleSession(int id, ResourceKey<Level> dimension, BlockPos triggerPos,
                             CsBossDefinition def, UUID bossUuid, Set<UUID> participantUuids,
                             List<BlockPos> changedBlocks, int playerRadius, int totalDuration) {
        this.id = id;
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

    // --- Accès ---------------------------------------------------------------

    public int getId() {
        return id;
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

    /** Spawne une bullet si le cap n'est pas atteint (plan 100 § 12.3). */
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

    /** Purge les bullets disparues + renvoie la liste à supprimer côté monde. */
    public void pruneBullets(ServerLevel level) {
        activeBullets.removeIf(uuid -> level.getEntity(uuid) == null);
    }

    // --- Participants vivants ------------------------------------------------

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

    // --- Snapshot persistant -------------------------------------------------

    public CsBossSavedData.Snapshot snapshot() {
        return new CsBossSavedData.Snapshot(id, dimension, triggerPos, bossUuid,
                new ArrayList<>(participants.keySet()), new ArrayList<>(changedBlocks));
    }

    public BossEvent.BossBarColor color() {
        return def.healthColor();
    }
}
