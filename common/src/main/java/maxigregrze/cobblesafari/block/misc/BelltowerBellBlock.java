package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import maxigregrze.cobblesafari.block.base.Waterlogged;
import maxigregrze.cobblesafari.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Belltower bell: hangs under a supporting block the way a hanging lantern does, and swings with a
 * damped oscillation when a player right-clicks or hits one of its sides.
 *
 * <p>Split in two like the punching bag: the yoke and mast are a static model baked into the chunk,
 * the bell itself is drawn by a {@code BlockEntityRenderer} so it can move. The trigger channel is
 * {@code blockEvent} to {@code triggerEvent}, exactly as vanilla's bell does it.</p>
 */
public class BelltowerBellBlock extends HorizontalModelBlock implements EntityBlock, SimpleWaterloggedBlock {

    public static final int EVENT_RING = 1;

    /**
     * Bell body plus the stub of mast that pokes out above it. Symmetric under a quarter turn, so
     * {@code Settings.rotateShape()} is deliberately not enabled - the yoke, which is not
     * symmetric, sits entirely inside the body and adds nothing to the union.
     */
    public static final VoxelShape SHAPE = Shapes.or(
            Block.box(4.5, 7, 4.5, 11.5, 14, 11.5),
            Block.box(7, 14, 7, 9, 16, 9));

    private final Settings settings;

    public BelltowerBellBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.settings = settings;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(Waterlogged.WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new BelltowerBellBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(Waterlogged.WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The parent already applies the canSurvive gate, which for Support.CEILING refuses the
        // placement when there is no supporting block above.
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(Waterlogged.WATERLOGGED, Waterlogged.atPlacement(context));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Waterlogged.fluidState(state, super.getFluidState(state));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Waterlogged.scheduleTick(state, level, pos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return ring(level, pos, hitResult.getDirection())
                ? InteractionResult.sidedSuccess(level.isClientSide())
                : InteractionResult.PASS;
    }

    /**
     * Left-click rings the bell <em>and</em> lets mining proceed, like a vanilla note block. No
     * BlockHitResult is available here, so the struck face is reconstructed from where the player
     * stands relative to the block centre - the same approximation {@code PunchingBagBlock} uses.
     */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        ring(level, pos, nearestHorizontalFace(player, pos));
    }

    /** Only a side rings: hitting the top or the bottom does nothing. */
    private boolean ring(Level level, BlockPos pos, Direction face) {
        if (!face.getAxis().isHorizontal()) {
            return false;
        }
        if (!level.isClientSide()) {
            level.blockEvent(pos, this, EVENT_RING, face.get3DDataValue());
            level.playSound(null, pos, ModSounds.BELL_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    private static Direction nearestHorizontalFace(Player player, BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx < 0 ? Direction.WEST : Direction.EAST;
        }
        return dz < 0 ? Direction.NORTH : Direction.SOUTH;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(id, param);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BelltowerBellBlockEntity(pos, state);
    }
}
