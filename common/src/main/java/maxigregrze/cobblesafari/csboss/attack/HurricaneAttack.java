package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.block.csboss.CsBossUpdraftTileBlock;
import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.underground.logic.PerlinNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code base_flying_3} — Hurricane (plan 199 § 12.3, AREA). Deux vagues (0 s et 6 s) de courants
 * ascendants posés en champ de Perlin sur ~35 % du sol. Tuile active : impulsion unique vers le haut
 * (~4 blocs), Nausée 5 s et 3 dégâts, une fois par joueur et par vague.
 */
public class HurricaneAttack implements CsBossAttack {

    private static final int WAVE2_AT = 120;
    private static final int DURATION = 240;
    private static final double PERLIN_SCALE = 0.2;
    private static final double COVERAGE_THRESHOLD = 0.57; // ~35 %
    private static final double LAUNCH_UP = 1.1;
    private static final int NAUSEA_TICKS = 100;
    private static final float DAMAGE = 3.0F; // base 3

    private final String id;
    private final CsBossTileField tiles = new CsBossTileField();
    private int tick;
    private boolean done;

    public HurricaneAttack(String id) {
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
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (tick == 0 || tick == WAVE2_AT) {
            placeWave(level, session, boss);
        }
        tiles.tick(level, session, tick, (player, batch) -> {
            player.setDeltaMovement(player.getDeltaMovement().x, LAUNCH_UP, player.getDeltaMovement().z);
            player.hurtMarked = true; // une seule impulsion, simulée par le client
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0));
            CsBossDamage.hurt(player, session, DAMAGE);
            CsBossAttackLib.sound(level, player.getX(), player.getY(), player.getZ(),
                    "minecraft:entity.wind_charge.wind_burst", SoundSource.HOSTILE, 1.0F, 1.0F);
        });
        if (++tick >= DURATION) {
            done = true;
        }
    }

    private void placeWave(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        PerlinNoise noise = new PerlinNoise(level.getRandom().nextLong());
        List<BlockPos> chosen = new ArrayList<>();
        for (BlockPos surface : CsBossSurfaceScanner.scanSurface(level, session)) {
            double n = noise.noise2dNormalized(surface.getX() * PERLIN_SCALE, surface.getZ() * PERLIN_SCALE);
            if (n > COVERAGE_THRESHOLD) {
                chosen.add(surface);
            }
        }
        tiles.place(level, chosen, ModBlocks.CSBOSS_UPDRAFT.defaultBlockState(), tick,
                CsBossUpdraftTileBlock.WARN_TICKS, CsBossUpdraftTileBlock.ACTIVE_TICKS);
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.gust.actor", SoundSource.HOSTILE, 1.6F, 0.7F);
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
