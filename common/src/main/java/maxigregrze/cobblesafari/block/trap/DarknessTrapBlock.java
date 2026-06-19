package maxigregrze.cobblesafari.block.trap;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DarknessTrapBlock extends TrapBlock {

    public static final MapCodec<DarknessTrapBlock> CODEC = simpleCodec(props -> new DarknessTrapBlock(props, false));

    public DarknessTrapBlock(Properties properties, boolean hard) {
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
        activator.addEffect(new MobEffectInstance(
                hard ? MobEffects.BLINDNESS : MobEffects.DARKNESS,
                hard ? 400 : 200, 0, false, false));
    }
}
