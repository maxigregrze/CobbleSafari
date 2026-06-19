package maxigregrze.cobblesafari.block.trap;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MoveTrapBlock extends TrapBlock {

    public static final MapCodec<MoveTrapBlock> CODEC = simpleCodec(props -> new MoveTrapBlock(props, false));

    /** Horizontal launch speed per block of intended travel. */
    private static final double SPEED_PER_BLOCK = 0.45;
    private static final double VERTICAL_HOP = 0.18;

    public MoveTrapBlock(Properties properties, boolean hard) {
        super(properties, hard);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onTriggered(ServerLevel level, BlockPos pos, BlockState state, @Nullable LivingEntity activator) {
        if (activator == null) {
            return;
        }
        Direction dir = state.getValue(FACING);
        int blocks = hard ? 3 : 1;
        double speed = blocks * SPEED_PER_BLOCK;
        // Force the entity to physically move along the block's facing (a shove, not a teleport).
        Vec3 velocity = new Vec3(dir.getStepX() * speed, VERTICAL_HOP, dir.getStepZ() * speed);
        activator.setDeltaMovement(velocity);
        // Required so the velocity is sent to (player) clients, otherwise it is ignored client-side.
        activator.hurtMarked = true;
    }
}
