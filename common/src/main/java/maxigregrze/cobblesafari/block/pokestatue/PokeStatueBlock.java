package maxigregrze.cobblesafari.block.pokestatue;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.manager.BannedItemsManager;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Two-block-tall configurable Pokémon statue.
 *
 * <p>The lower half carries the pedestal model and the configuration block entity; the upper half is
 * rendered entirely by {@code PokeStatueBlockEntityRenderer}, which draws the species model borrowed
 * from Cobblemon.</p>
 *
 * <p>Both halves carry {@code GLAZED} (honeycomb lock) and {@code HOLOGRAM} (mirror of material 3).
 * {@code HOLOGRAM} has to be a block state and not block-entity data because {@code lightLevel} is a
 * function of {@link BlockState} and the hologram finish emits light level 10.</p>
 *
 * <p>Unlike {@code UnionRoomGlobeBlock}, survivability is checked <b>only at placement time</b>:
 * mining the block underneath leaves the statue floating on purpose.</p>
 */
public class PokeStatueBlock extends AbstractPokeStatueBlock {

    public static final MapCodec<PokeStatueBlock> CODEC = simpleCodec(PokeStatueBlock::new);

    public static final BooleanProperty GLAZED = BooleanProperty.create("glazed");
    public static final BooleanProperty HOLOGRAM = BooleanProperty.create("hologram");
    /**
     * Mirror of "a colour is configured". Selects the pedestal model that bakes the tinted overlay
     * as a second layer, so the overlay lives in the chunk mesh instead of a per-frame renderer pass
     * (which z-fought with the base and flickered). Same recipe as {@code rusted_iron_block}.
     */
    public static final BooleanProperty TINTED = BooleanProperty.create("tinted");

    /** Pedestal outline: 12x12x16. Collision is a full cube on both halves. */
    private static final VoxelShape LOWER_OUTLINE = Block.box(2, 0, 2, 14, 16, 14);

    /**
     * Vanilla level events, matching copper waxing exactly: 3003 spawns WAX_ON particles on every
     * block face and plays HONEYCOMB_WAX_ON; 3004 spawns WAX_OFF particles only.
     */
    private static final int LEVEL_EVENT_WAX_ON = 3003;
    private static final int LEVEL_EVENT_WAX_OFF = 3004;
    /** Vanilla level event 1501 = lava fizz: extinguish sound + 8 large smoke particles. */
    private static final int LEVEL_EVENT_LAVA_FIZZ = 1501;

