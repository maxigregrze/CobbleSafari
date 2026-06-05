package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackWaveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * {@code base_water_2} (plan 110) : attaque <b>projectile</b> (pas par joueur). Le boss envoie, une
 * fois par seconde (8‑10 fois) dans une direction aléatoire, un trio de vagues en arc : la vague
 * centrale (3 blocs de large) fait face à la direction d'envoi, les deux latérales sont collées à
 * ses bords mais inclinées de 25° pour former un arc. 8 dégâts + forte poussée au contact.
 */
public class WaterWaveAttack implements CsBossAttack {

    private static final int WAVE_INTERVAL = 20;  // un trio par seconde
    private static final int WAVE_LIFESPAN = 50;
    private static final double SPEED = 0.5;
    private static final double SIDE_ANGLE_DEG = 25.0;
    private static final double HALF_WIDTH = 1.5;  // demi-largeur du mur (3 blocs)
    private static final double SPAWN_Y_OFFSET = 1.0;

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private int waves;
    private int tick;
    private int wavesSpawned;
    private boolean done;

    public WaterWaveAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.wavesSpawned = 0;
        this.done = false;
        this.waves = 8 + rng.nextInt(3); // 8‑10
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (wavesSpawned < waves && tick == wavesSpawned * WAVE_INTERVAL) {
            sendTrio(level, session, boss);
            wavesSpawned++;
        }
        if (tick >= (waves - 1) * WAVE_INTERVAL + WAVE_LIFESPAN) {
            done = true;
        }
        tick++;
    }

    private void sendTrio(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        double theta = rng.nextDouble() * Math.PI * 2.0;
        // Cap (F) et perpendiculaire droite (P) dans le plan horizontal.
        Vec3 f = new Vec3(Math.cos(theta), 0, Math.sin(theta));
        Vec3 p = new Vec3(Math.sin(theta), 0, -Math.cos(theta));
        double cos = Math.cos(Math.toRadians(SIDE_ANGLE_DEG));
        double sin = Math.sin(Math.toRadians(SIDE_ANGLE_DEG));
        double offLat = HALF_WIDTH + HALF_WIDTH * cos; // bord latéral interne collé au bord du milieu
        double offFwd = HALF_WIDTH * sin;

        Vec3 origin = new Vec3(boss.getX(), session.getTriggerPos().getY() + SPAWN_Y_OFFSET, boss.getZ());
        Vec3 velocity = f.scale(SPEED);

        // Milieu : face à F, avancé d'1 bloc + 5/16 vers l'avant par rapport aux latérales.
        spawnWave(level, session, origin.add(f.scale(1.0 + 5.0 / 16.0)), velocity, f);
        // Droite : décalée +offLat*P (+offFwd*F), inclinée +25° vers P.
        Vec3 rightDir = f.scale(cos).add(p.scale(sin));
        Vec3 rightPos = origin.add(p.scale(offLat)).add(f.scale(offFwd));
        spawnWave(level, session, rightPos, velocity, rightDir);
        // Gauche : symétrique.
        Vec3 leftDir = f.scale(cos).subtract(p.scale(sin));
        Vec3 leftPos = origin.subtract(p.scale(offLat)).add(f.scale(offFwd));
        spawnWave(level, session, leftPos, velocity, leftDir);

        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, origin.x, origin.y, origin.z,
                "cobblemon:move.waterpulse.actor", SoundSource.HOSTILE, 1.4F, 0.9F);
    }

    private void spawnWave(ServerLevel level, BossBattleSession session, Vec3 pos, Vec3 velocity, Vec3 facing) {
        float yaw = (float) Math.toDegrees(Math.atan2(facing.x, facing.z));
        AttackWaveEntity wave = AttackWaveEntity.spawn(level, pos.x, pos.y, pos.z, session.getId(), velocity, yaw);
        session.trackAttackEntity(wave);
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.SPREAD;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
