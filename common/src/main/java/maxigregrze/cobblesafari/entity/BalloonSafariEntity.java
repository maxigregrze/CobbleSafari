package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SafariConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BalloonSafariEntity extends BalloonEntity {

    private static final String SAFARI_BALL_ITEM_ID = "cobblemon:safari_ball";

    public BalloonSafariEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND && !hasDroppedLoot()) {
            dropSafariBallLoot();
            setHasDroppedLoot(true);
            flyAwayTimer = 0;

            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.NEUTRAL, 1.0F, 1.0F);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void dropSafariBallLoot() {
        if (!(this.level() instanceof ServerLevel)) return;

        int minDrop = SafariConfig.getBalloonSafariMinDrop();
        int maxDrop = SafariConfig.getBalloonSafariMaxDrop();
        int safariBallCount = minDrop + this.random.nextInt(Math.max(1, maxDrop - minDrop + 1));

        Item safariBall = BuiltInRegistries.ITEM.get(ResourceLocation.parse(SAFARI_BALL_ITEM_ID));
        if (safariBall.getDefaultInstance().isEmpty()) {
            CobbleSafari.LOGGER.warn("Safari ball item not found: {}", SAFARI_BALL_ITEM_ID);
            return;
        }

        ItemStack safariBallStack = new ItemStack(safariBall, safariBallCount);
        this.spawnAtLocation(safariBallStack);

        if (this.random.nextFloat() < 0.5f) {
            boolean isBait = this.random.nextBoolean();
            int itemMin = SafariConfig.getBalloonSafariItemMinDrop();
            int itemMax = SafariConfig.getBalloonSafariItemMaxDrop();
            int extraCount = itemMin + this.random.nextInt(Math.max(1, itemMax - itemMin + 1));

            Item extraItem = isBait 
                    ? maxigregrze.cobblesafari.init.ModItems.BAIT 
                    : maxigregrze.cobblesafari.init.ModItems.MUD_BALL;
            
            ItemStack extraStack = new ItemStack(extraItem, extraCount);
            this.spawnAtLocation(extraStack);
        }
    }
}
