package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.GroundedModelBlock;
import maxigregrze.cobblesafari.block.base.Waterlogged;
import maxigregrze.cobblesafari.manager.BannedItemsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Strange Bulb: a {@link GroundedModelBlock} whose model an axe toggles between the two drawn
 * shapes. {@link #ALT} is deliberately absent from the item - a bulb broken and replaced comes
 * back closed - so there is no {@code getCloneItemStack} override and no data component.
 */
public class UltraspaceBulbBlock extends GroundedModelBlock {

    public static final BooleanProperty ALT = BooleanProperty.create("alt");

    private final VoxelShape shape;

    public UltraspaceBulbBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, Support.NONE);
        this.shape = shape;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(Waterlogged.WATERLOGGED, Boolean.FALSE)
                .setValue(ALT, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new UltraspaceBulbBlock(props, this.shape));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ALT);
    }

    /**
     * Cycles rather than sets: the brief only asks for the outward trip, but the return one is free
     * and saves breaking the block to change one's mind. Guarded like every other build action in
     * the mod (see {@code HyperspaceBarrierBlock}).
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof AxeItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player == null || !player.mayBuild()) {
            return ItemInteractionResult.FAIL;
        }
        if (!player.isCreative() && !BannedItemsManager.isBlockBreakingAllowed(level.dimension())) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("cobblesafari.ban.block_breaking_banned"));
            }
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(ALT), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
