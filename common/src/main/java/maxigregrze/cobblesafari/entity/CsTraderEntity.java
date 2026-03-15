package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.cstrader.logic.CsTraderDefinition;
import maxigregrze.cobblesafari.cstrader.logic.CsTraderOfferFactory;
import maxigregrze.cobblesafari.cstrader.logic.CsTraderRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import java.util.Locale;

public class CsTraderEntity extends AbstractVillager {

    protected static final String TRADE_TYPE_KEY = "TradeType";
    protected static final String INITIALIZED_KEY = "TradesInitialized";
    protected static final String TRADER_NAME_KEY = "TraderName";
    protected static final String TRADER_VARIANT_KEY = "TraderVariantId";

    private static final EntityDataAccessor<String> DATA_VARIANT_ID =
            SynchedEntityData.defineId(CsTraderEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TRADER_NAME =
            SynchedEntityData.defineId(CsTraderEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TEXTURE_FILE =
            SynchedEntityData.defineId(CsTraderEntity.class, EntityDataSerializers.STRING);

    private static final ResourceLocation AIR_KEY = BuiltInRegistries.ITEM.getDefaultKey();

    public static final String DEFAULT_TRADER_NAME = "hiker";
    public static final String DEFAULT_VARIANT_ID = "small_sphere";
    public static final String DEFAULT_TEXTURE_FILE = "hiker";
    protected static final String LEGACY_SMALL = "small";
    protected static final String LEGACY_LARGE = "large";
    protected static final String LEGACY_TREASURE = "treasure";

    private boolean tradesInitialized = false;

    public CsTraderEntity(EntityType<? extends AbstractVillager> entityType, Level level) {
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
        builder.define(DATA_VARIANT_ID, DEFAULT_VARIANT_ID);
        builder.define(DATA_TRADER_NAME, DEFAULT_TRADER_NAME);
        builder.define(DATA_TEXTURE_FILE, DEFAULT_TEXTURE_FILE);
    }

    public String getTradeType() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    public void setTradeType(String tradeType) {
        this.entityData.set(DATA_VARIANT_ID, normalizeVariantId(tradeType));
    }

    public String getTraderName() {
        return this.entityData.get(DATA_TRADER_NAME);
    }

    public void setTraderName(String traderName) {
        if (traderName == null || traderName.isBlank()) {
            this.entityData.set(DATA_TRADER_NAME, DEFAULT_TRADER_NAME);
            applyDisplayName();
            return;
        }
        this.entityData.set(DATA_TRADER_NAME, traderName.toLowerCase(Locale.ROOT));
        applyDisplayName();
    }

    public String getTextureFile() {
        return this.entityData.get(DATA_TEXTURE_FILE);
    }

    protected void setTextureFile(String textureFile) {
        if (textureFile == null || textureFile.isBlank()) {
            this.entityData.set(DATA_TEXTURE_FILE, DEFAULT_TEXTURE_FILE);
            return;
        }
        this.entityData.set(DATA_TEXTURE_FILE, textureFile);
    }

    public boolean hasInitializedTrades() {
        return tradesInitialized;
    }

    public void setTradesInitialized(boolean initialized) {
        this.tradesInitialized = initialized;
    }

    private static boolean isOfferInvalid(MerchantOffer offer) {
        if (offer == null) return true;
        try {
            ItemStack result = offer.getResult();
            if (result == null || result.isEmpty()) return true;
            if (result.is(Items.AIR)) return true;
            ResourceLocation resultKey = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (AIR_KEY.equals(resultKey)) return true;
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("Invalid CSTrader offer detected during validation", e);
            return true;
        }
        return false;
    }

    public void initTradesForType(String tradeType) {
        if (tradesInitialized) {
            return;
        }
        setTradeType(tradeType);
        refreshDefinitionData();
        this.getOffers().clear();
        List<MerchantOffer> trades = CsTraderOfferFactory.generateOffers(getTraderName(), getTradeType(), this.getRandom());
        for (MerchantOffer offer : trades) {
            if (!isOfferInvalid(offer)) {
                this.getOffers().add(offer);
            }
        }
        tradesInitialized = true;
    }

    public void refreshDefinitionData() {
        CsTraderDefinition definition = CsTraderRegistry.getTrader(getTraderName());
        if (definition != null) {
            setTextureFile(definition.getTextureFile());
            if (getTradeType() == null || getTradeType().isBlank()) {
                setTradeType(definition.getDefaultVariantId());
            }
        } else {
            setTextureFile(DEFAULT_TEXTURE_FILE);
        }
        applyDisplayName();
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
            this.openTradingScreen(player, getTraderDisplayName(), 0);
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
        if (source.getEntity() instanceof Player player && player.isCreative()) {
            return false;
        }
        CsTraderDefinition definition = CsTraderRegistry.getTrader(getTraderName());
        boolean killeable = definition == null || definition.isKilleable();
        if (killeable) {
            return super.isInvulnerableTo(source);
        }
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        this.getOffers().removeIf(CsTraderEntity::isOfferInvalid);

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (stack.isEmpty() || stack.is(Items.AIR)) {
                this.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        try {
            super.addAdditionalSaveData(tag);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Failed to save CSTrader offers, clearing offers as fallback", e);
            this.getOffers().clear();
            super.addAdditionalSaveData(tag);
        }
        tag.putString(TRADE_TYPE_KEY, toLegacyTradeType(getTradeType()));
        tag.putString(TRADER_NAME_KEY, getTraderName());
        tag.putString(TRADER_VARIANT_KEY, getTradeType());
        tag.putBoolean(INITIALIZED_KEY, tradesInitialized);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TRADER_NAME_KEY)) {
            setTraderName(tag.getString(TRADER_NAME_KEY));
        } else {
            setTraderName(DEFAULT_TRADER_NAME);
        }
        if (tag.contains(TRADER_VARIANT_KEY)) {
            setTradeType(tag.getString(TRADER_VARIANT_KEY));
        }
        if (tag.contains(TRADE_TYPE_KEY) && !tag.contains(TRADER_VARIANT_KEY)) {
            setTradeType(tag.getString(TRADE_TYPE_KEY));
        }
        if (tag.contains(INITIALIZED_KEY)) {
            tradesInitialized = tag.getBoolean(INITIALIZED_KEY);
        }
        refreshDefinitionData();

        this.getOffers().removeIf(CsTraderEntity::isOfferInvalid);

        if (!level().isClientSide && !tradesInitialized) {
            String type = getTradeType();
            if (type != null && !type.isEmpty()) {
                initTradesForType(type);
            }
        }
    }

    protected static String normalizeVariantId(String variantId) {
        if (variantId == null || variantId.isBlank()) {
            return DEFAULT_VARIANT_ID;
        }
        String normalized = variantId.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case LEGACY_SMALL, "underground_small" -> DEFAULT_VARIANT_ID;
            case LEGACY_LARGE, "underground_large" -> "large_sphere";
            case LEGACY_TREASURE, "underground_treasure" -> "treasures";
            default -> normalized;
        };
    }

    protected static String toLegacyTradeType(String variantId) {
        if (variantId == null) return LEGACY_SMALL;
        return switch (variantId.toLowerCase(Locale.ROOT)) {
            case DEFAULT_VARIANT_ID, LEGACY_SMALL, "underground_small" -> LEGACY_SMALL;
            case "large_sphere", LEGACY_LARGE, "underground_large" -> LEGACY_LARGE;
            case "treasures", LEGACY_TREASURE, "underground_treasure" -> LEGACY_TREASURE;
            default -> variantId;
        };
    }

    private Component getTraderDisplayName() {
        CsTraderDefinition definition = CsTraderRegistry.getTrader(getTraderName());
        String fallbackName = definition == null ? getTraderName() : definition.getDisplayName();
        String translationKey = "entity.cobblesafari.cstrader_npc." + getTraderName();
        return Component.translatableWithFallback(translationKey, fallbackName);
    }

    private void applyDisplayName() {
        Component displayName = getTraderDisplayName();
        this.setCustomName(displayName);
        this.setCustomNameVisible(true);
    }
}
