package maxigregrze.cobblesafari.block.csboss;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tuile d'attaque CSBoss (plan 199 § 10.1) : posée au-dessus du sol, cycle auto-géré comme
 * {@link CsBossElectricityBlock} — alerte ({@code ACTIVE=false}) puis actif ({@code ACTIVE=true})
 * puis retrait. Le bloc ne fait <b>que</b> le visuel et son cycle : dégâts et effets sont appliqués
 * par l'attaque ({@code CsBossTileField}), qui seule connaît le multiplicateur du boss. Au retrait, la
 * décoration éventuellement remplacée revient ({@link CsBossTileBlockEntity}).
 */
public abstract class CsBossTileBlock extends Block implements EntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    private final int warnTicks;
    private final int activeTicks;

    protected CsBossTileBlock(Properties properties, int warnTicks, int activeTicks) {
        super(properties);
        this.warnTicks = warnTicks;
        this.activeTicks = activeTicks;
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    public int warnTicks() {
        return warnTicks;
    }

    public int activeTicks() {
        return activeTicks;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CsBossTileBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.getValue(ACTIVE)) {
            level.scheduleTick(pos, this, warnTicks);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, activeTicks);
        } else {
            CsBossTileBlockEntity.restore(level, pos);
        }
    }
}
