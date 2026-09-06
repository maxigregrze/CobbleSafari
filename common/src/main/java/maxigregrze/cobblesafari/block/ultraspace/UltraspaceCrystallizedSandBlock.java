package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Strange Crystallized Sand: Strange Sand that buds. Growth is budding-amethyst parity - one chance
 * in five per random tick, one face drawn at random, a shard placed if that neighbour is air or
 * water. Like budding amethyst it drops nothing when broken.
 */
public class UltraspaceCrystallizedSandBlock extends FallingBlock {

    /** Parity with {@code BuddingAmethystBlock}: one chance in five per random tick. */
    private static final int GROWTH_CHANCE = 5;

    private static final Direction[] FACES = Direction.values();

    /**
     * Held here rather than inherited from {@code ColoredFallingBlock}: that class declares an
     * invariant {@code MapCodec<ColoredFallingBlock>} which a subclass cannot narrow, so the
     * family extends {@link FallingBlock} and carries its own dust colour.
     */
    private final ColorRGBA dustColor;

    public UltraspaceCrystallizedSandBlock(ColorRGBA dustColor, Properties properties) {
        super(properties);
        this.dustColor = dustColor;
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return simpleCodec(props -> new UltraspaceCrystallizedSandBlock(this.dustColor, props));
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return dustColor.rgba();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // FallingBlock schedules an ordinary tick from onPlace and does not use the random tick, but
        // the super call keeps the parent's contract intact if that ever changes.
        super.randomTick(state, level, pos, random);
        if (random.nextInt(GROWTH_CHANCE) != 0) {
            return;
        }
        Direction face = FACES[random.nextInt(FACES.length)];
        BlockPos target = pos.relative(face);
        BlockState neighbour = level.getBlockState(target);
        boolean water = neighbour.getFluidState().getType() == Fluids.WATER;
        if (!neighbour.isAir() && !water) {
            return;
        }
        level.setBlockAndUpdate(target, ModBlocks.ULTRASPACE_CRYSTAL.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, face)
                .setValue(AmethystClusterBlock.WATERLOGGED, water));
    }
}
