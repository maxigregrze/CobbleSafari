package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleManager;
import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackBellEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShockwaveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code jump_1} — Cloches de la Tour (plan 199 § 13.1, SPREAD). Une cloche par 2 joueurs (1 à 4) tombe autour de
 * l'arène, flottent à 1,5 bloc du sol et sonnent toutes les 3 s (onde dorée, 3 dégâts). Chaque cloche
 * encore debout à 12 s rend 5 % de la durée totale au boss. Cassée par 3 coups ou 1 baume (voir
 * {@link AttackBellEntity}). Hors attaques simultanées, l'attaque s'arrête dès que toutes sont cassées.
 */
public class JumpBellsAttack implements CsBossAttack {

    /** Une cloche pour 2 joueurs (arrondi au-dessus), 1 à 4 cloches. */
    private static final int MAX_BELLS = 4;
    private static final double MIN_FROM_BOSS = 6.0;
    private static final double MIN_BETWEEN = 8.0;
    private static final double DROP_HEIGHT = 12.0;
    /** Hauteur de vol de la cloche au-dessus du sol (l'onde reste au sol). */
    private static final double HOVER = 1.5;
    private static final int DROP_TICKS = 20;
    private static final int[] RING_TICKS = {60, 120, 180};
    private static final int SHOCKWAVE_COLOR = 0xF2C744;
    private static final float SHOCKWAVE_DAMAGE = 3.0F; // base 3
    private static final int HEAL_PERCENT = 5;
    private static final int DURATION = 240;

    private record Bell(AttackBellEntity entity, double floorY) {}

    private final String id;
    private final List<Bell> bells = new ArrayList<>();
    private int tick;
    private boolean done;

    public JumpBellsAttack(String id) {
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
        this.bells.clear();
        int triggerY = session.getFloorY();
        Vec3 center = new Vec3(boss.getX(), triggerY, boss.getZ());
        double radius = Math.max(MIN_FROM_BOSS + 2.0, session.getPlayerRadius() - 2.0);
        int players = Math.max(1, session.aliveParticipants(level).size());
        int count = Math.min(MAX_BELLS, (players + 1) / 2);
        // Échantillonnage large puis filtrage : scatterPoints n'impose pas de distance au centre.
        for (Vec3 p : CsBossAttackLib.scatterPoints(center, radius, count * 4, MIN_BETWEEN, level.getRandom())) {
            if (bells.size() >= count) {
                break;
            }
            if (p.distanceTo(center) < MIN_FROM_BOSS) {
                continue;
            }
            BlockPos surface = CsBossSurfaceScanner.findSurfaceColumn(level, (int) Math.floor(p.x),
                    (int) Math.floor(p.z), triggerY, CsBossSurfaceScanner.DEFAULT_Y_TOLERANCE);
            double floorY = surface != null ? surface.getY() + 1.0 : triggerY;
            AttackBellEntity bell = session.trackAttackEntity(
                    AttackBellEntity.spawn(level, p.x, floorY + HOVER + DROP_HEIGHT, p.z, session.getId()));
            bells.add(new Bell(bell, floorY));
        }
        boss.triggerAttackAnimation();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        for (Bell b : bells) {
            AttackBellEntity e = b.entity;
            if (e.isBroken()) {
                continue;
            }
            if (tick <= DROP_TICKS) {
                double y = b.floorY + HOVER + DROP_HEIGHT * (1.0 - tick / (double) DROP_TICKS);
                e.setPos(e.getX(), y, e.getZ());
                if (tick == DROP_TICKS) {
                    CsBossAttackLib.sound(level, e.getX(), y, e.getZ(),
                            "minecraft:block.bell.use", SoundSource.HOSTILE, 2.0F, 0.8F);
                }
            }
            for (int ring : RING_TICKS) {
                if (tick == ring) {
                    CsBossAttackLib.sound(level, e.getX(), e.getY() + 1.0, e.getZ(),
                            "minecraft:block.bell.resonate", SoundSource.HOSTILE, 2.0F, 1.0F);
                    session.trackAttackEntity(AttackShockwaveEntity.spawn(level, e.getX(), b.floorY, e.getZ(),
                            session.getId(), SHOCKWAVE_COLOR, SHOCKWAVE_DAMAGE, false, true));
                }
            }
        }
        if (++tick >= DURATION) {
            healFromSurvivors(level, session, boss);
            done = true;
        } else if (allBroken() && !session.getDefinition().allowSimultaneousAttacks()) {
            // Attaque seule : toutes les cloches cassées, on passe à la suivante. En mode simultané la
            // durée fixe est gardée, la paire doit finir ensemble.
            done = true;
        }
    }

    private boolean allBroken() {
        for (Bell b : bells) {
            if (!b.entity.isBroken()) {
                return false;
            }
        }
        return true;
    }

    /** Fin : chaque cloche intacte soigne le boss puis disparaît. */
    private void healFromSurvivors(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        Vec3 target = boss.position().add(0.0, boss.getBbHeight() * 0.5, 0.0);
        for (Bell b : bells) {
            AttackBellEntity e = b.entity;
            if (e.isBroken()) {
                continue;
            }
            BossBattleManager.healBoss(session, HEAL_PERCENT);
            Vec3 from = e.position().add(0.0, 1.2, 0.0);
            for (double f = 0.0; f <= 1.0; f += 0.05) {
                Vec3 p = from.add(target.subtract(from).scale(f));
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.02);
            }
            CsBossAttackLib.sound(level, e.getX(), e.getY(), e.getZ(),
                    "minecraft:block.beacon.power_select", SoundSource.HOSTILE, 1.5F, 1.2F);
            e.discard();
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
