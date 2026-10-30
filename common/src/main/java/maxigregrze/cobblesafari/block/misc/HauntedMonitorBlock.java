package maxigregrze.cobblesafari.block.misc;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonBlocks;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.block.MonitorBlock;
import com.cobblemon.mod.common.block.entity.FossilMultiblockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Cobblemon data monitor that lightning struck while a lit candle stood on it (action plan 198,
 * part I): Rotom has moved in. Using it lets Rotom out the way an Upgrade lets Porygon2 out - smoke,
 * the monitor's break sound, a damaged monitor left behind - and a wild Rotom appears in front.
 *
 * <p>Same model and hitbox as Cobblemon's damaged monitor, only the screen differs. The strike
 * itself is {@link #tryHaunt}, run from the data monitor's random tick.</p>
 */
public class HauntedMonitorBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<HauntedMonitorBlock> CODEC = simpleCodec(HauntedMonitorBlock::new);

    /** The night window the strike can happen in, in day ticks (0 = 6 AM, 18000 = midnight). */
    private static final long NIGHT_START = 14000L;
    private static final long NIGHT_END = 21983L;
    private static final long DAY_LENGTH = 24000L;

    /** The level Cobblemon gives the Porygon of a monitor. */
    private static final int ROTOM_LEVEL = 20;
    private static final int SMOKE_COUNT = 10;

    public HauntedMonitorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Only ever placed by a strike, but placed like Cobblemon's monitors if it ever is. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return damagedMonitor(state).getShape(level, pos, context);
    }

    /** It drops a data monitor (decision 14.3), so picking it gives one too. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(CobblemonItems.MONITOR);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            release(state, serverLevel, pos);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                maxigregrze.cobblesafari.advancement.ModCriteria.HAUNTED_MONITOR_FREED.trigger(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Cobblemon's Upgrade ending, with Rotom instead of Porygon (decision 14.4). */
    private static void release(BlockState state, ServerLevel level, BlockPos pos) {
        Direction screen = state.getValue(FACING).getOpposite();
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5D + screen.getStepX() * 0.6D, pos.getY() + 0.7D, pos.getZ() + 0.5D + screen.getStepZ() * 0.6D,
                SMOKE_COUNT, 0.2D, 0.2D, 0.2D, 0.05D);
        level.playSound(null, pos, CobblemonSounds.MONITOR_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlockAndUpdate(pos, damagedMonitor(state));
        spawnRotom(level, pos, screen);
    }

    private static void spawnRotom(ServerLevel level, BlockPos pos, Direction screen) {
        RandomSource random = level.getRandom();
        boolean alpha = random.nextInt(Math.max(1, Cobblemon.INSTANCE.getConfig().getMonitorAlphaRate())) == 0;
        boolean shiny = random.nextInt(Math.max(1, Cobblemon.INSTANCE.getConfig().getMonitorShinyRate())) == 0;
        PokemonEntity rotom = PokemonProperties.Companion
                .parse("rotom lvl=" + ROTOM_LEVEL + " alpha=" + alpha + " shiny=" + shiny)
                .createEntity(level);
        Vec3 spot = findSpawnSpot(level, rotom, pos, screen);
        rotom.moveTo(spot.x, spot.y, spot.z, screen.toYRot(), 0.0F);
        level.addFreshEntity(rotom);
    }

    /**
     * A reduced copy of Cobblemon's {@code FossilMultiblockEntity#findSafeSpawnPos}: one then two
     * blocks in front of the screen, at the same height or one above or below, the first place
     * Rotom fits; right in front of the screen if none does.
     */
    private static Vec3 findSpawnSpot(ServerLevel level, PokemonEntity rotom, BlockPos pos, Direction screen) {
        for (int distance = 1; distance <= 2; distance++) {
            BlockPos ahead = pos.relative(screen, distance);
            for (int dy : new int[] {0, 1, -1}) {
                Vec3 spot = Vec3.atBottomCenterOf(ahead.above(dy));
                if (!level.collidesWithSuffocatingBlock(rotom, rotom.getDimensions(Pose.STANDING).makeBoundingBox(spot))) {
                    return spot;
                }
            }
        }
        return Vec3.atBottomCenterOf(pos.relative(screen));
    }

    /**
     * The data monitor's random tick (added by {@code MonitorBlockHauntMixin}). With a lit candle on
     * it, at night, a standalone monitor that is not busy with a Porygon may be struck and haunted
     * (decisions 14.1 and 14.2).
     */
    public static void tryHaunt(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!hasLitCandleOnTop(level, pos) || !isNight(level)) {
            return;
        }
        // A fossil machine's monitor belongs to the machine, as for Cobblemon's Porygon.
        if (!(level.getBlockEntity(pos) instanceof FossilMultiblockEntity monitor) || monitor.getMultiblockStructure() != null) {
            return;
        }
        MonitorBlock.MonitorScreen screen = state.getValue(MonitorBlock.Companion.getSCREEN());
        if (screen == MonitorBlock.MonitorScreen.PORYGON_GRID || screen == MonitorBlock.MonitorScreen.PORYGON_GLITCHING) {
            return;
        }
        if (random.nextDouble() >= MiscConfig.getHauntedMonitorStrikeChance()) {
            return;
        }

        // Visual only: no fire, no damage, but the flash and the thunder - even underground.
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.moveTo(Vec3.atBottomCenterOf(pos.above()));
            level.addFreshEntity(bolt);
        }
        // Any disc inside pops out through Cobblemon's own onRemove.
        level.setBlockAndUpdate(pos, ModBlocks.HAUNTED_MONITOR.defaultBlockState()
                .setValue(FACING, state.getValue(FACING)));
    }

    private static boolean hasLitCandleOnTop(Level level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(BlockTags.CANDLES) && above.hasProperty(CandleBlock.LIT) && above.getValue(CandleBlock.LIT);
    }

    /** Time of day as {@code CsMusicEvalContext} reads it, so fixed-time dimensions count too. */
    private static boolean isNight(Level level) {
        long time = Math.floorMod(level.dimensionType().fixedTime().orElse(level.getDayTime()), DAY_LENGTH);
        return time >= NIGHT_START && time <= NIGHT_END;
    }

    private static BlockState damagedMonitor(BlockState state) {
        return CobblemonBlocks.DAMAGED_MONITOR.defaultBlockState().setValue(FACING, state.getValue(FACING));
    }
}
