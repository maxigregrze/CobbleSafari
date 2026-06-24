package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Orientable Pokémon statue. Free‑standing (no support requirement); the hitbox is aligned
 * to the model and rotated by FACING via {@link HorizontalModelBlock} (shape authored for
 * {@link Direction#SOUTH}, default facing SOUTH).
 */
public abstract class PokemonStatueBlock extends HorizontalModelBlock {

    public static final VoxelShape BULBASAUR_SHAPE = Block.box(0, 0, 5, 16, 16, 16);
    public static final VoxelShape CHARMANDER_SHAPE = Block.box(0, 0, 7, 16, 16, 16);
    public static final VoxelShape PIKACHU_SHAPE = Block.box(0, 0, 6, 16, 16, 16);
    public static final VoxelShape SQUIRTLE_SHAPE = Block.box(0, 0, 5, 16, 16, 16);

    protected PokemonStatueBlock(VoxelShape southShape, Properties properties) {
        super(properties, Settings.builder()
                .shape(southShape)
                .authoredFacing(Direction.SOUTH)
                .rotateShape()
                .defaultFacing(Direction.SOUTH)
                .support(Support.NONE)
                .emptyOcclusion()
                .build());
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
}
