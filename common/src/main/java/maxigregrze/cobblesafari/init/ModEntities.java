package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.BalloonEntity;
import maxigregrze.cobblesafari.entity.BalloonSafariEntity;
import maxigregrze.cobblesafari.entity.HikerEntity;
import maxigregrze.cobblesafari.entity.projectile.ThrownBaitEntity;
import maxigregrze.cobblesafari.entity.projectile.ThrownMudBallEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public class ModEntities {

    private ModEntities() {}

    public static final EntityType<HikerEntity> HIKER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "hiker"),
            EntityType.Builder.<HikerEntity>of(HikerEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build(CobbleSafari.MOD_ID + ":hiker")
    );

    public static final EntityType<BalloonEntity> BALLOON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "balloon"),
            EntityType.Builder.<BalloonEntity>of(BalloonEntity::new, MobCategory.AMBIENT)
                    .sized(2.0f, 4.0f)
                    .clientTrackingRange(12)
                    .updateInterval(3)
                    .fireImmune()
                    .build(CobbleSafari.MOD_ID + ":balloon")
    );

    public static final EntityType<BalloonSafariEntity> BALLOON_SAFARI = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "balloon_safari"),
            EntityType.Builder.<BalloonSafariEntity>of(BalloonSafariEntity::new, MobCategory.AMBIENT)
                    .sized(2.0f, 4.0f)
                    .clientTrackingRange(12)
                    .updateInterval(3)
                    .fireImmune()
                    .build(CobbleSafari.MOD_ID + ":balloon_safari")
    );

    public static AttributeSupplier.Builder getHikerAttributes() {
        return HikerEntity.createAttributes();
    }

    public static AttributeSupplier.Builder getBalloonAttributes() {
        return BalloonEntity.createAttributes();
    }

    public static AttributeSupplier.Builder getBalloonSafariAttributes() {
        return BalloonSafariEntity.createAttributes();
    }

    public static final EntityType<ThrownMudBallEntity> THROWN_MUD_BALL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "thrown_mud_ball"),
            EntityType.Builder.<ThrownMudBallEntity>of(ThrownMudBallEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(CobbleSafari.MOD_ID + ":thrown_mud_ball")
    );

    public static final EntityType<ThrownBaitEntity> THROWN_BAIT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "thrown_bait"),
            EntityType.Builder.<ThrownBaitEntity>of(ThrownBaitEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(CobbleSafari.MOD_ID + ":thrown_bait")
    );

    public static void register() {
        CobbleSafari.LOGGER.info("Registering entities for " + CobbleSafari.MOD_ID);
    }
}
