package maxigregrze.cobblesafari.block.trap;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ExplosionTrapBlock extends TrapBlock {

    public static final MapCodec<ExplosionTrapBlock> CODEC = simpleCodec(props -> new ExplosionTrapBlock(props, false));

    public ExplosionTrapBlock(Properties properties, boolean hard) {
        super(properties, hard);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean projectileActivable() {
        return true;
    }

    @Override
    protected void onTriggered(ServerLevel level, BlockPos pos, BlockState state, @Nullable LivingEntity activator) {
        TrapEffects.explode(level, pos, hard);
    }
}
