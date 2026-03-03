package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.config.IncubatorConfig;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.phys.shapes.VoxelShape;

public class SafariEggNestBlock extends BaseEntityBlock {

    public static final MapCodec<SafariEggNestBlock> CODEC = simpleCodec(SafariEggNestBlock::new);
    public static final BooleanProperty HAS_EGG = BooleanProperty.create("has_egg");

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape EMPTY_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public SafariEggNestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_EGG, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_EGG);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(HAS_EGG)) {
            return EMPTY_SHAPE;
        }
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SafariEggNestBlockEntity(pos, state);
    }

    private InteractionResult handleInteraction(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (state.getValue(HAS_EGG)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SafariEggNestBlockEntity nestEntity) {
                String biomeType = nestEntity.getBiomeType();
                Item eggItem = ModItems.getWildEgg(biomeType);
                ItemStack eggStack = new ItemStack(eggItem);

                Block.popResource(level, pos, eggStack);

                level.setBlock(pos, state.setValue(HAS_EGG, false), 3);

                long currentTick = level.getGameTime();
                int refillDelay = level.random.nextIntBetweenInclusive(
                        IncubatorConfig.getEggNestMinRefillTicks(),
                        IncubatorConfig.getEggNestMaxRefillTicks());
                nestEntity.setNextRefillTick(currentTick + refillDelay);

                level.scheduleTick(pos, this, refillDelay);

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return handleInteraction(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = handleInteraction(state, level, pos, player);
        if (result.consumesAction()) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(HAS_EGG)) {
            level.setBlock(pos, state.setValue(HAS_EGG, true), 3);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SafariEggNestBlockEntity nestEntity) {
            String currentBiomeType = nestEntity.getBiomeType();
            if (currentBiomeType == null || currentBiomeType.isEmpty()) {
                ResourceLocation biomeLocation = level.getBiome(pos).unwrapKey()
                        .map(key -> key.location())
                        .orElse(null);

                if (biomeLocation != null && biomeLocation.getNamespace().equals("cobblesafari")) {
                    String biomeName = biomeLocation.getPath();
                    nestEntity.setBiomeType(biomeName);
                }
            }
        }
    }
}