    public PokeStatueBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                .setValue(GLAZED, Boolean.FALSE)
                .setValue(HOLOGRAM, Boolean.FALSE)
                .setValue(TINTED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends AbstractPokeStatueBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.DOUBLE_BLOCK_HALF, GLAZED, HOLOGRAM, TINTED);
    }

    // -- shape / rendering -------------------------------------------------

    private static boolean isLower(BlockState state) {
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isLower(state) ? LOWER_OUTLINE : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // The upper half and the hologram pedestal are drawn by the block entity renderer.
        return isLower(state) && !state.getValue(HOLOGRAM) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    // -- placement / destruction -------------------------------------------

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1) {
            return null;
        }
        if (!level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (isLower(state)) {
            level.setBlock(pos.above(),
                    state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER),
                    Block.UPDATE_ALL);
        }
    }

    /**
     * Survivability is intentionally never re-checked after placement: the statue stays put when the
     * ground under it is mined. Cohesion between the two halves is handled by {@link #onRemove}.
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    /**
     * Mining the upper half destroys the lower half <em>with</em> its drop first, so the loot table
     * (conditioned on {@code half=lower}) yields exactly one item whichever half the player breaks.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !isLower(state) && !player.isCreative()) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.getBlock() == this && isLower(belowState)) {
                level.destroyBlock(below, true, player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Removes the twin half whenever one goes away - covers explosions and world edits, not just
     * player breaks. The {@code is(this)} guard breaks the recursion: the second pass finds no twin.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(other);
            if (otherState.getBlock() == this
                    && otherState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != half) {
                level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PokeStatueBlockEntity(pos, state);
    }

    // -- interaction --------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.HONEYCOMB)) {
            return applyGlaze(stack, state, level, pos, player);
        }
        if (stack.getItem() instanceof AxeItem && !stack.is(ModItems.TINKHAMMER)) {
            return removeGlaze(stack, state, level, pos, player, hand);
        }
        if (stack.is(ModItems.TINKHAMMER)) {
            return tinkhammerInteract(stack, state, level, pos, player);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        // Creative shift-right-click with empty hands opens the config without the Tinkhammer
        // (plan 188, C4). Vanilla only reaches here while sneaking when both hands are empty.
        if (!player.isCreative() || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(GLAZED)) {
            refuse(level, pos);
            return InteractionResult.CONSUME;
        }
        return openConfig(level, pos, state, player) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /** Position of the data-bearing (lower) half for a clicked block. */
    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                ? pos.below() : pos;
    }

    @Override
    public BlockPos dataPos(BlockState state, BlockPos pos) {
        return lowerPos(state, pos);
    }

    /** The Pokémon stands on the pedestal cap, which overhangs 4 pixels into the upper half. */
    @Override
    public double modelOriginY() {
        return 0.25;
    }

    /**
     * Shared Tinkhammer logic - also invoked from {@link maxigregrze.cobblesafari.item.TinkhammerItem}
     * so the tool still reaches the handler when vanilla skips block activation.
     */
    public static ItemInteractionResult tinkhammerInteract(ItemStack stack, BlockState state, Level level,
                                                           BlockPos pos, @Nullable Player player) {
        if (!stack.is(ModItems.TINKHAMMER) || player == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (state.getValue(GLAZED)) {
            refuse(level, pos);
            return ItemInteractionResult.SUCCESS;
        }
        // Creative players are exempt from the dimensional block-breaking ban for editing only.
        if (!player.isCreative() && !BannedItemsManager.isBlockBreakingAllowed(level.dimension())) {
            refuse(level, pos);
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof PokeStatueBlock block)
                || !block.openConfig(level, pos, state, player)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.SUCCESS;
    }

    private ItemInteractionResult applyGlaze(ItemStack stack, BlockState state, Level level,
                                             BlockPos pos, Player player) {
        if (state.getValue(GLAZED)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        // Waxing is a block edit: gated even in creative.
        if (!BannedItemsManager.isBlockBreakingAllowed(level.dimension())) {
            refuse(level, pos);
            return ItemInteractionResult.SUCCESS;
        }
        setGlazedBothHalves(level, pos, state, true);
        // null, not player: the Player argument of levelEvent / playSound is the recipient to
        // EXCLUDE (PlayerList.broadcast skips it). Vanilla passes the actor because HoneycombItem
        // runs client-side too and predicts the effect locally - this handler is server-only and
        // returns early on the client, so passing the actor would leave them with no feedback at all.
        level.levelEvent(null, LEVEL_EVENT_WAX_ON, pos, 0);
        stack.consume(1, player);
        return ItemInteractionResult.SUCCESS;
    }

    private ItemInteractionResult removeGlaze(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand) {
        if (!state.getValue(GLAZED)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!BannedItemsManager.isBlockBreakingAllowed(level.dimension())) {
            refuse(level, pos);
            return ItemInteractionResult.SUCCESS;
        }
        setGlazedBothHalves(level, pos, state, false);
        // null for the same reason as in applyGlaze: it is an exclusion, not a target. Level event
        // 3004 carries only the WAX_OFF particles, so the sound has to be played separately - unlike
        // 3003, which bundles the HONEYCOMB_WAX_ON sound with its particles.
        level.playSound(null, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.levelEvent(null, LEVEL_EVENT_WAX_OFF, pos, 0);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return ItemInteractionResult.SUCCESS;
    }

    /** Rain-on-lava fizz: the feedback for a refused interaction. */
    private static void refuse(Level level, BlockPos pos) {
        level.levelEvent(null, LEVEL_EVENT_LAVA_FIZZ, pos, 0);
    }

    private void setGlazedBothHalves(Level level, BlockPos pos, BlockState state, boolean glazed) {
        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        level.setBlock(pos, state.setValue(GLAZED, glazed), Block.UPDATE_ALL);
        BlockState otherState = level.getBlockState(other);
        if (otherState.getBlock() == this
                && otherState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != half) {
            level.setBlock(other, otherState.setValue(GLAZED, glazed), Block.UPDATE_ALL);
        }
    }
}
