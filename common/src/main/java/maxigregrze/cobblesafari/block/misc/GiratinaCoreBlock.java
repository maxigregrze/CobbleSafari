package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GiratinaCoreBlock extends BaseEntityBlock {

    public static final MapCodec<GiratinaCoreBlock> CODEC = simpleCodec(GiratinaCoreBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    private static boolean removingMultiblock = false;

    private static final int[][] SIDE_OFFSETS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0},            {1, 0},
            {-1, 1},  {0, 1},  {1, 1}
    };
    private static final ResourceLocation GIRATINA_CORE_TRADE_LOOT_TABLE = ResourceLocation.fromNamespaceAndPath("cobblesafari", "blocks/giratina_core_trade");

    public GiratinaCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos centerPos = context.getClickedPos();
        Level level = context.getLevel();
        for (int[] offset : SIDE_OFFSETS) {
            BlockPos sidePos = centerPos.offset(offset[0], 0, offset[1]);
            if (!level.getBlockState(sidePos).canBeReplaced(context)) {
                return null;
            }
        }
        return this.defaultBlockState();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GiratinaCoreBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        level.setBlock(pos.offset(0, 0, -1), ModBlocks.GIRATINA_CORE_N.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(1, 0, -1), ModBlocks.GIRATINA_CORE_NE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(1, 0, 0), ModBlocks.GIRATINA_CORE_E.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(1, 0, 1), ModBlocks.GIRATINA_CORE_SE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(0, 0, 1), ModBlocks.GIRATINA_CORE_S.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(-1, 0, 1), ModBlocks.GIRATINA_CORE_SW.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(-1, 0, 0), ModBlocks.GIRATINA_CORE_W.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(-1, 0, -1), ModBlocks.GIRATINA_CORE_NW.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.isCreative()) {
            return 0.0f;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            breakMultiblock(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && !removingMultiblock) {
            breakMultiblock(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static boolean isRemovingMultiblock() {
        return removingMultiblock;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return tryUseRedchainFragmentOnCore(stack, level, pos, player, hand);
    }

    public static ItemInteractionResult tryUseRedchainFragmentOnCore(ItemStack stack, Level level, BlockPos corePos, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!stack.is(ModItems.REDCHAIN_FRAGMENT)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return ItemInteractionResult.FAIL;
        }

        if (!(level.getBlockEntity(corePos) instanceof GiratinaCoreBlockEntity coreBlockEntity)) {
            return ItemInteractionResult.FAIL;
        }
        if (!coreBlockEntity.canTrade(level)) {
            return ItemInteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        coreBlockEntity.markTrade(level);

        serverLevel.playSound(null, corePos, ModSounds.GIRATINA_TRADE, SoundSource.BLOCKS, 1.0f, 1.0f);
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, corePos.getX() + 0.5, corePos.getY() + 1.1, corePos.getZ() + 0.5, 20, 0.2, 0.2, 0.2, 0.02);

        LootTable lootTable = serverLevel.getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, GIRATINA_CORE_TRADE_LOOT_TABLE));
        LootParams lootParams = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, corePos.getCenter())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.GIFT);
        for (ItemStack reward : lootTable.getRandomItems(lootParams)) {
            Block.popResource(serverLevel, corePos.above(), reward);
        }

        return ItemInteractionResult.CONSUME;
    }

    public static void breakMultiblock(Level level, BlockPos centerPos) {
        if (removingMultiblock) {
            return;
        }
        removingMultiblock = true;
        removeIfGiratinaPart(level, centerPos);
        for (int[] offset : SIDE_OFFSETS) {
            removeIfGiratinaPart(level, centerPos.offset(offset[0], 0, offset[1]));
        }
        removingMultiblock = false;
    }

    private static void removeIfGiratinaPart(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (current.is(ModBlocks.GIRATINA_CORE)
                || current.is(ModBlocks.GIRATINA_CORE_N)
                || current.is(ModBlocks.GIRATINA_CORE_NE)
                || current.is(ModBlocks.GIRATINA_CORE_E)
                || current.is(ModBlocks.GIRATINA_CORE_SE)
                || current.is(ModBlocks.GIRATINA_CORE_S)
                || current.is(ModBlocks.GIRATINA_CORE_SW)
                || current.is(ModBlocks.GIRATINA_CORE_W)
                || current.is(ModBlocks.GIRATINA_CORE_NW)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
        }
    }

    @Override
    public String getDescriptionId() {
        return "block.cobblesafari.giratina_core";
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, ModBlockEntities.GIRATINA_CORE, GiratinaCoreBlockEntity::clientTick);
        }
        return null;
    }
}
