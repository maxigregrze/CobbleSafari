package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class PokemonStatueBlock extends HorizontalDirectionalBlock {

    public static final VoxelShape BULBASAUR_SHAPE = Block.box(0, 0, 5, 16, 16, 16);
    public static final VoxelShape CHARMANDER_SHAPE = Block.box(0, 0, 7, 16, 16, 16);
    public static final VoxelShape PIKACHU_SHAPE = Block.box(0, 0, 6, 16, 16, 16);
    public static final VoxelShape SQUIRTLE_SHAPE = Block.box(0, 0, 5, 16, 16, 16);
    public static final VoxelShape KARATE_MANNEQUIN_SHAPE = Block.box(4, 0, 4, 12, 16, 16);

    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    protected PokemonStatueBlock(VoxelShape southShape, Properties properties) {
        super(properties);
        AABB bounds = southShape.bounds();
        this.minX = bounds.minX * 16;
        this.minY = bounds.minY * 16;
        this.minZ = bounds.minZ * 16;
        this.maxX = bounds.maxX * 16;
        this.maxY = bounds.maxY * 16;
        this.maxZ = bounds.maxZ * 16;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    private VoxelShape shapeForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> Block.box(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
            case NORTH -> Block.box(16 - this.maxX, this.minY, 16 - this.maxZ, 16 - this.minX, this.maxY, 16 - this.minZ);
            case EAST -> Block.box(this.minZ, this.minY, 16 - this.maxX, this.maxZ, this.maxY, 16 - this.minX);
            case WEST -> Block.box(16 - this.maxZ, this.minY, this.minX, 16 - this.minZ, this.maxY, this.maxX);
            default -> Block.box(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
        };
    }

    public static final class Bulbasaur extends PokemonStatueBlock {
        public static final MapCodec<Bulbasaur> CODEC = simpleCodec(Bulbasaur::new);

        public Bulbasaur(Properties properties) {
            super(BULBASAUR_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }

    public static final class Charmander extends PokemonStatueBlock {
        public static final MapCodec<Charmander> CODEC = simpleCodec(Charmander::new);

        public Charmander(Properties properties) {
            super(CHARMANDER_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }

    public static final class Pikachu extends PokemonStatueBlock {
        public static final MapCodec<Pikachu> CODEC = simpleCodec(Pikachu::new);

        public Pikachu(Properties properties) {
            super(PIKACHU_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }

    public static final class Squirtle extends PokemonStatueBlock {
        public static final MapCodec<Squirtle> CODEC = simpleCodec(Squirtle::new);

        public Squirtle(Properties properties) {
            super(SQUIRTLE_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }

    public static final class KarateMannequin extends PokemonStatueBlock {
        public static final MapCodec<KarateMannequin> CODEC = simpleCodec(KarateMannequin::new);

        public KarateMannequin(Properties properties) {
            super(KARATE_MANNEQUIN_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }
}
