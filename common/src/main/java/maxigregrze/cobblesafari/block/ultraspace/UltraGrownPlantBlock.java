package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.FaceAttachedModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Grown Ultra Beast plant (action plan 192, section 12): what a stage-8 crop becomes once its
 * fruit is picked, and what a stage 5-8 crop drops. Purely decorative: floor, ceiling or wall, four
 * facings on floor and ceiling, a box centred in the cell.
 * <ul>
 *   <li>Support: the block behind it must be in its support tag (soil, plus logs or sand-like
 *   blocks). Farmland also holds it - that is where the harvest leaves it - but a player cannot
 *   place it there.</li>
 *   <li>{@link #SUNKEN}: on farmland (15 px tall) the model is drawn 1 px lower, like the crop it
 *   came from. Kept up to date as the block below changes.</li>
 *   <li>Optionally stinging, like a cactus, without its block-breaking side.</li>
 * </ul>
 */
public class UltraGrownPlantBlock extends FaceAttachedModelBlock {

    public static final BooleanProperty SUNKEN = BooleanProperty.create("sunken");

    private final Settings plantSettings;
    private final TagKey<Block> supports;
    private final boolean stinging;

    public UltraGrownPlantBlock(Properties properties, Settings settings, TagKey<Block> supports, boolean stinging) {
        super(properties, settings);
        this.plantSettings = settings;
        this.supports = supports;
        this.stinging = stinging;
        registerDefaultState(defaultBlockState().setValue(SUNKEN, false));
    }

    /** Floor box of a plant {@code height} px tall and 10 px wide, and its five other orientations. */
    public static Settings settingsFor(int height) {
        int lo = 3;
        int hi = 13;
        return Settings.builder()
                .shapes(Block.box(lo, 0, lo, hi, height, hi),
                        Block.box(lo, 16 - height, lo, hi, 16, hi),
                        Block.box(lo, lo, 16 - height, hi, hi, 16),
                        Block.box(lo, lo, 0, hi, hi, height),
                        Block.box(0, lo, lo, height, hi, hi),
                        Block.box(16 - height, lo, lo, 16, hi, hi))
                .build();
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new UltraGrownPlantBlock(props, plantSettings, supports, stinging));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SUNKEN);
    }

    /** {@code state} with {@link #SUNKEN} matching the block under {@code pos}. */
    public static BlockState withSink(BlockState state, LevelReader level, BlockPos pos) {
        boolean onFarmland = state.getValue(FACE) == AttachFace.FLOOR
                && level.getBlockState(pos.below()).getBlock() instanceof FarmBlock;
        return state.setValue(SUNKEN, onFarmland);
    }

    private BlockState supportOf(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(getConnectedDirection(state).getOpposite()));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState support = supportOf(state, level, pos);
        return support.is(supports) || support.getBlock() instanceof FarmBlock;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        // Farmland only ever receives the plant from a harvest, never from a player's hand.
        if (state == null || !supportOf(state, context.getLevel(), context.getClickedPos()).is(supports)) {
            return null;
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (updated.is(this) && direction == Direction.DOWN) {
            return withSink(updated, level, pos);
        }
        return updated;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (stinging && entity instanceof LivingEntity) {
            entity.hurt(level.damageSources().cactus(), 1.0f);
        }
    }
}
