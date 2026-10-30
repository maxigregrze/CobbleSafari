package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.block.csboss.CsBossScorchedTileBlock;
import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code base_fire_3} — Overheat (plan 199 § 11.3, AREA ; revu après retours). Le carré d'arène est
 * coupé en trois zones de même surface autour du centre : disque intérieur, anneau du milieu, anneau
 * extérieur + coins. Les phases alternent « intérieur + extérieur » et « milieu seul », si bien qu'il
 * suffit toujours de passer d'une zone à sa voisine. 4 phases de 60 ticks (alerte 30 t puis actif
 * 30 t : 6 dégâts, une fois par phase, sans brûlure).
 */
public class OverheatAttack implements CsBossAttack {

    private static final int PHASE_TICKS = 60;
    private static final int PHASES = 4;
    /** Rayons (en fraction du demi-côté) qui coupent le carré en trois surfaces égales : πr² = 4h²/3 et 8h²/3. */
    private static final double INNER_FRACTION = Math.sqrt(4.0 / (3.0 * Math.PI)); // ≈ 0,65
    private static final double MIDDLE_FRACTION = Math.sqrt(8.0 / (3.0 * Math.PI)); // ≈ 0,92
    private static final float DAMAGE = 6.0F; // base 6
    private static final int DURATION = PHASE_TICKS * PHASES; // 240

    private final String id;
    private final CsBossTileField tiles = new CsBossTileField();
    private final List<BlockPos> sides = new ArrayList<>();  // intérieur + extérieur
    private final List<BlockPos> middle = new ArrayList<>();
    private boolean sidesFirst;
    private int tick;
    private boolean done;

    public OverheatAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.AREA;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        this.tiles.clear();
        this.sides.clear();
        this.middle.clear();
        this.sidesFirst = level.getRandom().nextBoolean();
        double half = session.getPlayerRadius();
        double r2Inner = Math.pow(half * INNER_FRACTION, 2);
        double r2Middle = Math.pow(half * MIDDLE_FRACTION, 2);
        net.minecraft.world.phys.Vec3 c = session.getArenaCenter();
        for (BlockPos surface : CsBossSurfaceScanner.scanSurface(level, session)) {
            double dx = surface.getX() + 0.5 - c.x;
            double dz = surface.getZ() + 0.5 - c.z;
            if (Math.abs(dx) > half || Math.abs(dz) > half) {
                continue;
            }
            double d2 = dx * dx + dz * dz;
            if (d2 > r2Inner && d2 <= r2Middle) {
                middle.add(surface);
            } else {
                sides.add(surface);
            }
        }
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        int phaseTick = tick % PHASE_TICKS;
        if (phaseTick == 0) {
            // Les deux groupes alternent : une zone a toujours fini son cycle avant d'être reposée.
            boolean sidesPhase = ((tick / PHASE_TICKS) % 2 == 0) == sidesFirst;
            List<BlockPos> zone = sidesPhase ? sides : middle;
            tiles.place(level, zone, ModBlocks.CSBOSS_SCORCHED.defaultBlockState(), tick,
                    CsBossScorchedTileBlock.WARN_TICKS, CsBossScorchedTileBlock.ACTIVE_TICKS);
            boss.triggerAttackAnimation();
        } else if (phaseTick == CsBossScorchedTileBlock.WARN_TICKS) {
            CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                    "cobblemon:move.flamethrower.actor", SoundSource.HOSTILE, 1.6F, 0.7F);
        }
        if (tick % 2 == 0) {
            double h = boss.getBbHeight();
            level.sendParticles(ParticleTypes.FLAME, boss.getX(), boss.getY() + h * 0.5, boss.getZ(),
                    6, boss.getBbWidth() * 0.4, h * 0.4, boss.getBbWidth() * 0.4, 0.02);
            level.sendParticles(ParticleTypes.WHITE_ASH, boss.getX(), boss.getY() + h * 0.5, boss.getZ(),
                    6, boss.getBbWidth() * 0.5, h * 0.5, boss.getBbWidth() * 0.5, 0.0);
        }
        tiles.tick(level, session, tick, (player, batch) -> CsBossDamage.hurt(player, session, DAMAGE));
        if (++tick >= DURATION) {
            done = true;
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
