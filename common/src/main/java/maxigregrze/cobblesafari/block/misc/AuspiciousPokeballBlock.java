package maxigregrze.cobblesafari.block.misc;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.network.AuspiciousPokeballConfigServerHandler;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

public class AuspiciousPokeballBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int[] CATEGORY_WEIGHTS = {1, 1, 1, 1};

    public static final MapCodec<AuspiciousPokeballBlock> CODEC = simpleCodec(AuspiciousPokeballBlock::new);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public AuspiciousPokeballBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof AuspiciousPokeballBlockEntity be)) {
            return SHAPE;
        }
        if (shouldHideInteractionShape(context, be)) {
            return Shapes.empty();
        }
        return SHAPE;
    }

    private static boolean shouldHideInteractionShape(CollisionContext context, AuspiciousPokeballBlockEntity be) {
        if (!(context instanceof EntityCollisionContext entityContext)) {
            return false;
        }
        if (!(entityContext.getEntity() instanceof Player player)) {
            return false;
        }
        return be.shouldHideShapeForSurvivalPlayer(player);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("[AuspiciousPokeball] Player is not ServerPlayer!");
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof AuspiciousPokeballBlockEntity blockEntity)) {
            LOGGER.warn("[AuspiciousPokeball] No BlockEntity found at position!");
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            LOGGER.warn("[AuspiciousPokeball] Level is not ServerLevel!");
            return InteractionResult.PASS;
        }

        if (player.isCreative() && player.isShiftKeyDown()) {
            Services.PLATFORM.sendPayloadToPlayer(serverPlayer, blockEntity.createOpenPayload());
            return InteractionResult.CONSUME;
        }

        UUID playerId = serverPlayer.getUUID();
        if (blockEntity.hasClaimed(playerId)) {
            return InteractionResult.CONSUME;
        }
        if (!blockEntity.canPlayerAttemptClaim(serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        blockEntity.tryClaim(playerId);
        givePoolLoot(serverLevel, pos, serverPlayer, blockEntity);
        return InteractionResult.CONSUME;
    }

    protected static void givePoolLoot(ServerLevel level, BlockPos pos, ServerPlayer player, AuspiciousPokeballBlockEntity be) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);

        be.normalizeRollBounds();
        int lo = be.getMinRoll();
        int hi = be.getMaxRoll();
        int count = lo;
        if (hi > lo) {
            count = lo + level.random.nextInt(hi - lo + 1);
        }

        for (int i = 0; i < count; i++) {
            int cat = pickWeightedCategory(level.random);
            String poolId = be.getPoolIdForCategory(cat).trim();
            if (poolId.isEmpty()) {
                AuspiciousPokeballConfigServerHandler.logPoolRollFailure("empty pool id for category " + cat, "(empty)");
                continue;
            }
            ResourceLocation poolLoc = ResourceLocation.tryParse(poolId);
            if (poolLoc == null) {
                AuspiciousPokeballConfigServerHandler.logPoolRollFailure("invalid resource location syntax", poolId);
                continue;
            }
            ResourceKey<LootTable> poolKey = ResourceKey.create(Registries.LOOT_TABLE, poolLoc);
            if (!lootTableExists(level, poolKey)) {
                AuspiciousPokeballConfigServerHandler.logPoolRollFailure("loot table not found", poolId);
                continue;
            }
            LootTable table = level.getServer().reloadableRegistries().getLootTable(poolKey);
            for (ItemStack stack : table.getRandomItems(params)) {
                if (!stack.isEmpty()) {
                    giveStack(level, pos, player, stack);
                }
            }
        }
    }

    private static int pickWeightedCategory(RandomSource rng) {
        int total = 0;
        for (int w : CATEGORY_WEIGHTS) {
            total += w;
        }
        int roll = rng.nextInt(total);
        int sum = 0;
        for (int i = 0; i < CATEGORY_WEIGHTS.length; i++) {
            sum += CATEGORY_WEIGHTS[i];
            if (roll < sum) {
                return i;
            }
        }
        return CATEGORY_WEIGHTS.length - 1;
    }

    private static boolean lootTableExists(ServerLevel level, ResourceKey<LootTable> key) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        return table != LootTable.EMPTY;
    }

    private static void giveStack(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack stack) {
        boolean added = player.getInventory().add(stack);
        if (!added) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, stack);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AuspiciousPokeballBlockEntity(pos, state);
    }
}
