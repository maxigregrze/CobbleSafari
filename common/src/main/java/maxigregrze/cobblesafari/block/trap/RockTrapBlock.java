package maxigregrze.cobblesafari.block.trap;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.entity.safari.SafariBallisticMeteorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RockTrapBlock extends TrapBlock {

    public static final MapCodec<RockTrapBlock> CODEC = simpleCodec(props -> new RockTrapBlock(props, false));

    public RockTrapBlock(Properties properties, boolean hard) {
        super(properties, hard);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    private static final double FALL_HEIGHT = 20.0;
    private static final int COOLDOWN_TICKS = 60; // 3 seconds

    @Override
    protected int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    protected void onTriggered(ServerLevel level, BlockPos pos, BlockState state, @Nullable LivingEntity activator) {
        if (!hard) {
            // Base: throw a rock straight up immediately (no telegraph delay), like the crater but instant.
            SafariBallisticMeteorEntity.spawnInstant(level, craterSpawn(pos), level.getRandom());
            return;
        }
        if (activator == null) {
            return;
        }
        // Hard: drop a meteorite from the sky onto the activator (damage only, no block placed).
        Vec3 above = activator.position().add(0, FALL_HEIGHT, 0);
        SafariBallisticMeteorEntity.spawnFalling(level, above, activator, level.getRandom());
    }

    private static Vec3 craterSpawn(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5);
    }
}
