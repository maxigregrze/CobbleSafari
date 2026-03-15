package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class HikerEntity extends CsTraderEntity {

    private boolean migrated = false;

    public HikerEntity(EntityType<? extends CsTraderEntity> entityType, Level level) {
        super(entityType, level);
        setTraderName(DEFAULT_TRADER_NAME);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || migrated || this.isRemoved()) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        CsTraderEntity replacement = new CsTraderEntity(ModEntities.CSTRADER_NPC, serverLevel);
        replacement.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        replacement.setYBodyRot(this.yBodyRot);
        replacement.setYHeadRot(this.getYHeadRot());
        replacement.setTraderName(this.getTraderName());
        replacement.setTradeType(this.getTradeType());
        replacement.setTradesInitialized(this.hasInitializedTrades());
        replacement.refreshDefinitionData();
        replacement.setPersistenceRequired();
        if (this.hasCustomName()) {
            replacement.setCustomName(this.getCustomName());
            replacement.setCustomNameVisible(this.isCustomNameVisible());
        }
        if (this.hasInitializedTrades() && !this.getOffers().isEmpty()) {
            this.getOffers().forEach(replacement.getOffers()::add);
        }

        this.migrated = true;
        serverLevel.addFreshEntity(replacement);
        this.discard();
    }
}
