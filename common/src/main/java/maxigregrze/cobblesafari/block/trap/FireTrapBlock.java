package maxigregrze.cobblesafari.block.trap;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FireTrapBlock extends TrapBlock {

    public static final MapCodec<FireTrapBlock> CODEC = simpleCodec(props -> new FireTrapBlock(props, false));

    public FireTrapBlock(Properties properties, boolean hard) {
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
        activator.setRemainingFireTicks(hard ? 100 : 40);
    }
}
