package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.block.csboss.CsBossPuddleTileBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossScorchedTileBlock;
import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.csboss.CsBossVanish;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code jump_2} — Bêtes légendaires (plan 199 § 13.2, SPREAD). Raikou, Entei et Suicune (ou
 * Voltali / Pyroli / Aquali s'ils ne sont pas disponibles) partent du boss à 120° en courant
 * (pose de marche imposée) en ligne droite jusqu'au bord du carré d'arène ; là ils fuient
 * ({@link CsBossVanish}), réapparaissent au centre et repartent dans la direction opposée (180°),
 * et ainsi de suite. Traînée de 3 blocs de rayon selon leur type : champ électrique, sol brûlant,
 * flaques qui jaillissent en geysers. Tous fuient une dernière fois avant la fin de l'attaque.
 */
public class JumpBeastsAttack implements CsBossAttack {

    private enum Kind { THUNDER, FIRE, WATER }

    private enum Phase { RUN, FLEE, APPEAR }

    private static final String[][] SPECIES = {
            {"raikou", "jolteon"},
            {"entei", "flareon"},
            {"suicune", "vaporeon"}
    };
    private static final double SPEED = 0.7;
    private static final double MINION_HEIGHT = 2.0;
    private static final int BLINK = CsBossVanish.BLINK_TICKS;
    private static final int DURATION = 240;
    /** Dernière fuite : elle se termine pile avec l'attaque. */
    private static final int FINAL_FLEE_AT = DURATION - BLINK;
    /** Arrêt un bloc avant le bord du carré. */
    private static final double EDGE_MARGIN = 1.0;
    /** Disque de traînée (rayon 3) posé tous les ~3,5 blocs parcourus : bande continue de 7 de large. */
    private static final int TRAIL_RADIUS = 3;
    private static final int TRAIL_EVERY = 5;
    private static final float PUDDLE_DAMAGE = 3.0F; // base 3
    private static final double PUDDLE_LAUNCH = 0.6;
    private static final double CONTACT_RADIUS = 1.3;
    private static final float CONTACT_DAMAGE = 4.0F; // base 4
    private static final int CONTACT_COOLDOWN = 40;
    private static final float SCORCH_DAMAGE = 3.0F; // base 3 + brûlure
    private static final int FIRE_TICKS = 60;

    private static final class Beast {
        final Kind kind;
        final CsBossMinionEntity minion;
        final Vec3 baseDir;
        final Map<UUID, Integer> nextHit = new HashMap<>();
        Vec3 dir;
        Phase phase = Phase.RUN;
        int phaseTick;
        int runIndex;
        double runLength;
        double travelled;
        boolean finalFlee;

        Beast(Kind kind, CsBossMinionEntity minion, Vec3 dir, double runLength) {
            this.kind = kind;
            this.minion = minion;
            this.baseDir = dir;
            this.dir = dir;
            this.runLength = runLength;
        }
    }

    private final String id;
    private final List<Beast> beasts = new ArrayList<>();
    private final CsBossTileField scorch = new CsBossTileField();
    private final CsBossTileField puddles = new CsBossTileField();
    private Vec3 origin = Vec3.ZERO;
    private Vec3 center = Vec3.ZERO;
    private double half;
    private int tick;
    private boolean done;

    public JumpBeastsAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.SPREAD;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        this.beasts.clear();
        this.scorch.clear();
        this.puddles.clear();
        this.origin = new Vec3(boss.getX(), session.getFloorY(), boss.getZ());
        this.center = session.getArenaCenter();
        this.half = session.getPlayerRadius();
        double base = level.getRandom().nextDouble() * Math.PI * 2.0;
        Kind[] kinds = Kind.values();
        for (int i = 0; i < kinds.length; i++) {
            String specie = CsBossAttackLib.resolveSpecies(SPECIES[i][0], SPECIES[i][1]);
            double a = base + i * Math.PI * 2.0 / 3.0;
            Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            Vec3 facing = origin.add(dir);
            // Réglée avant l'envoi aux clients : une première image en pose debout bloquait la course.
            CsBossMinionEntity minion = session.spawnFixedMinion(level, origin, specie, MINION_HEIGHT, m -> {
                m.setForcedPose(CsBossMinionEntity.POSE_WALK); // déplacée par code : course animée
                m.faceTargetInstant(facing);
            });
            beasts.add(new Beast(kinds[i], minion, dir, runLength(dir)));
            CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                    "cobblemon:pokemon." + specie + ".cry", SoundSource.HOSTILE, 1.2F, 1.0F);
        }
        boss.triggerAttackAnimation();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        for (Beast b : beasts) {
            if (!b.minion.isAlive()) {
                continue;
            }
            if (tick == FINAL_FLEE_AT && !b.finalFlee) {
                b.finalFlee = true;
                if (b.phase != Phase.FLEE) {
                    startFlee(b);
                }
            }
            switch (b.phase) {
                case RUN -> {
                    run(b);
                    trail(level, session, b);
                    contact(level, session, b);
                }
                case FLEE -> flee(level, b);
                case APPEAR -> appear(level, b);
            }
        }
        scorch.tick(level, session, tick, (player, batch) -> {
            CsBossDamage.hurt(player, session, level.damageSources().inFire(), SCORCH_DAMAGE);
            if (CsBossDamage.stillInFight(player, level)) {
                player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), FIRE_TICKS));
            }
        });
        puddles.tick(level, session, tick, (player, batch) -> {
            CsBossDamage.hurt(player, session, PUDDLE_DAMAGE);
            if (CsBossDamage.stillInFight(player, level)) {
                player.setDeltaMovement(player.getDeltaMovement().x, PUDDLE_LAUNCH, player.getDeltaMovement().z);
                player.hurtMarked = true; // une seule impulsion
            }
            CsBossAttackLib.sound(level, player.getX(), player.getY(), player.getZ(),
                    "minecraft:entity.generic.splash", SoundSource.HOSTILE, 1.0F, 1.0F);
        });
        if (++tick >= DURATION) {
            for (Beast b : beasts) {
                if (b.minion.isAlive()) {
                    b.minion.discard();
                }
            }
            done = true;
        }
    }

    /** Longueur d'une course depuis le centre jusqu'au bord du carré d'arène, dans {@code dir}. */
    private double runLength(Vec3 dir) {
        return Math.max(2.0, CsBossAttackLib.distanceToSquareEdge(origin, dir, center, half) - EDGE_MARGIN);
    }

    /** Ligne droite depuis le centre ; au bord, la bête fuit. */
    private void run(Beast b) {
        b.travelled = Math.min(b.runLength, b.travelled + SPEED);
        CsBossMinionEntity m = b.minion;
        Vec3 next = origin.add(b.dir.scale(b.travelled));
        m.setPos(next.x, next.y, next.z);
        m.faceTarget(next.add(b.dir));
        if (b.travelled >= b.runLength) {
            startFlee(b);
        }
    }

    private static void startFlee(Beast b) {
        b.phase = Phase.FLEE;
        b.phaseTick = 0;
        b.minion.setForcedPose(CsBossMinionEntity.POSE_AUTO); // immobile pendant le clignotement
    }

    /** Fuite sur place ; une fois cachée, la bête revient au centre pour la course suivante (180°). */
    private void flee(ServerLevel level, Beast b) {
        CsBossVanish.blink(level, b.minion, b.phaseTick, false);
        if (++b.phaseTick < BLINK || b.finalFlee) {
            return;
        }
        b.runIndex++;
        b.dir = b.runIndex % 2 == 0 ? b.baseDir : b.baseDir.reverse();
        b.runLength = runLength(b.dir);
        b.travelled = 0.0;
        b.minion.setPos(origin.x, origin.y, origin.z);
        b.minion.faceTargetInstant(origin.add(b.dir));
        b.phase = Phase.APPEAR;
        b.phaseTick = 0;
    }

    private static void appear(ServerLevel level, Beast b) {
        CsBossVanish.blink(level, b.minion, b.phaseTick, true);
        if (++b.phaseTick >= BLINK) {
            b.phase = Phase.RUN;
            b.minion.setForcedPose(CsBossMinionEntity.POSE_WALK);
        }
    }

    private void trail(ServerLevel level, BossBattleSession session, Beast b) {
        if (tick % TRAIL_EVERY != 0) {
            return;
        }
        CsBossMinionEntity m = b.minion;
        int triggerY = session.getFloorY();
        List<BlockPos> surfaces = new ArrayList<>();
        for (BlockPos off : CsBossGridShapes.filledCircle(TRAIL_RADIUS)) {
            BlockPos surface = CsBossSurfaceScanner.findSurfaceColumn(level, m.getBlockX() + off.getX(),
                    m.getBlockZ() + off.getZ(), triggerY, CsBossSurfaceScanner.DEFAULT_Y_TOLERANCE);
            if (surface != null) {
                surfaces.add(surface);
            }
        }
        switch (b.kind) {
            case THUNDER -> {
                for (BlockPos surface : surfaces) {
                    CsBossSurfaceScanner.placeOnSurface(level, surface, ModBlocks.CSBOSS_ELECTRICITY.defaultBlockState());
                }
            }
            case FIRE -> scorch.place(level, surfaces, ModBlocks.CSBOSS_SCORCHED.defaultBlockState(), tick,
                    CsBossScorchedTileBlock.WARN_TICKS, CsBossScorchedTileBlock.ACTIVE_TICKS);
            case WATER -> puddles.place(level, surfaces, ModBlocks.CSBOSS_PUDDLE.defaultBlockState(), tick,
                    CsBossPuddleTileBlock.WARN_TICKS, CsBossPuddleTileBlock.ACTIVE_TICKS);
        }
    }

    private void contact(ServerLevel level, BossBattleSession session, Beast b) {
        CsBossMinionEntity m = b.minion;
        for (ServerPlayer p : session.aliveParticipants(level)) {
            if (tick < b.nextHit.getOrDefault(p.getUUID(), 0)) {
                continue;
            }
            double dx = p.getX() - m.getX();
            double dz = p.getZ() - m.getZ();
            if (dx * dx + dz * dz <= CONTACT_RADIUS * CONTACT_RADIUS && Math.abs(p.getY() - m.getY()) <= 2.0) {
                b.nextHit.put(p.getUUID(), tick + CONTACT_COOLDOWN);
                CsBossDamage.hurt(p, session, CONTACT_DAMAGE);
            }
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
