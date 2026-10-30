package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.block.csboss.CsBossScorchedTileBlock;
import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackPileProjectileEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.PileKind;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code base_fire_2} — Fire Blast (plan 199 § 11.2, SPREAD). Deux vagues de 4 boules de feu en
 * cloche vers des points répartis autour du boss ; chaque impact pose une étoile « 大 » de tuiles
 * de sol brûlant (alerte puis actif : 5 + brûlure, une fois par joueur et par étoile).
 */
public class FireBlastAttack implements CsBossAttack {

    private static final int WAVES = 2;
    private static final int WAVE_INTERVAL = 120;
    private static final int FIREBALLS = 4;
    private static final int FLIGHT_TICKS = 30;
    private static final double ARC_HEIGHT = 8.0;
    private static final float HIT_DAMAGE = 5.0F; // base 5 : boule touchée en vol, et tuile active
    private static final int FIRE_TICKS = 60;
    private static final double MIN_DIST = 6.0;
    private static final double JITTER = Math.toRadians(20.0);
    private static final int DURATION = 240;

    private record Pending(Vec3 target, int impactAt, AttackShadowEntity shadow) {}

    private final String id;
    private final List<Pending> pending = new ArrayList<>();
    private final CsBossTileField tiles = new CsBossTileField();
    private int tick;
    private boolean done;

    public FireBlastAttack(String id) {
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
        this.pending.clear();
        this.tiles.clear();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (tick % WAVE_INTERVAL == 0 && tick / WAVE_INTERVAL < WAVES) {
            launchWave(level, session, boss);
        }
        for (Pending p : new ArrayList<>(pending)) {
            if (p.impactAt == tick) {
                impact(level, session, p);
                pending.remove(p);
            }
        }
        tiles.tick(level, session, tick, (player, batch) -> {
            CsBossDamage.hurt(player, session, level.damageSources().inFire(), HIT_DAMAGE);
            if (CsBossDamage.stillInFight(player, level)) {
                player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), FIRE_TICKS));
            }
        });
        if (++tick >= DURATION) {
            done = true;
        }
    }

    private void launchWave(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        RandomSource rng = level.getRandom();
        int triggerY = session.getFloorY();
        double maxDist = Math.max(MIN_DIST + 1.0, session.getPlayerRadius() - 3.0);
        double base = rng.nextDouble() * Math.PI * 2.0;
        Vec3 origin = boss.position().add(0.0, 2.0, 0.0);
        for (int i = 0; i < FIREBALLS; i++) {
            double angle = base + i * Math.PI / 2.0 + (rng.nextDouble() * 2.0 - 1.0) * JITTER;
            double dist = MIN_DIST + rng.nextDouble() * (maxDist - MIN_DIST);
            double x = boss.getX() + Math.cos(angle) * dist;
            double z = boss.getZ() + Math.sin(angle) * dist;
            BlockPos surface = CsBossSurfaceScanner.findSurfaceColumn(level, (int) Math.floor(x), (int) Math.floor(z),
                    triggerY, CsBossSurfaceScanner.DEFAULT_Y_TOLERANCE);
            double y = surface != null ? surface.getY() + 1.0 : triggerY;
            Vec3 target = new Vec3(x, y, z);
            AttackShadowEntity shadow = session.trackAttackEntity(
                    AttackShadowEntity.spawn(level, x, y, z, session.getId(), true));
            session.trackAttackEntity(AttackPileProjectileEntity.spawn(level, origin, target, session.getId(),
                    PileKind.FIRE, FLIGHT_TICKS, ARC_HEIGHT, 1, HIT_DAMAGE));
            pending.add(new Pending(target, tick + FLIGHT_TICKS, shadow));
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.fireblast.actor", SoundSource.HOSTILE, 1.6F, 1.0F);
    }

    private void impact(ServerLevel level, BossBattleSession session, Pending p) {
        if (p.shadow.isAlive()) {
            p.shadow.discard();
        }
        Vec3 at = p.target;
        for (int i = 1; i <= 4; i++) {
            CsBossAttackLib.snowstorm(level, "cobblemon:daimonji" + i, at);
        }
        CsBossAttackLib.sound(level, at.x, at.y, at.z, "cobblemon:move.fireblast.target", SoundSource.HOSTILE, 1.4F, 1.0F);
        List<BlockPos> surfaces = new ArrayList<>();
        int triggerY = session.getFloorY();
        for (int[] cell : starCells(level.getRandom().nextDouble() * Math.PI * 2.0)) {
            BlockPos surface = CsBossSurfaceScanner.findSurfaceColumn(level,
                    (int) Math.floor(at.x) + cell[0], (int) Math.floor(at.z) + cell[1],
                    triggerY, CsBossSurfaceScanner.DEFAULT_Y_TOLERANCE);
            if (surface != null) {
                surfaces.add(surface);
            }
        }
        tiles.place(level, surfaces, ModBlocks.CSBOSS_SCORCHED.defaultBlockState(), tick,
                CsBossScorchedTileBlock.WARN_TICKS, CsBossScorchedTileBlock.ACTIVE_TICKS);
    }

    /**
     * Étoile « 大 » (le motif emblématique de Fire Blast), tournée de {@code angle} : barre
     * horizontale de 9, tige supérieure de 3, deux jambes diagonales de 4.
     */
    private static List<int[]> starCells(double angle) {
        List<double[]> local = new ArrayList<>();
        for (int u = -4; u <= 4; u++) {
            local.add(new double[]{u, 0});
        }
        for (int v = 1; v <= 3; v++) {
            local.add(new double[]{0, v});
        }
        for (int k = 1; k <= 4; k++) {
            local.add(new double[]{-k, -k});
            local.add(new double[]{k, -k});
        }
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Set<Long> seen = new LinkedHashSet<>();
        List<int[]> out = new ArrayList<>();
        for (double[] c : local) {
            int dx = (int) Math.round(c[0] * cos - c[1] * sin);
            int dz = (int) Math.round(c[0] * sin + c[1] * cos);
            if (seen.add(((long) dx << 32) ^ (dz & 0xFFFFFFFFL))) {
                out.add(new int[]{dx, dz});
            }
        }
        return out;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
