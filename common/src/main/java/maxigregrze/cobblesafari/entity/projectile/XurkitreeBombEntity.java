package maxigregrze.cobblesafari.entity.projectile;

import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Static Heap (action plan 192, section 6). The strike only needs a clear column above the impact:
 * it ignores weather, biome and the dimension's skylight, so it also works where lightning never
 * falls naturally. A strike sounds the Channeling thunder; an obstructed landing crackles and drops
 * the item back.
 */
public class XurkitreeBombEntity extends ThrowableItemProjectile {

    /** Same as the Channeling trident's thunder (enchantment/channeling.json). */
    private static final float THUNDER_VOLUME = 5.0f;
    private static final float CRACKLE_VOLUME = 0.6f;

    public XurkitreeBombEntity(EntityType<? extends XurkitreeBombEntity> type, Level level) {
        super(type, level);
    }

    public XurkitreeBombEntity(Level level, LivingEntity thrower) {
        super(ModEntities.XURKITREE_BOMB, thrower, level);
    }

    public XurkitreeBombEntity(Level level, double x, double y, double z) {
        super(ModEntities.XURKITREE_BOMB, x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.XURKITREE_BOMB;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel serverLevel) || !isAlive()) {
            return;
        }
        Vec3 impact = impactPoint(result);
        BlockPos column = BlockPos.containing(impact);
        if (hasClearSky(column)) {
            strike(serverLevel, column);
            playLandingSound(serverLevel, impact, SoundEvents.TRIDENT_THUNDER.value(), THUNDER_VOLUME, 1.0f);
        } else {
            playLandingSound(serverLevel, impact, SoundEvents.LIGHTNING_BOLT_IMPACT, CRACKLE_VOLUME,
                    1.4f + serverLevel.getRandom().nextFloat() * 0.2f);
            if (!thrownFromCreative()) {
                spawnAtLocation(new ItemStack(ModItems.XURKITREE_BOMB));
            }
        }
        discard();
    }

    private static Vec3 impactPoint(HitResult result) {
        if (result instanceof EntityHitResult entityHit) {
            return entityHit.getEntity().position();
        }
        if (result instanceof BlockHitResult blockHit) {
            return Vec3.atCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection()));
        }
        return result.getLocation();
    }

    /**
     * Clear column above the impact. The heightmap ignores weather, biome and skylight, so it also
     * holds where lightning never falls; {@code canSeeSky} backs it up where the heightmap is stale.
     */
    private boolean hasClearSky(BlockPos pos) {
        return pos.getY() >= level().getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ())
                || level().canSeeSky(pos);
    }

    /**
     * Landing sound, in the neutral category so it does not hang on the weather slider like the
     * bolt's own thunder: the Channeling thunder on a strike, a short crackle when covered.
     */
    private static void playLandingSound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.NEUTRAL, volume, pitch);
    }

    /** A real, damaging bolt (not visual-only), credited to the thrower. */
    private void strike(ServerLevel level, BlockPos column) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(Vec3.atBottomCenterOf(column));
        bolt.setVisualOnly(false);
        if (getOwner() instanceof ServerPlayer player) {
            bolt.setCause(player);
        }
        level.addFreshEntity(bolt);
    }

    private boolean thrownFromCreative() {
        return getOwner() instanceof Player player && player.getAbilities().instabuild;
    }
}
