package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Orientable tombstone. Placeable only on solid ground, mined with pickaxe, drops itself.
 * Hitbox aligned to the model and rotated by FACING — all handled by
 * {@link HorizontalModelBlock} (the shape is authored for {@link Direction#SOUTH}).
 */
public abstract class TombstoneBlock extends HorizontalModelBlock {

    public static final VoxelShape TOMBSTONE_SHAPE = Block.box(1, 0, 0, 15, 8, 16);
    public static final VoxelShape TOMBSTONE_SMALL_SHAPE = Block.box(0, 0, 5, 16, 16, 11);

    protected TombstoneBlock(VoxelShape southShape, Properties properties) {
        super(properties, Settings.builder()
                .shape(southShape)
                .authoredFacing(Direction.SOUTH)
                .rotateShape()
                .support(Support.GROUND)
                .emptyOcclusion()
                .build());
    }

    public static final class Standard extends TombstoneBlock {
        public static final MapCodec<Standard> CODEC = simpleCodec(Standard::new);

        public Standard(Properties properties) {
            super(TOMBSTONE_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }

    public static final class Small extends TombstoneBlock {
        public static final MapCodec<Small> CODEC = simpleCodec(Small::new);

        public Small(Properties properties) {
            super(TOMBSTONE_SMALL_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }
}
