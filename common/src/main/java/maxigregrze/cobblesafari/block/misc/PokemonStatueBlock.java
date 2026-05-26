package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class PokemonStatueBlock extends Block {

    public static final VoxelShape BULBASAUR_SHAPE = Block.box(0, 0, 5, 16, 16, 16);
    public static final VoxelShape CHARMANDER_SHAPE = Block.box(0, 0, 7, 16, 16, 16);
    public static final VoxelShape PIKACHU_SHAPE = Block.box(0, 0, 6, 16, 16, 16);
    public static final VoxelShape SQUIRTLE_SHAPE = Block.box(0, 0, 5, 16, 16, 16);

    private final VoxelShape shape;

    protected PokemonStatueBlock(VoxelShape shape, Properties properties) {
        super(properties);
        this.shape = shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    public static final class Bulbasaur extends PokemonStatueBlock {
        public static final MapCodec<Bulbasaur> CODEC = simpleCodec(Bulbasaur::new);

        public Bulbasaur(Properties properties) {
            super(BULBASAUR_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }
    }

    public static final class Charmander extends PokemonStatueBlock {
        public static final MapCodec<Charmander> CODEC = simpleCodec(Charmander::new);

        public Charmander(Properties properties) {
            super(CHARMANDER_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }
    }

    public static final class Pikachu extends PokemonStatueBlock {
        public static final MapCodec<Pikachu> CODEC = simpleCodec(Pikachu::new);

        public Pikachu(Properties properties) {
            super(PIKACHU_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }
    }

    public static final class Squirtle extends PokemonStatueBlock {
        public static final MapCodec<Squirtle> CODEC = simpleCodec(Squirtle::new);

        public Squirtle(Properties properties) {
            super(SQUIRTLE_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }
    }
}
