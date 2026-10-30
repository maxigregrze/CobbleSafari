package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.csboss.CsBossVanish;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackLaneShadowEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * {@code base_flying_1} — Sky Attack (plan 199 § 12.1, TARGETED ; revu après retours).
 * <ol>
 *   <li>Ho-Oh disparaît avec le clignotement de fuite du Safari ({@link CsBossVanish}) ;</li>
 *   <li>pour chacun des 2 plongeons : une copie de Ho-Oh par joueur réapparaît (même clignotement,
 *       à l'envers) au-dessus de l'entrée d'un couloir passant par ce joueur, pousse son cri ; l'ombre
 *       du couloir se dessine pendant la charge (éclats dorés), puis la copie rase le sol en vol le
 *       long du couloir (contact : 7) et disparaît à son tour ;</li>
 *   <li>Ho-Oh réapparaît (fuite à l'envers).</li>
 * </ol>
 * Tant que Ho-Oh est caché il ignore les baumes ; les copies servent de relais et retirent du temps
 * au boss comme s'il était touché.
 */
public class SkyAttackDiveAttack implements CsBossAttack {

    private static final int BLINK = CsBossVanish.BLINK_TICKS; // 20
    private static final int DIVES = 2;
    private static final int FIRST_DIVE = BLINK;
    private static final int APPEAR_END = BLINK;          // local : réapparition 0..19
    private static final int CHARGE_END = APPEAR_END + 25; // charge 20..44
    private static final int DIVE_TICKS = 32;            // piqué 45..76, durée fixe (déterministe)
    private static final int DIVE_END = CHARGE_END + DIVE_TICKS;
    private static final int DIVE_PERIOD = DIVE_END + BLINK; // fuite 77..96 ⇒ 97
    private static final int BOSS_RETURN = FIRST_DIVE + DIVES * DIVE_PERIOD; // 214
    private static final int DURATION = 240;
    private static final double START_HEIGHT = 8.0;
    private static final int DESCENT_TICKS = 8;
    private static final double HIT_RADIUS = 1.5;
    private static final double HIT_HEIGHT = 2.0;
    private static final float DAMAGE = 7.0F; // base 7 : charge lente et couloir lisible
    private static final double MINION_HEIGHT = 3.0;
    private static final float LANE_WIDTH = 2.5F;
    private static final float LANE_ALPHA = 0.75F;

    private static final class Lane {
        final Vec3 start;
        final Vec3 dir;
        final double length;
        final CsBossMinionEntity minion;
        final AttackLaneShadowEntity shadow;
        final Set<UUID> hit = new HashSet<>();

        Lane(Vec3 start, Vec3 dir, double length, CsBossMinionEntity minion, AttackLaneShadowEntity shadow) {
            this.start = start;
            this.dir = dir;
            this.length = length;
            this.minion = minion;
            this.shadow = shadow;
        }
    }

    private final String id;
    private final List<Lane> lanes = new ArrayList<>();
    private int tick;
    private boolean done;

    public SkyAttackDiveAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.TARGETED;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        this.lanes.clear();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        // 1. Ho-Oh s'efface (immunisé dès le début de la fuite).
        if (tick < FIRST_DIVE) {
            if (tick == 0) {
                CsBossVanish.setBossHidden(boss, true);
                boss.triggerAttackAnimation();
            }
            CsBossVanish.blink(level, boss, tick, false);
        }
        // 2. Plongeons.
        int local = tick - FIRST_DIVE;
        if (local >= 0 && local < DIVES * DIVE_PERIOD) {
            tickDive(level, session, local % DIVE_PERIOD);
        }
        // 3. Ho-Oh revient (fuite à l'envers) ; à nouveau ciblable une fois entièrement visible.
        if (tick >= BOSS_RETURN && tick < BOSS_RETURN + BLINK) {
            CsBossVanish.blink(level, boss, tick - BOSS_RETURN, true);
        } else if (tick == BOSS_RETURN + BLINK) {
            CsBossVanish.setBossHidden(boss, false);
        }
        if (++tick >= DURATION) {
            discardLanes();
            CsBossVanish.setBossHidden(boss, false);
            done = true;
        }
    }

    private void tickDive(ServerLevel level, BossBattleSession session, int d) {
        if (d == 0) {
            discardLanes();
            setupLanes(level, session);
        }
        double floorY = session.getFloorY();
        for (Lane lane : lanes) {
            CsBossMinionEntity m = lane.minion;
            if (!m.isAlive()) {
                continue;
            }
            if (d < APPEAR_END) {
                CsBossVanish.blink(level, m, d, true);
            }
            if (d < CHARGE_END) {
                lane.shadow.setAlpha(LANE_ALPHA * Math.min(1.0F, d / (float) CHARGE_END));
                if (d >= APPEAR_END) {
                    level.sendParticles(ParticleTypes.WAX_ON, m.getX(), m.getY() + 1.5, m.getZ(), 3, 1.0, 1.0, 1.0, 0.05);
                    if (d % 3 == 0) {
                        level.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + 1.5, m.getZ(), 1, 0.8, 0.8, 0.8, 0.02);
                    }
                }
            } else if (d < DIVE_END) {
                if (d == CHARGE_END) {
                    m.triggerAttackAnimation();
                    CsBossAttackLib.sound(level, m.getX(), m.getY(), m.getZ(),
                            "minecraft:entity.ender_dragon.flap", SoundSource.HOSTILE, 1.4F, 1.2F);
                }
                int t = d - CHARGE_END + 1;
                double travelled = lane.length * t / DIVE_TICKS;
                double descent = Math.min(1.0, t / (double) DESCENT_TICKS);
                Vec3 p = lane.start.add(lane.dir.scale(travelled));
                double y = floorY + START_HEIGHT - (START_HEIGHT - 1.0) * descent;
                m.setPos(p.x, y, p.z);
                level.sendParticles(ParticleTypes.WAX_ON, p.x, y + 1.0, p.z, 2, 0.6, 0.4, 0.6, 0.02);
                lane.shadow.setAlpha(LANE_ALPHA * (float) Math.max(0.0, 1.0 - travelled / lane.length));
                hitPlayers(level, session, lane);
            } else {
                if (d == DIVE_END && lane.shadow.isAlive()) {
                    lane.shadow.discard();
                }
                CsBossVanish.blink(level, m, d - DIVE_END, false);
                if (d == DIVE_PERIOD - 1) {
                    m.discard();
                }
            }
        }
    }

    private void setupLanes(ServerLevel level, BossBattleSession session) {
        double half = session.getPlayerRadius();
        double floorY = session.getFloorY();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            double a = level.getRandom().nextDouble() * Math.PI * 2.0;
            Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            Vec3 start = new Vec3(p.getX(), floorY, p.getZ()).subtract(dir.scale(half));
            Vec3 end = start.add(dir.scale(half * 2.0));
            CsBossMinionEntity minion = session.spawnMinion(level,
                    new Vec3(start.x, floorY + START_HEIGHT, start.z), MINION_HEIGHT, m -> {
                        m.setInvisible(true); // la réapparition le révèle en clignotant
                        m.setForcedPose(CsBossMinionEntity.POSE_FLY); // vole aussi au ras du sol
                        m.setBalmProxy(true); // reçoit les baumes à la place du boss caché
                        m.faceTargetInstant(end);
                    });
            AttackLaneShadowEntity shadow = session.trackAttackEntity(AttackLaneShadowEntity.spawn(level,
                    start.x, floorY, start.z, session.getId(), (float) Math.toDegrees(a), (float) (half * 2.0),
                    LANE_WIDTH));
            lanes.add(new Lane(start, dir, half * 2.0, minion, shadow));
            CsBossAttackLib.sound(level, start.x, floorY + START_HEIGHT, start.z, cry(session),
                    SoundSource.HOSTILE, 1.5F, 1.0F);
        }
    }

    /** Cri de l'espèce du minion ({@code "hooh"} → {@code cobblemon:pokemon.hooh.cry}). */
    private static String cry(BossBattleSession session) {
        String specie = session.getDefinition().effectiveMinionSpecie().trim().split("\\s+")[0];
        return "cobblemon:pokemon." + specie.toLowerCase(Locale.ROOT) + ".cry";
    }

    private void hitPlayers(ServerLevel level, BossBattleSession session, Lane lane) {
        for (ServerPlayer p : session.aliveParticipants(level)) {
            if (lane.hit.contains(p.getUUID())) {
                continue;
            }
            double dx = p.getX() - lane.minion.getX();
            double dz = p.getZ() - lane.minion.getZ();
            double dy = p.getY() - lane.minion.getY();
            if (dx * dx + dz * dz <= HIT_RADIUS * HIT_RADIUS && Math.abs(dy) <= HIT_HEIGHT) {
                lane.hit.add(p.getUUID());
                CsBossDamage.hurt(p, session, DAMAGE);
            }
        }
    }

    private void discardLanes() {
        for (Lane lane : lanes) {
            if (lane.minion.isAlive()) {
                lane.minion.discard();
            }
            if (lane.shadow.isAlive()) {
                lane.shadow.discard();
            }
        }
        lanes.clear();
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
