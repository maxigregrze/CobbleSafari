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
 * Bloc météorite éphémère (plan 107 § 5.1) posé par les attaques roche/dragon. Il programme sa
 * propre destruction {@code ttlTicks} ticks après sa pose ({@code onPlace} → {@code scheduleTick}),
 * indépendamment de l'attaque qui l'a créé. À l'expiration : particules {@code block_destruct} +
 * son de cassage via {@code levelEvent(2001)}.
 */
public class MeteoriteBlock extends Block {

    /** Hauteur 1,15 bloc (clôture ≈ 1,5) : dépasse le cube visuel pour bloquer le saut par-dessus. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.5 * 16.0, 16.0);

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
        level.levelEvent(2001, pos, Block.getId(state)); // particules + son de cassage
        level.removeBlock(pos, false);
    }
}
