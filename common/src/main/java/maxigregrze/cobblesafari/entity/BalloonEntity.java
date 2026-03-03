package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.MiscConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BalloonEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> DATA_HAS_DROPPED_LOOT =
            SynchedEntityData.defineId(BalloonEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float DESCENT_SPEED = -0.04F;
    private static final float ASCENT_SPEED = 0.48F; // 6x original speed
    private static final int DESPAWN_AFTER_LOOT_TICKS = 160; // 8 seconds after loot drop (doubled)

    public static final ResourceLocation BALLOON_LOOT_TABLE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "entities/balloon");

    protected int flyAwayTimer = 0;

    public BalloonEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // No AI goals - the balloon just floats
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HAS_DROPPED_LOOT, false);
    }

    public boolean hasDroppedLoot() {
        return this.entityData.get(DATA_HAS_DROPPED_LOOT);
    }

    protected void setHasDroppedLoot(boolean value) {
        this.entityData.set(DATA_HAS_DROPPED_LOOT, value);
    }

    @Override
    public void tick() {
        super.tick();

        if (hasDroppedLoot()) {
            // Flying away after loot drop
            this.setDeltaMovement(
                    this.getDeltaMovement().x * 0.95D,
                    ASCENT_SPEED,
                    this.getDeltaMovement().z * 0.95D
            );
            flyAwayTimer++;
            if (flyAwayTimer > DESPAWN_AFTER_LOOT_TICKS && !this.level().isClientSide) {
                this.discard();
            }
        } else {
            // Slowly descending (feather falling effect)
            if (!this.onGround()) {
                this.setDeltaMovement(
                        this.getDeltaMovement().x * 0.95D,
                        DESCENT_SPEED,
                        this.getDeltaMovement().z * 0.95D
                );
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
        }

        // Gentle horizontal drift (wind effect)
        if (this.tickCount % 40 == 0 && !this.level().isClientSide && !hasDroppedLoot()) {
            double driftX = (this.random.nextDouble() - 0.5) * 0.02;
            double driftZ = (this.random.nextDouble() - 0.5) * 0.02;
            this.setDeltaMovement(this.getDeltaMovement().add(driftX, 0, driftZ));
        }

        // Lifetime despawn
        if (this.tickCount > MiscConfig.getBalloonLifetimeTicks() && !this.level().isClientSide && !hasDroppedLoot()) {
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 0.5F, 1.2F);
            this.discard();
        }

        // Despawn if too high
        if (this.getY() > this.level().getMaxBuildHeight() + 10) {
            this.discard();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND && !hasDroppedLoot()) {
            dropBalloonLoot(player);
            setHasDroppedLoot(true);
            flyAwayTimer = 0;

            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.NEUTRAL, 1.0F, 1.0F);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void dropBalloonLoot(Player player) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        LootTable lootTable = serverLevel.getServer().reloadableRegistries()
                .getLootTable(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.LOOT_TABLE, BALLOON_LOOT_TABLE));

        LootParams lootParams = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .create(LootContextParamSets.GIFT);

        List<ItemStack> loot = lootTable.getRandomItems(lootParams);
        for (ItemStack stack : loot) {
            this.spawnAtLocation(stack);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true; // Needs to be true for interaction to work
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround,
                                   net.minecraft.world.level.block.state.BlockState state,
                                   BlockPos pos) {
        // No fall damage
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasDroppedLoot", hasDroppedLoot());
        tag.putInt("FlyAwayTimer", flyAwayTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HasDroppedLoot")) {
            setHasDroppedLoot(tag.getBoolean("HasDroppedLoot"));
        }
        if (tag.contains("FlyAwayTimer")) {
            flyAwayTimer = tag.getInt("FlyAwayTimer");
        }
    }
}
