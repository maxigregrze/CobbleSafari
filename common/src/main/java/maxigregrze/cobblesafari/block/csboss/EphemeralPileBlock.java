package maxigregrze.cobblesafari.block.csboss;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.misc.PileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ephemeral sludge/mud pile placed by boss attacks. Extends {@link PileBlock}:
 * passable (empty collision), heavily slows entities that pass through ({@code makeStuckInBlock}),
 * and applies poison for the sludge variant ({@code poison = true}). Self-destructs after
 * {@code ttlTicks} via {@code scheduleTick} (same pattern as {@link MeteoriteBlock}).
 */
public class EphemeralPileBlock extends PileBlock {

    private static final int POISON_DURATION_TICKS = 40; // 2 s (same as SludgePileBlock)

    private final int ttlTicks;
    private final boolean poison;

    public EphemeralPileBlock(Properties properties, int ttlTicks, boolean poison) {
        super(properties);
        this.ttlTicks = ttlTicks;
        this.poison = poison;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new EphemeralPileBlock(props, this.ttlTicks, this.poison));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity); // slowdown (makeStuckInBlock)
        if (this.poison && !level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, 0));
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, ttlTicks);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.levelEvent(2001, pos, Block.getId(state)); // break particles + sound
        level.removeBlock(pos, false);
    }
}
