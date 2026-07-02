package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Shared base for blocks oriented on the full {@link DirectionalBlock#FACING} (six directions:
 * up / down / north / south / east / west) that are just a custom model + orientation, with no
 * bespoke behaviour. Everything is driven by {@link Settings}: the shape authored for {@code UP},
 * whether that shape rotates with the facing's axis, the default facing and the collision mode.
 *
 * <p>By default a block places with {@code FACING = } the clicked face (floor click ⇒ UP, wall
 * click ⇒ that side, ceiling click ⇒ DOWN). Blocks that need a real placement/survival rule may
 * extend this and override only {@link #getStateForPlacement} — see {@code HyperspaceLogBlock}.</p>
 */
public class DirectionalModelBlock extends DirectionalBlock {

    /** Collision behaviour. */
    public enum Collision { SHAPE, NONE, FULL }

    /** Immutable configuration for a {@link DirectionalModelBlock}. Build via {@link #builder()}. */
    public record Settings(
            @Nullable VoxelShape upShape,
            boolean rotateShape,
            Direction defaultFacing,
            Collision collision) {

        public static Builder builder() {
            return new Builder();
        }

        /** Plain oriented full cube (no custom shape). */
        public static Settings cube() {
            return builder().build();
        }

        public static final class Builder {
            private VoxelShape upShape = null;
            private boolean rotateShape = false;
            private Direction defaultFacing = Direction.UP;
            private Collision collision = Collision.SHAPE;

            public Builder shape(VoxelShape shape) { this.upShape = shape; return this; }
            public Builder rotateShape() { this.rotateShape = true; return this; }
            public Builder defaultFacing(Direction facing) { this.defaultFacing = facing; return this; }
            public Builder collision(Collision collision) { this.collision = collision; return this; }

            public Settings build() {
                return new Settings(upShape, rotateShape, defaultFacing, collision);
            }
        }
    }

    private final Settings settings;
    private final VoxelShape baseShape;
    @Nullable
    private final Map<Direction.Axis, VoxelShape> axisShapes;

    public DirectionalModelBlock(Properties properties, Settings settings) {
        super(properties);
        this.settings = settings;
        this.baseShape = settings.upShape() != null ? settings.upShape() : Shapes.block();
        if (settings.rotateShape() && settings.upShape() != null) {
            Map<Direction.Axis, VoxelShape> map = new EnumMap<>(Direction.Axis.class);
            for (Direction.Axis axis : Direction.Axis.values()) {
                map.put(axis, BlockShapeUtils.rotatePillar(settings.upShape(), axis));
            }
            this.axisShapes = map;
        } else {
            this.axisShapes = null;
        }
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, settings.defaultFacing()));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(props -> new DirectionalModelBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (axisShapes != null) {
            return axisShapes.getOrDefault(state.getValue(FACING).getAxis(), baseShape);
        }
        return baseShape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (settings.collision()) {
            case SHAPE -> getShape(state, level, pos, context);
            case NONE -> Shapes.empty();
            case FULL -> Shapes.block();
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
