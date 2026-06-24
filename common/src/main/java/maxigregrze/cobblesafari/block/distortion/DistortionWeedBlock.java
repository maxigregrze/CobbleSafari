package maxigregrze.cobblesafari.block.distortion;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.FaceAttachedModelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Orthonormal (floor/ceiling/wall) distortion weed. Floor/ceiling placement faces the
 * player ({@link FloorFacing#PLAYER}); wall placement and survival are handled by
 * {@link FaceAttachedModelBlock}. The shapes are shared with
 * {@link DistortionStonebricksRubbleBlock} (which only differs by floor/ceiling facing).
 */
public class DistortionWeedBlock extends FaceAttachedModelBlock {

    public static final MapCodec<DistortionWeedBlock> CODEC = simpleCodec(DistortionWeedBlock::new);

    public static final VoxelShape FLOOR_SHAPE = Block.box(2, 0, 2, 14, 4, 14);
    public static final VoxelShape CEILING_SHAPE = Block.box(2, 12, 2, 14, 16, 14);
    public static final VoxelShape WALL_NORTH_SHAPE = Block.box(2, 2, 12, 14, 14, 16);
    public static final VoxelShape WALL_SOUTH_SHAPE = Block.box(2, 2, 0, 14, 14, 4);
    public static final VoxelShape WALL_EAST_SHAPE = Block.box(0, 2, 2, 4, 14, 14);
    public static final VoxelShape WALL_WEST_SHAPE = Block.box(12, 2, 2, 16, 14, 14);

    /** Shared settings factory (reused by the stonebricks rubble variant). */
    static Settings settings(boolean allowWallPlacement, FloorFacing floorCeilingFacing) {
        Settings.Builder builder = Settings.builder()
                .shapes(FLOOR_SHAPE, CEILING_SHAPE, WALL_NORTH_SHAPE, WALL_SOUTH_SHAPE, WALL_EAST_SHAPE, WALL_WEST_SHAPE)
                .floorFacing(floorCeilingFacing);
        if (!allowWallPlacement) {
            builder.noWall();
        }
        return builder.build();
    }

    public DistortionWeedBlock(Properties properties) {
        this(properties, true);
    }

    public DistortionWeedBlock(Properties properties, boolean allowWallPlacement) {
        super(properties, settings(allowWallPlacement, FloorFacing.PLAYER));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
