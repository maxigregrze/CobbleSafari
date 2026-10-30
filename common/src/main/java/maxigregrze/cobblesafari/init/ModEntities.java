package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.BalloonEntity;
import maxigregrze.cobblesafari.entity.BalloonSafariEntity;
import maxigregrze.cobblesafari.entity.CsTraderEntity;
import maxigregrze.cobblesafari.entity.HikerEntity;
import maxigregrze.cobblesafari.entity.HyperspaceBoatEntity;
import maxigregrze.cobblesafari.entity.HyperspaceChestBoatEntity;
import maxigregrze.cobblesafari.entity.SafariDroneEntity;
import maxigregrze.cobblesafari.entity.SeatEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossBulletEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackBeamEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionStemCoreEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionStemEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDigdirtEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionFlowerEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackGiratinaOrbEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackMeteoriteEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackPileProjectileEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackRedChainEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.CsBossPortalEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.CsBossSpawnProjectileEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowballEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShockwaveEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackWaveEntity;
import maxigregrze.cobblesafari.entity.projectile.ThrownBaitEntity;
import maxigregrze.cobblesafari.entity.projectile.ThrownBalmEntity;
import maxigregrze.cobblesafari.entity.projectile.ThrownMudBallEntity;
import maxigregrze.cobblesafari.entity.projectile.ThrownRedChainEntity;
import maxigregrze.cobblesafari.entity.projectile.BlacephalonBombEntity;
import maxigregrze.cobblesafari.entity.projectile.CelesteelaProjectileEntity;
import maxigregrze.cobblesafari.entity.projectile.PaperPlaneEntity;
import maxigregrze.cobblesafari.entity.projectile.NeedleDartEntity;
import maxigregrze.cobblesafari.entity.projectile.XurkitreeBombEntity;
import maxigregrze.cobblesafari.entity.safari.SafariBallisticMeteorEntity;
import maxigregrze.cobblesafari.entity.safari.SafariShadowHazardEntity;
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

    public static final EntityType<CsTraderEntity> CSTRADER_NPC = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "cstrader_npc"),
            EntityType.Builder.<CsTraderEntity>of(CsTraderEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build(CobbleSafari.MOD_ID + ":cstrader_npc")
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

    public static final EntityType<SafariDroneEntity> SAFARI_DRONE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "safari_drone"),
            EntityType.Builder.<SafariDroneEntity>of(SafariDroneEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.2f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .build(CobbleSafari.MOD_ID + ":safari_drone")
    );

    public static final EntityType<HyperspaceBoatEntity> HYPERSPACE_BOAT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "hyperspace_boat"),
            EntityType.Builder.<HyperspaceBoatEntity>of(HyperspaceBoatEntity::new, MobCategory.MISC)
                    .sized(1.375f, 0.5625f)
                    .eyeHeight(0.5625f)
                    .clientTrackingRange(10)
                    .build(CobbleSafari.MOD_ID + ":hyperspace_boat")
    );

    public static final EntityType<HyperspaceChestBoatEntity> HYPERSPACE_CHEST_BOAT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "hyperspace_chest_boat"),
            EntityType.Builder.<HyperspaceChestBoatEntity>of(HyperspaceChestBoatEntity::new, MobCategory.MISC)
                    .sized(1.375f, 0.5625f)
                    .eyeHeight(0.5625f)
                    .clientTrackingRange(10)
                    .build(CobbleSafari.MOD_ID + ":hyperspace_chest_boat")
    );

    public static AttributeSupplier.Builder getHikerAttributes() {
        return HikerEntity.createAttributes();
    }

    public static AttributeSupplier.Builder getCsTraderAttributes() {
        return CsTraderEntity.createAttributes();
    }

    public static AttributeSupplier.Builder getBalloonAttributes() {
        return BalloonEntity.createAttributes();
    }

    public static AttributeSupplier.Builder getBalloonSafariAttributes() {
        return BalloonSafariEntity.createAttributes();
    }

    public static AttributeSupplier.Builder getSafariDroneAttributes() {
        return SafariDroneEntity.createAttributes();
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

    public static final EntityType<ThrownBalmEntity> THROWN_BALM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "thrown_balm"),
            EntityType.Builder.<ThrownBalmEntity>of(ThrownBalmEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(CobbleSafari.MOD_ID + ":thrown_balm")
    );

    public static final EntityType<ThrownRedChainEntity> THROWN_RED_CHAIN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "thrown_red_chain"),
            EntityType.Builder.<ThrownRedChainEntity>of(ThrownRedChainEntity::new, MobCategory.MISC)
                    .sized(3.0f, 1.0f)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build(CobbleSafari.MOD_ID + ":thrown_red_chain")
    );

    // Ultra Space loot projectiles (action plan 192).
    public static final EntityType<NeedleDartEntity> POIPOLE_ARROW = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "poipole_arrow"),
            EntityType.Builder.<NeedleDartEntity>of(NeedleDartEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(CobbleSafari.MOD_ID + ":poipole_arrow")
    );

    public static final EntityType<BlacephalonBombEntity> BLACEPHALON_BOMB = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "blacephalon_bomb"),
            EntityType.Builder.<BlacephalonBombEntity>of(BlacephalonBombEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(CobbleSafari.MOD_ID + ":blacephalon_bomb")
    );

    public static final EntityType<XurkitreeBombEntity> XURKITREE_BOMB = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "xurkitree_bomb"),
            EntityType.Builder.<XurkitreeBombEntity>of(XurkitreeBombEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(CobbleSafari.MOD_ID + ":xurkitree_bomb")
    );

    public static final EntityType<CelesteelaProjectileEntity> CELESTEELA_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "celesteela_projectile"),
            EntityType.Builder.<CelesteelaProjectileEntity>of(CelesteelaProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build(CobbleSafari.MOD_ID + ":celesteela_projectile")
    );

    public static final EntityType<PaperPlaneEntity> PAPER_PLANE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "paper_plane"),
            EntityType.Builder.<PaperPlaneEntity>of(PaperPlaneEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.2f)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build(CobbleSafari.MOD_ID + ":paper_plane")
    );

    public static final EntityType<CsBossEntity> CSBOSS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "csboss"),
            EntityType.Builder.<CsBossEntity>of(CsBossEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(CobbleSafari.MOD_ID + ":csboss")
    );

    public static final EntityType<CsBossBulletEntity> CSBOSS_BULLET = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "csboss_bullet"),
            EntityType.Builder.<CsBossBulletEntity>of(CsBossBulletEntity::new, MobCategory.MISC)
                    .sized(1.0f, 5.0f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":csboss_bullet")
    );

    public static final EntityType<CsBossMinionEntity> CSBOSS_MINION = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "csboss_minion"),
            EntityType.Builder.<CsBossMinionEntity>of(CsBossMinionEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(CobbleSafari.MOD_ID + ":csboss_minion")
    );

    public static final EntityType<AttackShadowEntity> ATTACK_SHADOW = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_shadow"),
            EntityType.Builder.<AttackShadowEntity>of(AttackShadowEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.05f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_shadow")
    );

    public static final EntityType<AttackMeteoriteEntity> ATTACK_METEORITE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_meteorite"),
            EntityType.Builder.<AttackMeteoriteEntity>of(AttackMeteoriteEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_meteorite")
    );

    public static final EntityType<AttackDistortionStemEntity> ATTACK_DISTORTION_STEM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_distortion_stem"),
            EntityType.Builder.<AttackDistortionStemEntity>of(AttackDistortionStemEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_distortion_stem")
    );

    public static final EntityType<AttackDistortionStemCoreEntity> ATTACK_DISTORTION_STEM_CORE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_distortion_stem_core"),
            EntityType.Builder.<AttackDistortionStemCoreEntity>of(AttackDistortionStemCoreEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_distortion_stem_core")
    );

    public static final EntityType<AttackWaveEntity> ATTACK_WAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_wave"),
            EntityType.Builder.<AttackWaveEntity>of(AttackWaveEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_wave")
    );

    public static final EntityType<AttackShockwaveEntity> ATTACK_SHOCKWAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_shockwave"),
            EntityType.Builder.<AttackShockwaveEntity>of(AttackShockwaveEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_shockwave")
    );

    public static final EntityType<AttackDistortionFlowerEntity> ATTACK_DISTORTION_FLOWER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_distortion_flower"),
            EntityType.Builder.<AttackDistortionFlowerEntity>of(AttackDistortionFlowerEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_distortion_flower")
    );

    public static final EntityType<AttackBeamEntity> ATTACK_BEAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_beam"),
            EntityType.Builder.<AttackBeamEntity>of(AttackBeamEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_beam")
    );

    public static final EntityType<AttackGiratinaOrbEntity> ATTACK_GIRATINA_ORB = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_giratina_orb"),
            EntityType.Builder.<AttackGiratinaOrbEntity>of(AttackGiratinaOrbEntity::new, MobCategory.MISC)
                    .sized(0.9f, 0.9f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_giratina_orb")
    );

    public static final EntityType<AttackShadowballEntity> ATTACK_SHADOWBALL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_shadowball"),
            EntityType.Builder.<AttackShadowballEntity>of(AttackShadowballEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_shadowball")
    );

    public static final EntityType<AttackRedChainEntity> ATTACK_RED_CHAIN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_red_chain"),
            EntityType.Builder.<AttackRedChainEntity>of(AttackRedChainEntity::new, MobCategory.MISC)
                    .sized(3.0f, 1.0f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_red_chain")
    );

    public static final EntityType<AttackDigdirtEntity> ATTACK_DIGDIRT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_digdirt"),
            EntityType.Builder.<AttackDigdirtEntity>of(AttackDigdirtEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_digdirt")
    );

    public static final EntityType<AttackPileProjectileEntity> ATTACK_PILE_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_pile_projectile"),
            EntityType.Builder.<AttackPileProjectileEntity>of(AttackPileProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_pile_projectile")
    );

    public static final EntityType<CsBossSpawnProjectileEntity> CSBOSS_SPAWN_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "csboss_spawn_projectile"),
            EntityType.Builder.<CsBossSpawnProjectileEntity>of(CsBossSpawnProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":csboss_spawn_projectile")
    );

    public static final EntityType<CsBossPortalEntity> CSBOSS_PORTAL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "csboss_portal"),
            EntityType.Builder.<CsBossPortalEntity>of(CsBossPortalEntity::new, MobCategory.MISC)
                    .sized(10.0f, 1.0f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":csboss_portal")
    );

    public static final EntityType<SafariShadowHazardEntity> SAFARI_SHADOW_HAZARD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "safari_shadow_hazard"),
            EntityType.Builder.<SafariShadowHazardEntity>of(SafariShadowHazardEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":safari_shadow_hazard")
    );

    public static final EntityType<SafariBallisticMeteorEntity> SAFARI_BALLISTIC_METEOR = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "safari_ballistic_meteor"),
            EntityType.Builder.<SafariBallisticMeteorEntity>of(SafariBallisticMeteorEntity::new, MobCategory.MISC)
                    .sized(0.9f, 0.9f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":safari_ballistic_meteor")
    );

    // Invisible mount behind chairs and benches (plan 194 - 13). Never saved.
    public static final EntityType<SeatEntity> SEAT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "seat"),
            EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.001f, 0.001f)
                    .clientTrackingRange(10)
                    .noSummon()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":seat")
    );

    // Ho-Oh (plan 199 § 10)
    public static final EntityType<maxigregrze.cobblesafari.entity.csboss.attacks.AttackBellEntity> ATTACK_BELL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_bell"),
            EntityType.Builder.<maxigregrze.cobblesafari.entity.csboss.attacks.AttackBellEntity>of(
                            maxigregrze.cobblesafari.entity.csboss.attacks.AttackBellEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.5f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_bell")
    );

    public static final EntityType<maxigregrze.cobblesafari.entity.csboss.attacks.AttackTornadoEntity> ATTACK_TORNADO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_tornado"),
            EntityType.Builder.<maxigregrze.cobblesafari.entity.csboss.attacks.AttackTornadoEntity>of(
                            maxigregrze.cobblesafari.entity.csboss.attacks.AttackTornadoEntity::new, MobCategory.MISC)
                    .sized(1.5f, 3.0f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_tornado")
    );

    public static final EntityType<maxigregrze.cobblesafari.entity.csboss.attacks.AttackAirSlashEntity> ATTACK_AIR_SLASH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_air_slash"),
            EntityType.Builder.<maxigregrze.cobblesafari.entity.csboss.attacks.AttackAirSlashEntity>of(
                            maxigregrze.cobblesafari.entity.csboss.attacks.AttackAirSlashEntity::new, MobCategory.MISC)
                    .sized(1.6f, 0.5f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_air_slash")
    );

    public static final EntityType<maxigregrze.cobblesafari.entity.csboss.attacks.AttackZoneRingEntity> ATTACK_ZONE_RING = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_zone_ring"),
            EntityType.Builder.<maxigregrze.cobblesafari.entity.csboss.attacks.AttackZoneRingEntity>of(
                            maxigregrze.cobblesafari.entity.csboss.attacks.AttackZoneRingEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_zone_ring")
    );

    public static final EntityType<maxigregrze.cobblesafari.entity.csboss.attacks.AttackLaneShadowEntity> ATTACK_LANE_SHADOW = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "attack_lane_shadow"),
            EntityType.Builder.<maxigregrze.cobblesafari.entity.csboss.attacks.AttackLaneShadowEntity>of(
                            maxigregrze.cobblesafari.entity.csboss.attacks.AttackLaneShadowEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build(CobbleSafari.MOD_ID + ":attack_lane_shadow")
    );

    /** Porteur générique (plan 199 § 6) : sauvegardé (reprise du trajet via RootVehicle), mise à jour chaque tick. */
    public static final EntityType<maxigregrze.cobblesafari.entity.carrier.CarrierEntity> CARRIER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "carrier"),
            EntityType.Builder.<maxigregrze.cobblesafari.entity.carrier.CarrierEntity>of(
                            maxigregrze.cobblesafari.entity.carrier.CarrierEntity::new, MobCategory.MISC)
                    .sized(0.2f, 0.2f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSummon()
                    .fireImmune()
                    .build(CobbleSafari.MOD_ID + ":carrier")
    );

    public static AttributeSupplier.Builder getCsBossAttributes() {
        return CsBossEntity.createAttributes();
    }

    public static AttributeSupplier.Builder getCsBossMinionAttributes() {
        return CsBossMinionEntity.createAttributes();
    }

    public static void register() {
        CobbleSafari.LOGGER.info("Registering entities for " + CobbleSafari.MOD_ID);
    }
}
