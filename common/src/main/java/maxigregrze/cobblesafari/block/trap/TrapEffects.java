package maxigregrze.cobblesafari.block.trap;

import maxigregrze.cobblesafari.init.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class TrapEffects {

    private static final int SOFT_BLOCK_RADIUS = 2;
    private static final float BASE_RADIUS = 1.6F;
    private static final float HARD_RADIUS = 2.2F;

    private TrapEffects() {}

    /** Explosion with the default (audible) explosion sound. */
    static void explode(ServerLevel level, BlockPos pos, boolean hard) {
        explode(level, pos, hard, null);
    }

    /**
     * Explosion that deals damage/knockback without breaking blocks (base) or breaking only soft blocks (hard).
     * When {@code silentSound} is non-null it replaces the default explosion boom — used by the fart trap so its
     * own sound can be heard. Trap supports are preserved so a hard blast never knocks a trap off (no self-drop).
     */
    static void explode(ServerLevel level, BlockPos pos, boolean hard, @Nullable Holder<SoundEvent> silentSound) {
        Vec3 center = Vec3.atCenterOf(pos);
        float radius = hard ? HARD_RADIUS : BASE_RADIUS;
        if (silentSound == null) {
            level.explode(null, level.damageSources().explosion(null, null),
                    null, center.x, center.y, center.z, radius, false, Level.ExplosionInteraction.NONE);
        } else {
            level.explode(null, level.damageSources().explosion(null, null),
                    null, center.x, center.y, center.z, radius, false, Level.ExplosionInteraction.NONE,
                    ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, silentSound);
        }
        if (hard) {
            // The hard explosion/fart blows the trap up: it breaks without dropping itself
            // (removeBlock never drops). Done before destroySoftBlocks so its former support is cleared too.
            level.removeBlock(pos, false);
            destroySoftBlocks(level, pos);
        }
    }

    private static void destroySoftBlocks(ServerLevel level, BlockPos pos) {
        int r = SOFT_BLOCK_RADIUS;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
            if (p.distSqr(pos) > r * r) {
                continue;
            }
            if (!level.getBlockState(p).is(ModBlockTags.TRAP_SOFT_BLOCKS)) {
                continue;
            }
            // Never remove a block that supports a trap, otherwise the trap would break and drop itself.
            if (level.getBlockState(p.above()).getBlock() instanceof TrapBlock) {
                continue;
            }
            level.destroyBlock(p, false);
        }
    }
}
