package maxigregrze.cobblesafari.block.csboss;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.ApparentState;
import maxigregrze.cobblesafari.block.base.ApparentStateBlock;
import maxigregrze.cobblesafari.block.base.NeighborDerivedBlock;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * CSBoss-reactive "mimic" block. Toggles between invisible/no-hitbox and a solid block whose
 * appearance, connections and hitbox are copied from a configurable target block (rendered by the
 * BER).
 *
 * <p>{@code active} is posted by the battle scan ({@link BattleReactiveBlock}); {@code reverse} is a
 * creative GUI option. The block is "solid" (visible + hitbox) when {@code active == reverse}:
 * default (reverse=false) means solid when idle, invisible during a fight; reverse=true flips it.
 * In creative the selection box is always shown (even while invisible); in survival there is no
 * hitbox when invisible.
 *
 * <p><strong>Disguise.</strong> While solid, the mimic reads as the block it copies - see
 * {@link ApparentStateBlock} and {@link ApparentState}. A tower of mimics set to a connected block
 * therefore draws the same run of models a real tower would, and a real connected block standing
 * next to one connects to it. While invisible the disguise is dropped, so neighbours re-shape as if
 * the mimic were not there. The resolved state itself lives on the block entity and is recomputed by
 * {@link #refresh}.
 *
 * <p>The disguise reaches vanilla too. The mod's own connected blocks resolve a mimic through
 * {@link ApparentState}, which nothing in vanilla does; a vanilla fence or wall is instead handed the
 * disguise as the neighbour state by {@link #refresh} and {@link #tellNeighbour}, which is the one
 * channel its {@code connectsTo} reads. In the other direction {@link #walkShape} replays vanilla's
 * own six neighbour updates, so a mimicked fence, wall, pane, bar, redstone wire or stair works out
 * its own connections. Anything solid was already connectable, through the face-sturdiness branch
 * that {@code dynamicShape} restored.
 *
 * <p>Not inherited from the target: {@code LIT} (set on placement and random tick, never by the
 * connection engine, so a mimicked window always reads unlit), {@code WATERLOGGED}, multiblock parts
 * and player-chosen facings - which leaves fence gates out, since theirs is player-chosen. A wall's
 * post is partial: {@code WallBlock} reads the block above straight off the level rather than through
 * us, so it weighs our collision shape but never recognises a mimic up there as a wall. Mobs also
 * still path against the plain cube, because {@code isPathfindable} is asked with an empty block
 * getter and so cannot see the block entity.
 */
public class CsBossMimicBlock extends BaseEntityBlock implements BattleReactiveBlock, ApparentStateBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty REVERSE = BooleanProperty.create("reverse");
    /** Swap mode (plan 199, iteration 5): always solid, copies one block outside a fight, another during it. */
    public static final BooleanProperty SWAP = BooleanProperty.create("swap");
    public static final MapCodec<CsBossMimicBlock> CODEC = simpleCodec(CsBossMimicBlock::new);

    private static final VoxelShape FULL = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    /** Budget handed to a reciprocal shape update, matching vanilla's own default. */
    private static final int RECIPROCAL_BUDGET = 512;

    public CsBossMimicBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ACTIVE, false)
                .setValue(REVERSE, false)
                .setValue(SWAP, false));
    }

    /**
     * The three behaviours offered by the config screen, stored as the {@code reverse} and {@code swap}
     * properties (a structure saved before {@code swap} existed loads it as {@code false}).
     */
    public enum Mode {
        /** Visible outside a fight, gone during it. */
        HIDE_IN_BATTLE,
        /** Gone outside a fight, visible during it. */
        SHOW_IN_BATTLE,
        /** Always there: the idle block outside a fight, the battle block during it. */
        SWAP;

        public static Mode of(BlockState state) {
            if (state.getValue(CsBossMimicBlock.SWAP)) {
                return SWAP;
            }
            return state.getValue(REVERSE) ? SHOW_IN_BATTLE : HIDE_IN_BATTLE;
        }

        public BlockState apply(BlockState state) {
            return state.setValue(CsBossMimicBlock.SWAP, this == SWAP).setValue(REVERSE, this == SHOW_IN_BATTLE);
        }

        public static Mode byIndex(int index) {
            Mode[] all = values();
            return all[Math.floorMod(index, all.length)];
        }
    }

    /** Visible + hitbox when active and reverse agree, or always in swap mode. */
    public static boolean isSolid(BlockState state) {
        return state.getValue(SWAP) || state.getValue(ACTIVE) == state.getValue(REVERSE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, REVERSE, SWAP);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CsBossMimicBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        // Universal self-heal. onPlace cannot serve here: a chunk creates the block entity *after*
        // calling it, so the first resolution has to wait for the next tick - which also covers
        // /setblock, /fill, structures, chunk load and worlds saved before this feature existed.
        return createTickerHelper(type, ModBlockEntities.CSBOSS_MIMIC, CsBossMimicBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Always rendered by the BER (copies the target block's model); nothing from the blockstate.
        return RenderShape.INVISIBLE;
    }

    // ------------------------------------------------------------------ disguise

    /**
     * The block this mimic copies. Falls back to {@code distortion_stone_bricks} for a blank,
     * unparseable or unregistered id, and for another mimic - which would otherwise recurse through
     * {@link #getShape} and {@link ApparentState} alike.
     */
    public static Block targetBlock(String id) {
        if (id != null && !id.isBlank()) {
            ResourceLocation loc = ResourceLocation.tryParse(id.trim());
            if (loc != null) {
                Block block = BuiltInRegistries.BLOCK.getOptional(loc).orElse(null);
                if (block != null && !(block instanceof CsBossMimicBlock)) {
                    return block;
                }
            }
        }
        return ModBlocks.DISTORTION_STONE_BRICKS;
    }

    /** The full state the target block would carry at {@code pos}, connections included. */
    public static BlockState resolve(String id, LevelAccessor level, BlockPos pos) {
        Block target = targetBlock(id);
        return target instanceof NeighborDerivedBlock derived
                ? derived.apparentStateAt(level, pos)
                : walkShape(target, level, pos);
    }

    /**
     * The fallback for a target that is not one of the mod's connected blocks: replay the six
     * neighbour updates vanilla itself performs, starting from the default state. Fences, walls,
     * panes, iron bars, redstone wire, chorus plant and stair corners all derive their state through
     * {@code updateShape}, so this covers them without naming a single one.
     *
     * <p>{@code UPDATE_SHAPE_ORDER} is vanilla's own, and the order matters: UP comes last, which is
     * what lets a wall settle its sides before deciding whether to raise its post. Each neighbour is
     * read through {@link ApparentState}, so one mimicked fence connects to the next.
     *
     * <p>A block may schedule a tick on itself from here - a waterlogged wall does. That is harmless:
     * the tick is filed for the target block at our position and {@code ServerLevel.tickBlock} drops
     * any tick whose block is not the one actually standing there.
     */
    private static BlockState walkShape(Block target, LevelAccessor level, BlockPos pos) {
        BlockState state = target.defaultBlockState();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        for (Direction direction : UPDATE_SHAPE_ORDER) {
            neighborPos.setWithOffset(pos, direction);
            BlockState updated = state.updateShape(direction, ApparentState.at(level, neighborPos),
                    level, pos, neighborPos);
            // A failed support check turns the block to air, or to something else entirely. It is not
            // really standing here, so keep the plain default rather than adopt that verdict.
            if (!updated.is(target)) {
                return target.defaultBlockState();
            }
            state = updated;
        }
        return state;
    }

    /**
     * Whether {@code state} already resolves its neighbours through {@link ApparentState} and so
     * needs no help seeing through the disguise. True for the mod's own connected blocks and for
     * other mimics; false for everything vanilla.
     */
    private static boolean readsApparentState(BlockState state) {
        Block block = state.getBlock();
        return block instanceof NeighborDerivedBlock || block instanceof ApparentStateBlock;
    }

    @Nullable
    @Override
    public BlockState apparentState(BlockState self, BlockGetter level, BlockPos pos) {
        if (!isSolid(self) || !(level.getBlockEntity(pos) instanceof CsBossMimicBlockEntity be)) {
            return null;
        }
        BlockState resolved = be.getResolvedMimicState();
        // Before the first server tick - and on a client not yet told - the identity is still known
        // from the id alone, which is all connection matching ever reads.
        return resolved != null ? resolved : targetBlock(be.getTargetId(self)).defaultBlockState();
    }

    /**
     * Recomputes the resolved state and, when it moved, tells the neighbourhood. Server side only.
     *
     * <p>Immediate rather than deferred: a per-block scheduled tick would make a tall tower unzip
     * over a second at every fight start. It is safe to run from inside {@code updateShape} because
     * it never writes a block state of its own - it writes block entity data and then re-enters the
     * ordinary shape-update machinery, exactly as {@code Block.updateOrDestroy} does.
     *
     * <p>Termination: only {@code LAYER} and {@code BOTTOM} read a neighbour's <em>resolved</em>
     * value, and both read strictly downward, so the propagating dependency graph is an upward DAG.
     * Everything else reads block identity only, which no cascade changes. A node whose value did
     * not move stops the walk; {@code force} exists for the two cases where the identity itself
     * changed (target edited, solidity flipped) and the resolved state may coincidentally match.
     */
    public static void refresh(LevelAccessor level, BlockPos pos, BlockState state, boolean force) {
        // ServerLevel and not merely "not client": a WorldGenRegion is neither client-side nor the
        // main thread, and sendBlockUpdated must not be reached from it. Structures placed at
        // worldgen are covered by the block entity ticker instead.
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof CsBossMimicBlockEntity be)) {
            return;
        }
        BlockState resolved = resolve(be.getTargetId(state), level, pos);
        boolean changed = be.setResolvedMimicState(resolved);
        if (!changed && !force) {
            return;
        }
        be.syncToClients();

        // Broadcast the disguise, not ourselves. updateNeighbourShapes hands this state to each
        // neighbour as "what your neighbour is", and that is the only channel through which a vanilla
        // fence or wall can be told to connect: their connectsTo reads the state they are given, and
        // no vanilla code path routes through ApparentState. Nothing is written at our position by
        // that call, so passing a state that is not literally here is safe.
        //
        // No stack guard is needed. On a Level these go through CollectingNeighborUpdater, which
        // queues them and drains the queue iteratively rather than recursing, under its own
        // chained-update cap. What bounds the work is the early return above: a mimic whose resolved
        // state did not move never re-broadcasts, and the propagating dependencies form an upward DAG.
        broadcastState(state, resolved).updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
    }

    /** What the neighbourhood should see us as: the disguise while solid, ourselves once invisible. */
    private static BlockState broadcastState(BlockState state, BlockState resolved) {
        return isSolid(state) ? resolved : state;
    }

    /**
     * Hands the disguise to the one neighbour that just changed. Vanilla placement bypasses every
     * hook we have - {@code FenceBlock.getStateForPlacement} reads {@code getBlockState} straight
     * from the level - so a fence put down beside a mimic arrives unconnected, and a refresh that
     * found nothing to change would never correct it. Skipped for blocks that read through
     * {@link ApparentState} already, and while invisible: dropping the disguise is carried by the
     * ordinary block update that flipped us.
     */
    private static void tellNeighbour(LevelAccessor level, BlockPos pos, BlockState state,
                                      Direction direction, BlockPos neighborPos) {
        // Deliberately the block actually standing there, not the state we were handed: a
        // neighbouring mimic passes us its disguise, and mistaking that for a vanilla block would
        // have the two of them trade reciprocal updates until the depth guard cut them off.
        if (!(level instanceof ServerLevel) || !isSolid(state)
                || readsApparentState(level.getBlockState(neighborPos))
                || !(level.getBlockEntity(pos) instanceof CsBossMimicBlockEntity be)) {
            return;
        }
        BlockState resolved = be.getResolvedMimicState();
        if (resolved == null) {
            return;
        }
        // Mirror of the call updateNeighbourShapes would have made: the direction is the one pointing
        // from the neighbour back at us. Self-limiting - the neighbour recomputes, and if that changed
        // it, its own write brings us straight back here, where it now recomputes to the same state
        // and updateOrDestroy writes nothing.
        level.neighborShapeChanged(direction.getOpposite(), resolved, neighborPos, pos,
                Block.UPDATE_ALL, RECIPROCAL_BUDGET);
    }

    /** Every neighbour write reaches us here; our own state is never rewritten. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        refresh(level, pos, state, false);
        tellNeighbour(level, pos, state, direction, neighborPos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState battleState(BlockState state) {
        return state.setValue(ACTIVE, true);
    }

    @Override
    public void setBattleState(ServerLevel level, BlockPos pos, BlockState state, boolean battle) {
        if (state.getValue(ACTIVE) != battle) {
            BlockState next = state.setValue(ACTIVE, battle);
            level.setBlock(pos, next, Block.UPDATE_ALL);
            // The setBlock already re-shaped our neighbours. This is for us: while invisible we
            // resolved against undisguised peers, so a whole arena coming back at once has to
            // recompute rather than trust what it worked out mid-fight.
            refresh(level, pos, next, true);
        }
    }

    // ------------------------------------------------------------------ shape and light

    /**
     * The mimicked block's shape, or the plain cube when there is nothing to ask. A target carrying
     * a block entity is deliberately not followed: it would recurse on a mimic of a mimic and read
     * <em>our</em> block entity as if it were a chest's.
     */
    private static VoxelShape delegateShape(BlockGetter level, BlockPos pos, CollisionContext context,
                                            boolean collision) {
        if (!(level.getBlockEntity(pos) instanceof CsBossMimicBlockEntity be)) {
            // Empty block getter: the state cache and isPathfindable both land here.
            return FULL;
        }
        BlockState resolved = be.getResolvedMimicState();
        if (resolved == null || resolved.isAir() || resolved.hasBlockEntity()) {
            return FULL;
        }
        return collision
                ? resolved.getCollisionShape(level, pos, context)
                : resolved.getShape(level, pos, context);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (isSolid(state)) {
            VoxelShape shape = delegateShape(level, pos, context, false);
            // A target with an empty model must still be selectable, or it is an unbreakable trap.
            return shape.isEmpty() ? FULL : shape;
        }
        // Invisible: keep the selection outline ONLY for a creative player, and keep it a full cube -
        // nothing is drawn, so a carved outline would only make the block harder to grab.
        if (context instanceof EntityCollisionContext ec
                && ec.getEntity() instanceof Player player && player.isCreative()) {
            return FULL;
        }
        return Shapes.empty();
    }

    /** Followed verbatim, empty included: a mimic of a hollow block must be walked through like one. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isSolid(state) ? delegateShape(level, pos, context, true) : Shapes.empty();
    }

    /*
     * The block is dynamicShape (see ModBlocks) so that support and collision follow the block
     * entity, which the per-state cache could never see. Light must not: these two reproduce exactly
     * what that cache used to hold for a noOcclusion block, and keep the lighting engine off the
     * block entity entirely.
     */

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !isSolid(state);
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return isSolid(state) ? 1 : 0;
    }

    // ------------------------------------------------------------------ configuration

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && sp.isCreative()
                && stack.getItem() instanceof BlockItem blockItem
                && level.getBlockEntity(pos) instanceof CsBossMimicBlockEntity be) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            be.setMimicBlockId(id.toString());
            // Forced: our apparent identity changed, so neighbours must re-read us even if the
            // resolved state happens to land on the same value.
            refresh(level, pos, state, true);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)
                || !(level.getBlockEntity(pos) instanceof CsBossMimicBlockEntity be)) {
            return InteractionResult.PASS;
        }
        // Creative + empty hand + no shift: config GUI.
        if (sp.isCreative() && !sp.isShiftKeyDown()) {
            Services.PLATFORM.sendPayloadToPlayer(sp, be.createOpenPayload());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
