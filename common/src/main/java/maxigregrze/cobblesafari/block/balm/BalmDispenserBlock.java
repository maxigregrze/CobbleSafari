package maxigregrze.cobblesafari.block.balm;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.csboss.BattleReactiveBlock;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BalmDispenserBlock extends BaseEntityBlock implements BattleReactiveBlock {

    public static final MapCodec<BalmDispenserBlock> CODEC = simpleCodec(BalmDispenserBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 8);

    public static final int CHARGE_READY = 0;
    public static final int CHARGE_MAX = 8;

    /** Aligné sur le modèle Blockbench (corps + pieds, y max = 10). */
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);

    public BalmDispenserBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(ACTIVE, false)
                .setValue(CHARGE, CHARGE_READY));
    }

    /** Item distribué au clic droit quand le distributeur est chargé. */
    public Item getDispensedItem() {
        return ModItems.BALM;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, CHARGE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BalmDispenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() && type == ModBlockEntities.BALM_DISPENSER) {
            return (lvl, pos, st, be) -> ((BalmDispenserBlockEntity) be).serverTick(lvl, pos, st);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!state.getValue(ACTIVE) || state.getValue(CHARGE) != CHARGE_READY) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        ItemStack grant = new ItemStack(getDispensedItem(), 16);
        if (!sp.getInventory().add(grant)) {
            sp.drop(grant, false);
        }
        level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0f, 1.0f);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BalmDispenserBlockEntity dispenser) {
            dispenser.beginRecharge(BalmDispenserSettings.getRechargeSeconds());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setBattleState(ServerLevel level, BlockPos pos, BlockState state, boolean battle) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BalmDispenserBlockEntity dispenser)) {
            return;
        }

        dispenser.resetRecharge();
        if (battle) {
            level.setBlock(pos, state.setValue(ACTIVE, true).setValue(CHARGE, CHARGE_READY), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
        }
        dispenser.setChanged();
    }

    @Override
    public boolean isReactive(BlockState state) {
        return true;
    }
}
