package maxigregrze.cobblesafari.block.teleporter;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.teleporter.TeleporterTickHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SafariTeleporterBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<SafariTeleporterBlock> CODEC = simpleCodec(SafariTeleporterBlock::new);
    private static final VoxelShape SHAPE = Block.box(-1, 0, -1, 17, 8, 17);
    private static final int OVERWORLD_MESSAGE_COOLDOWN_TICKS = 100;
    private static final Map<UUID, Long> overworldMessageCooldown = new ConcurrentHashMap<>();

    public SafariTeleporterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            if (level.dimension() != Level.OVERWORLD) {
                UUID playerId = player.getUUID();
                long currentTick = level.getGameTime();
                Long lastMessage = overworldMessageCooldown.get(playerId);
                if (lastMessage == null || currentTick - lastMessage > OVERWORLD_MESSAGE_COOLDOWN_TICKS) {
                    player.sendSystemMessage(Component.translatable("cobblesafari.teleporter.overworld_only"));
                    overworldMessageCooldown.put(playerId, currentTick);
                }
                return;
            }
            TeleporterTickHandler.updatePlayerOnTeleporter(player);
        }
    }
}
