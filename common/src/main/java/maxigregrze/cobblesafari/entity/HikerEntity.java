package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HikerEntity extends AbstractVillager {

    private static final String TRADE_TYPE_KEY = "TradeType";
    private static final String INITIALIZED_KEY = "TradesInitialized";
    private static final EntityDataAccessor<String> DATA_TRADE_TYPE =
            SynchedEntityData.defineId(HikerEntity.class, EntityDataSerializers.STRING);
    
    private static final ResourceLocation AIR_KEY = BuiltInRegistries.ITEM.getDefaultKey();

    private boolean tradesInitialized = false;

    public HikerEntity(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TRADE_TYPE, "small");
    }

    public String getTradeType() {
        return this.entityData.get(DATA_TRADE_TYPE);
    }

    public void setTradeType(String tradeType) {
        this.entityData.set(DATA_TRADE_TYPE, tradeType);
    }

    public boolean hasInitializedTrades() {
        return tradesInitialized;
    }
    
    /**
     * Checks if a MerchantOffer is invalid and would crash during NBT serialization.
     * Covers AIR items, empty stacks, unregistered items, and broken holders.
     */
    private static boolean isOfferInvalid(MerchantOffer offer) {
        if (offer == null) return true;
        try {
            ItemStack result = offer.getResult();
            if (result == null || result.isEmpty()) return true;
            if (result.is(Items.AIR)) return true;
            ResourceLocation resultKey = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (AIR_KEY.equals(resultKey)) return true;
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("Invalid Hiker trade offer detected during validation", e);
            return true;
        }
        return false;
    }

    public void initTradesForType(String tradeType) {
        if (tradesInitialized) {
            return;
        }
        
        setTradeType(tradeType);
        this.getOffers().clear();
        List<MerchantOffer> trades = HikerTrades.getTradesForType(tradeType, this.getRandom());
        for (MerchantOffer offer : trades) {
            if (!isOfferInvalid(offer)) {
                this.getOffers().add(offer);
            }
        }
        tradesInitialized = true;
    }

    @Override
    protected void updateTrades() {
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            if (!tradesInitialized) {
                initTradesForType(getTradeType());
            }
            if (this.getOffers().isEmpty()) {
                return InteractionResult.PASS;
            }
            this.setTradingPlayer(player);
            this.openTradingScreen(player, this.getDisplayName(), 1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        this.getOffers().removeIf(HikerEntity::isOfferInvalid);

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (stack.isEmpty() || stack.is(Items.AIR)) {
                this.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        try {
            super.addAdditionalSaveData(tag);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Failed to save Hiker trades, clearing offers as fallback", e);
            this.getOffers().clear();
            super.addAdditionalSaveData(tag);
        }
        tag.putString(TRADE_TYPE_KEY, getTradeType());
        tag.putBoolean(INITIALIZED_KEY, tradesInitialized);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TRADE_TYPE_KEY)) {
            setTradeType(tag.getString(TRADE_TYPE_KEY));
        }
        if (tag.contains(INITIALIZED_KEY)) {
            tradesInitialized = tag.getBoolean(INITIALIZED_KEY);
        }

        this.getOffers().removeIf(HikerEntity::isOfferInvalid);

        if (!level().isClientSide && !tradesInitialized) {
            String type = getTradeType();
            if (type != null && !type.isEmpty()) {
                initTradesForType(type);
            }
        }
    }
}
