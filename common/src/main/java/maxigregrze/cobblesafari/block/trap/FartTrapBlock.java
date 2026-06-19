package maxigregrze.cobblesafari.block.trap;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class FartTrapBlock extends TrapBlock {

    public static final MapCodec<FartTrapBlock> CODEC = simpleCodec(props -> new FartTrapBlock(props, false));

    public FartTrapBlock(Properties properties, boolean hard) {
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
        // Silent explosion (no boom) so the custom fart sound can be heard.
        TrapEffects.explode(level, pos, hard, Holder.direct(ModSounds.SILENT));
        Vec3 center = Vec3.atCenterOf(pos);
        level.playSound(null, center.x, center.y, center.z,
                hard ? ModSounds.FART_REVERB : ModSounds.FART, SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}
