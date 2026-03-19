package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DistortionPortalBlock extends BaseEntityBlock {
    public static final MapCodec<DistortionPortalBlock> CODEC = simpleCodec(DistortionPortalBlock::new);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public DistortionPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, Mode.TOP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(MODE, Mode.TOP);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        if (!player.isCreative()) {
            return InteractionResult.PASS;
        }

        Mode nextMode = state.getValue(MODE) == Mode.TOP ? Mode.BOTTOM : Mode.TOP;
        level.setBlock(pos, state.setValue(MODE, nextMode), Block.UPDATE_ALL);
        player.sendSystemMessage(Component.translatable("message.cobblesafari.distortion_portal.mode." + nextMode.getSerializedName()));
        return InteractionResult.CONSUME;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            int targetY = pos.getY() + (state.getValue(MODE) == Mode.TOP ? 128 : -128);
            int clampedY = Math.clamp(targetY, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
            serverPlayer.teleportTo(
                    (net.minecraft.server.level.ServerLevel) level,
                    pos.getX() + 0.5,
                    clampedY + 0.1,
                    pos.getZ() + 0.5,
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );
            serverPlayer.resetFallDistance();
        }
        super.entityInside(state, level, pos, entity);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DistortionPortalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, ModBlockEntities.DISTORTION_PORTAL, DistortionPortalBlockEntity::clientTick);
        }
        return null;
    }

    public enum Mode implements StringRepresentable {
        TOP("top"),
        BOTTOM("bottom");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
