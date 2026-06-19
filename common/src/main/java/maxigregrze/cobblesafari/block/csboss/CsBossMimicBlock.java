package maxigregrze.cobblesafari.block.csboss;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * CSBoss-reactive "mimic" block. Toggles between invisible/no-hitbox and a full
 * solid block whose texture is copied from a configurable target block (rendered by the BER).
 *
 * <p>{@code active} is posted by the battle scan ({@link BattleReactiveBlock}); {@code reverse} is a
 * creative GUI option. The block is "solid" (visible + full hitbox) when {@code active == reverse}:
 * default (reverse=false) ⇒ solid when idle, invisible during a fight; reverse=true flips it.
 * In creative the selection box is always shown (even while invisible); in survival there is no
 * hitbox when invisible.
 */
public class CsBossMimicBlock extends BaseEntityBlock implements BattleReactiveBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty REVERSE = BooleanProperty.create("reverse");
    public static final MapCodec<CsBossMimicBlock> CODEC = simpleCodec(CsBossMimicBlock::new);

    private static final VoxelShape FULL = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public CsBossMimicBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ACTIVE, false)
                .setValue(REVERSE, false));
    }

    /** Visible + full hitbox when active and reverse agree. */
    private static boolean isSolid(BlockState state) {
        return state.getValue(ACTIVE) == state.getValue(REVERSE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, REVERSE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CsBossMimicBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Always rendered by the BER (copies the target block's model); nothing from the blockstate.
        return RenderShape.INVISIBLE;
    }

    @Override
    public void setBattleState(ServerLevel level, BlockPos pos, BlockState state, boolean battle) {
        if (state.getValue(ACTIVE) != battle) {
            level.setBlock(pos, state.setValue(ACTIVE, battle), Block.UPDATE_ALL);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (isSolid(state)) {
            return FULL;
        }
        // Invisible: keep the selection outline ONLY for a creative player.
        if (context instanceof EntityCollisionContext ec
                && ec.getEntity() instanceof Player player && player.isCreative()) {
            return FULL;
        }
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isSolid(state) ? FULL : Shapes.empty();
    }

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
            be.syncToClients();
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
        // Creative + empty hand + no shift ⇒ config GUI.
        if (sp.isCreative() && !sp.isShiftKeyDown()) {
            Services.PLATFORM.sendPayloadToPlayer(sp, be.createOpenPayload());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
