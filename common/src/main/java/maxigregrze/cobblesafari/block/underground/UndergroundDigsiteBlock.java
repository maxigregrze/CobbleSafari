package maxigregrze.cobblesafari.block.underground;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class UndergroundDigsiteBlock extends Block {

    public static final MapCodec<UndergroundDigsiteBlock> CODEC = simpleCodec(UndergroundDigsiteBlock::new);

    public UndergroundDigsiteBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            level.setBlock(pos, ModBlocks.UNDERGROUND_STONE_TRANSITION.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundType.DEEPSLATE.getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            UndergroundMinigame.startSession(serverPlayer);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double px = pos.getX();
        double py = pos.getY();
        double pz = pos.getZ();

        for (int i = 0; i < 5; i++) {
            int face = random.nextInt(6);
            double x;
            double y;
            double z;
            double vx = (random.nextDouble() - 0.5) * 0.02;
            double vy = random.nextDouble() * 0.05;
            double vz = (random.nextDouble() - 0.5) * 0.02;

            switch (face) {
                case 0 -> {
                    x = px + random.nextDouble();
                    y = py + 1.0;
                    z = pz + random.nextDouble();
                    vy = 0.02 + random.nextDouble() * 0.03;
                }
                case 1 -> {
                    x = px + random.nextDouble();
                    y = py;
                    z = pz + random.nextDouble();
                    vy = -(0.02 + random.nextDouble() * 0.03);
                }
                case 2 -> {
                    x = px + random.nextDouble();
                    y = py + random.nextDouble();
                    z = pz;
                    vz = -(0.02 + random.nextDouble() * 0.03);
                }
                case 3 -> {
                    x = px + random.nextDouble();
                    y = py + random.nextDouble();
                    z = pz + 1.0;
                    vz = 0.02 + random.nextDouble() * 0.03;
                }
                case 4 -> {
                    x = px + 1.0;
                    y = py + random.nextDouble();
                    z = pz + random.nextDouble();
                    vx = 0.02 + random.nextDouble() * 0.03;
                }
                default -> {
                    x = px;
                    y = py + random.nextDouble();
                    z = pz + random.nextDouble();
                    vx = -(0.02 + random.nextDouble() * 0.03);
                }
            }

            level.addParticle(ParticleTypes.END_ROD, x, y, z, vx, vy, vz);
        }

        for (int i = 0; i < 5; i++) {
            int face = random.nextInt(6);
            double x;
            double y;
            double z;
            switch (face) {
                case 0 -> {
                    x = px + random.nextDouble();
                    y = py + 1.0;
                    z = pz + random.nextDouble();
                }
                case 1 -> {
                    x = px + random.nextDouble();
                    y = py;
                    z = pz + random.nextDouble();
                }
                case 2 -> {
                    x = px + random.nextDouble();
                    y = py + random.nextDouble();
                    z = pz;
                }
                case 3 -> {
                    x = px + random.nextDouble();
                    y = py + random.nextDouble();
                    z = pz + 1.0;
                }
                case 4 -> {
                    x = px + 1.0;
                    y = py + random.nextDouble();
                    z = pz + random.nextDouble();
                }
                default -> {
                    x = px;
                    y = py + random.nextDouble();
                    z = pz + random.nextDouble();
                }
            }
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
