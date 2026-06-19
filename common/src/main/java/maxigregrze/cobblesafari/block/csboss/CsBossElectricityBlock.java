package maxigregrze.cobblesafari.block.csboss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Trap electric field placed by {@code base_electric_2}. Cycle managed by the block
 * itself (scheduled tick), independent of the attack: charge 3 s ({@code ACTIVE=false}, animated
 * texture) → active 3 s ({@code ACTIVE=true}) → removal. When active, strikes any player on
 * contact, with a 2 s cooldown per player.
 */
public class CsBossElectricityBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final int CHARGE_TICKS = 60; // 3 s
    private static final int ACTIVE_TICKS = 60; // 3 s
    private static final int STRIKE_COOLDOWN = 40; // 2 s per player
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    private static final SoundEvent BUZZ =
            SoundEvent.createVariableRangeEvent(ResourceLocation.parse("cobblemon:move.thundershock.actor"));
    /** Per-player global cooldown (game time of next allowed strike). */
    private static final Map<UUID, Long> NEXT_STRIKE = new HashMap<>();

    public CsBossElectricityBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            // No sound here: bulk placement (perlin) would play hundreds of sounds the same tick.
            // The "placement" buzz is played once by the attack; active ambience is
            // emitted locally by animateTick.
            level.scheduleTick(pos, this, CHARGE_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, ACTIVE_TICKS);
        } else {
            level.levelEvent(2001, pos, Block.getId(state));
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !state.getValue(ACTIVE) || !(entity instanceof Player player)
                || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = serverLevel.getGameTime();
        UUID uuid = player.getUUID();
        if (now < NEXT_STRIKE.getOrDefault(uuid, 0L)) {
            return;
        }
        NEXT_STRIKE.put(uuid, now + STRIKE_COOLDOWN);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(entity.blockPosition()));
            serverLevel.addFreshEntity(bolt);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + random.nextDouble(), pos.getY() + 0.2, pos.getZ() + random.nextDouble(),
                    0.0, 0.0, 0.0);
        }
        // Active-mode ambience buzz, emitted locally (no network packet).
        if (random.nextInt(24) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    BUZZ, SoundSource.HOSTILE, 0.4F, 1.3F, false);
        }
    }
}
