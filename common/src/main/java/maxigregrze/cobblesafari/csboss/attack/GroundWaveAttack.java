package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShockwaveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * "Jump → ground shockwave" attack: on each landing the boss spawns a shockwave.
 *
 * <p>Parameterized generic utility: the variant is defined by constructor arguments
 * (color, damage, jump interval, poison, knockback), not a fixed type — anyone can
 * wire up a new variant. The {@link #water}/{@link #poison}/{@link #steel}/{@link #normal}
 * factories are only presets.
 */
public class GroundWaveAttack implements CsBossAttack {

    private static final double JUMP_HEIGHT = 3.0;
    private static final int DURATION = 240;

    private final String id;
    private final int colorRgb;
    private final float damage;
    private final int jumpInterval;
    private final boolean applyPoison;
    private final boolean applyKnockback;
    private final String landingSound;

    private int tick;
    private boolean done;
    private double cx;
    private double cz;

    /**
     * @param colorRgb ring color
     * @param damage contact damage (scale 8–18, skill)
     * @param jumpInterval ticks between jumps/waves (50 = nominal; 25 = twice as fast)
     * @param applyPoison applies Poison on contact
     * @param applyKnockback applies radial knockback on contact
     * @param landingSound sound played on each landing
     */
    public GroundWaveAttack(String id, int colorRgb, float damage, int jumpInterval,
                            boolean applyPoison, boolean applyKnockback, String landingSound) {
        this.id = id;
        this.colorRgb = colorRgb;
        this.damage = damage;
        this.jumpInterval = Math.max(1, jumpInterval);
        this.applyPoison = applyPoison;
        this.applyKnockback = applyKnockback;
        this.landingSound = landingSound;
    }

    public static GroundWaveAttack water(String id) {
        return new GroundWaveAttack(id, 0x3A8EE6, 10.0F, 50, false, true, "cobblemon:move.waterpulse.actor");
    }

    public static GroundWaveAttack poison(String id) {
        return new GroundWaveAttack(id, 0x4C2C7B, 8.0F, 50, true, false, "cobblemon:move.sludgebomb.actor");
    }

    public static GroundWaveAttack steel(String id) {
        // Steel: twice as fast (interval 25 ⇒ 2× jumps and waves).
        return new GroundWaveAttack(id, 0x929292, 10.0F, 25, false, false, "cobblemon:move.ironhead.actor");
    }

    public static GroundWaveAttack normal(String id) {
        return new GroundWaveAttack(id, 0xECECEC, 10.0F, 50, false, true, "cobblemon:move.stomp.actor");
    }

    public String id() {
        return id;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        this.cx = boss.getX();
        this.cz = boss.getZ();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }

        double floorY = session.getTriggerPos().getY();
        double hop = CsBossAttackLib.hopOffset(tick % jumpInterval, jumpInterval, JUMP_HEIGHT);
        boss.setPos(cx, floorY + hop, cz);
        boss.setDeltaMovement(Vec3.ZERO);

        if (tick > 0 && tick % jumpInterval == 0) {
            AttackShockwaveEntity wave = AttackShockwaveEntity.spawn(
                    level, cx, floorY, cz, session.getId(), colorRgb, damage, applyPoison, applyKnockback);
            session.trackAttackEntity(wave);
            boss.triggerAttackAnimation();
            CsBossAttackLib.sound(level, cx, floorY, cz, landingSound, SoundSource.HOSTILE, 1.2F, 1.0F);
        }

        if (++tick >= DURATION) {
            done = true;
            // Reset the boss to ground level (otherwise it stays airborne at the peak of the last jump).
            boss.setPos(cx, floorY, cz);
            boss.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.AREA;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
