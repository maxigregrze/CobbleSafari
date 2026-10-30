package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackAirSlashEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * {@code base_flying_2} — Air Slash (plan 199 § 12.2, SPREAD). Quatre volées d'un éventail de 5
 * lames d'air tournoyantes (15° entre elles), chacune visant un joueur au hasard. 4 au contact.
 */
public class AirSlashAttack implements CsBossAttack {

    private static final int VOLLEY_INTERVAL = 60;
    private static final int VOLLEYS = 4;
    private static final int BLADES = 5;
    private static final double SPREAD_DEG = 15.0;
    private static final double SPEED = 0.6;
    private static final float DAMAGE = 4.0F; // base 4
    private static final int DURATION = 240;

    private final String id;
    private int tick;
    private boolean done;

    public AirSlashAttack(String id) {
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
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (tick % VOLLEY_INTERVAL == 0 && tick / VOLLEY_INTERVAL < VOLLEYS) {
            volley(level, session, boss);
        }
        if (++tick >= DURATION) {
            done = true;
        }
    }

    private void volley(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        ServerPlayer target = CsBossAttackLib.randomAlive(session, level, level.getRandom());
        double y = session.getFloorY() + 0.5; // lame de 0,5 à 1,0 au-dessus du sol : un saut l'évite
        double baseDeg = target != null
                ? Math.toDegrees(Math.atan2(target.getZ() - boss.getZ(), target.getX() - boss.getX()))
                : level.getRandom().nextDouble() * 360.0;
        Vec3 origin = new Vec3(boss.getX(), y, boss.getZ());
        for (int i = 0; i < BLADES; i++) {
            double a = Math.toRadians(baseDeg + (i - (BLADES - 1) / 2.0) * SPREAD_DEG);
            Vec3 velocity = new Vec3(Math.cos(a), 0.0, Math.sin(a)).scale(SPEED);
            session.trackAttackEntity(AttackAirSlashEntity.spawn(level, origin, session.getId(), velocity, DAMAGE));
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.gust.actor", SoundSource.HOSTILE, 1.5F, 1.3F);
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
