package maxigregrze.cobblesafari.block.csboss;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ephemeral meteorite block (plan 107 § 5.1) placed by rock/dragon attacks. Schedules its own
 * destruction {@code ttlTicks} ticks after placement ({@code onPlace} → {@code scheduleTick}),
 * independent of the attack that created it. On expiry: {@code block_destruct} particles +
 * break sound via {@code levelEvent(2001)}.
 */
public class MeteoriteBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.1 * 16.0, 16.0);

    private final int ttlTicks;

    public MeteoriteBlock(Properties properties, int ttlTicks) {
        super(properties);
        this.ttlTicks = ttlTicks;
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, ttlTicks);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.levelEvent(2001, pos, Block.getId(state)); // particles + break sound
        level.removeBlock(pos, false);
    }
}
