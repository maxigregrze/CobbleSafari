package maxigregrze.cobblesafari.block.distortion;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.FaceAttachedModelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Orthonormal distortion boulder: oversized custom model on floor/ceiling/wall, with a
 * full‑cube collision box. Floor/ceiling placement faces away from the player.
 */
public class DistortionBoulderBlock extends FaceAttachedModelBlock {

    public static final MapCodec<DistortionBoulderBlock> CODEC = simpleCodec(DistortionBoulderBlock::new);

    private static final VoxelShape SHAPE_FLOOR      = Block.box(-4,  0, -4, 20, 18, 20);
    private static final VoxelShape SHAPE_CEILING    = Block.box(-4, -2, -4, 20, 16, 20);
    private static final VoxelShape SHAPE_WALL_NORTH = Block.box(-4, -4, -2, 20, 20, 16);
    private static final VoxelShape SHAPE_WALL_SOUTH = Block.box(-4, -4,  0, 20, 20, 18);
    private static final VoxelShape SHAPE_WALL_EAST  = Block.box( 0, -4, -4, 18, 20, 20);
    private static final VoxelShape SHAPE_WALL_WEST  = Block.box(-2, -4, -4, 16, 20, 20);

    public DistortionBoulderBlock(Properties properties) {
        super(properties, Settings.builder()
                .shapes(SHAPE_FLOOR, SHAPE_CEILING, SHAPE_WALL_NORTH, SHAPE_WALL_SOUTH, SHAPE_WALL_EAST, SHAPE_WALL_WEST)
                .floorFacing(FloorFacing.OPPOSITE)
                .collision(Collision.FULL)
                .build());
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
