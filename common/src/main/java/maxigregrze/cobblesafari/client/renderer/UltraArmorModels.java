package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 3D armor models of the Ultra Space loot (action plan 192, section 10.3), converted by hand from
 * the author's Blockbench files (which are left untouched).
 * <ul>
 *   <li><b>Ultra Recon helmet / chestplate</b>: the big cube of each export stands for the vanilla
 *   piece, so it is replaced by vanilla's armor mesh (inflated by 1.0, bare {@code hat}). The other
 *   cubes undergo the very transform that inflate applies to that cube - a per-axis stretch about its
 *   centre - so the inflated assembly is the author's model, only bigger
 *   ({@link Reference#addFitted}).</li>
 *   <li><b>Nihilhat</b>: no reference cube; its 32 cubes are transcribed onto the {@code head} bone,
 *   raised by half a head (4 px), the two domes one pixel more. The two coincident dome cubes are
 *   split by inflate (opaque body 1.0, mostly transparent overlay 1.25). It is drawn translucent,
 *   which the armor layers cannot do: see {@link #renderNihilegoHat}.</li>
 * </ul>
 */
public final class UltraArmorModels {

    private static final CubeDeformation ARMOR_INFLATE = new CubeDeformation(1.0f);
    private static final CubeDeformation DOME_OVERLAY_INFLATE = new CubeDeformation(1.25f);
    private static final String HEAD = "head";
    private static final String HAT = "hat";
    private static final String BODY = "body";
    private static final String[] HUMANOID_PARTS = {HEAD, HAT, BODY, "right_arm", "left_arm", "right_leg", "left_leg"};

    /** Nihilhat: whole hat raised by half a head; the domes one more pixel. */
    private static final float NIHILEGO_TENTACLE_Y = -6.0f;
    private static final float NIHILEGO_DOME_Y = -7.0f;

    public static final ResourceLocation RECON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/models/armor/recon_armor_layer_1.png");
    public static final ResourceLocation NIHILEGO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/models/armor/nihilego_hat_layer_1.png");

    private static HumanoidModel<LivingEntity> reconHelmet;
    private static HumanoidModel<LivingEntity> reconChestplate;
    private static HumanoidModel<LivingEntity> nihilegoHat;
    private static HumanoidModel<LivingEntity> empty;

    private UltraArmorModels() {}

    /** The recon model for this stack, or {@code null} when it renders as vanilla armor. */
    @Nullable
    public static HumanoidModel<LivingEntity> reconModelFor(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.RECON_ARMOR_HELMET) {
            if (reconHelmet == null) {
                reconHelmet = new HumanoidModel<>(createReconHelmet().bakeRoot());
            }
            return reconHelmet;
        }
        if (item == ModItems.RECON_ARMOR_CHESTPLATE) {
            if (reconChestplate == null) {
                reconChestplate = new HumanoidModel<>(createReconChestplate().bakeRoot());
            }
            return reconChestplate;
        }
        return null;
    }

    /** A model with no cube: what the armor layer gets for the Nihilhat, drawn elsewhere. */
    public static HumanoidModel<LivingEntity> empty() {
        if (empty == null) {
            MeshDefinition mesh = new MeshDefinition();
            for (String part : HUMANOID_PARTS) {
                mesh.getRoot().addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
            }
            empty = new HumanoidModel<>(LayerDefinition.create(mesh, 16, 16).bakeRoot());
        }
        return empty;
    }

    /** Shows only the bones this slot covers, as {@code HumanoidArmorLayer#setPartVisibility} does. */
    public static void prepare(HumanoidModel<LivingEntity> model, EquipmentSlot slot) {
        model.setAllVisible(false);
        if (slot == EquipmentSlot.HEAD) {
            model.head.visible = true;
        } else if (slot == EquipmentSlot.CHEST) {
            model.body.visible = true;
            model.rightArm.visible = true;
            model.leftArm.visible = true;
        }
    }

    /**
     * Draws the Nihilhat on {@code parent}'s head with a translucent render type, so its texture may
     * use partial alpha. Both loaders' armor layers force a cutout type, hence this separate path.
     */
    @SuppressWarnings("unchecked")
    public static void renderNihilegoHat(PoseStack pose, MultiBufferSource buffers, int packedLight, ItemStack stack,
                                         HumanoidModel<?> parent) {
        if (nihilegoHat == null) {
            nihilegoHat = new HumanoidModel<>(createNihilegoHat().bakeRoot());
        }
        // copyPropertiesTo only reads the parent's poses: the entity type parameter is irrelevant.
        ((HumanoidModel<LivingEntity>) parent).copyPropertiesTo(nihilegoHat);
        prepare(nihilegoHat, EquipmentSlot.HEAD);
        VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffers,
                RenderType.entityTranslucent(NIHILEGO_TEXTURE), stack.hasFoil());
        nihilegoHat.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    }

    private static float rad(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }

    /** Vanilla's outer armor mesh, minus the hat overlay the author's helmet does not have. */
    private static MeshDefinition armorMesh() {
        MeshDefinition mesh = HumanoidModel.createMesh(ARMOR_INFLATE, 0.0f);
        mesh.getRoot().addOrReplaceChild(HAT, CubeListBuilder.create(), PartPose.ZERO);
        return mesh;
    }

    /**
     * The author's reference cube (the vanilla piece) and the stretch vanilla's 1.0 inflate gives it.
     * Inflating a box grows it by 1 px on each side, i.e. scales it about its centre by
     * {@code (size + 2) / size} per axis.
     */
    private record Reference(float minX, float minY, float minZ, float sizeX, float sizeY, float sizeZ) {

        float scale(float size) {
            return (size + 2.0f * ARMOR_INFLATE_PX) / size;
        }

        /**
         * Adds an extra cube transformed exactly as the reference was by the inflate, so the inflated
         * assembly looks like the author's model, only bigger. Its centre is scaled about the
         * reference's centre, and its size grows through a per-axis {@link CubeDeformation} so that
         * its box UV stays as authored.
         */
        void addFitted(CubeListBuilder builder, int u, int v, float x, float y, float z, float w, float h, float d) {
            float kx = scale(sizeX);
            float ky = scale(sizeY);
            float kz = scale(sizeZ);
            float cx = minX + sizeX / 2.0f;
            float cy = minY + sizeY / 2.0f;
            float cz = minZ + sizeZ / 2.0f;
            float centreX = cx + (x + w / 2.0f - cx) * kx;
            float centreY = cy + (y + h / 2.0f - cy) * ky;
            float centreZ = cz + (z + d / 2.0f - cz) * kz;
            builder.texOffs(u, v).addBox(centreX - w / 2.0f, centreY - h / 2.0f, centreZ - d / 2.0f, w, h, d,
                    new CubeDeformation(w * (kx - 1.0f) / 2.0f, h * (ky - 1.0f) / 2.0f, d * (kz - 1.0f) / 2.0f));
        }
    }

    private static final float ARMOR_INFLATE_PX = 1.0f;
    /** Vanilla head box, which the 8×8×8 helmet cube of recon_armor_hat.java stands for. */
    private static final Reference HEAD_BOX = new Reference(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f);
    /** Vanilla body box, which the 8×12×4 trunk of recon_armor_chestplate.java stands for. */
    private static final Reference BODY_BOX = new Reference(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f);

    static LayerDefinition createReconHelmet() {
        MeshDefinition mesh = armorMesh();
        PartDefinition head = mesh.getRoot().getChild(HEAD);
        // recon_armor_hat.java as exported (head-bone coordinates already), helmet cube dropped.
        CubeListBuilder extras = CubeListBuilder.create();
        HEAD_BOX.addFitted(extras, 32, 10, -4.0f, -8.0f, 4.0f, 8.0f, 3.0f, 3.0f);
        HEAD_BOX.addFitted(extras, 0, 4, -5.0f, -5.0f, -2.0f, 1.0f, 2.0f, 2.0f);
        HEAD_BOX.addFitted(extras, 0, 4, 4.0f, -5.0f, -2.0f, 1.0f, 2.0f, 2.0f);
        HEAD_BOX.addFitted(extras, 50, 11, -3.0f, -7.0f, -5.0f, 6.0f, 1.0f, 1.0f);
        HEAD_BOX.addFitted(extras, 44, 0, 2.0f, -9.0f, -3.0f, 1.0f, 1.0f, 9.0f);
        HEAD_BOX.addFitted(extras, 44, 0, -3.0f, -9.0f, -3.0f, 1.0f, 1.0f, 9.0f);
        head.addOrReplaceChild("recon_extras", extras, PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    static LayerDefinition createReconChestplate() {
        MeshDefinition mesh = armorMesh();
        PartDefinition body = mesh.getRoot().getChild(BODY);
        // recon_armor_chestplate.java, trunk (-7,-12,-1 8×12×4) dropped; every cube translated by
        // (+3, +12, -1), the move that lays that trunk onto the body bone's box.
        CubeListBuilder backpack = CubeListBuilder.create();
        BODY_BOX.addFitted(backpack, 24, 0, -3.0f, 1.0f, 2.0f, 6.0f, 5.0f, 3.0f);
        BODY_BOX.addFitted(backpack, 54, 15, -2.0f, 6.0f, 2.0f, 4.0f, 4.0f, 1.0f);
        BODY_BOX.addFitted(backpack, 42, 0, -2.0f, 9.0f, 3.0f, 4.0f, 1.0f, 2.0f);
        BODY_BOX.addFitted(backpack, 54, 27, -1.5f, 6.0f, 2.5f, 3.0f, 3.0f, 2.0f);
        BODY_BOX.addFitted(backpack, 0, 0, 2.0f, 6.0f, 2.0f, 1.0f, 2.0f, 1.0f);
        BODY_BOX.addFitted(backpack, 42, 6, -2.0f, 0.0f, 2.0f, 4.0f, 1.0f, 2.0f);
        BODY_BOX.addFitted(backpack, 0, 0, -3.0f, 6.0f, 2.0f, 1.0f, 2.0f, 1.0f);
        BODY_BOX.addFitted(backpack, 43, 1, -1.6f, 6.0f, 2.5f, 3.0f, 3.0f, 2.0f);
        body.addOrReplaceChild("recon_backpack", backpack, PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    static LayerDefinition createNihilegoHat() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(HAT, CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition head = root.addOrReplaceChild(HEAD, CubeListBuilder.create(), PartPose.ZERO);
        // nihilego_hat.bbmodel: x and y flipped from Blockbench space, rotations (-x, -y, z).
        head.addOrReplaceChild("dome", CubeListBuilder.create().texOffs(0, 34)
                .addBox(-8.0f, -6.0f, -1.0f, 9.0f, 6.0f, 9.0f, ARMOR_INFLATE),
                PartPose.offset(3.5f, NIHILEGO_DOME_Y, -3.5f));
        head.addOrReplaceChild("dome_overlay", CubeListBuilder.create().texOffs(0, 49)
                .addBox(-8.0f, -6.0f, -1.0f, 9.0f, 6.0f, 9.0f, DOME_OVERLAY_INFLATE),
                PartPose.offset(3.5f, NIHILEGO_DOME_Y, -3.5f));
        head.addOrReplaceChild("tentacle_front", CubeListBuilder.create().texOffs(20, 0)
                .addBox(-2.0f, -1.0f, 0.0f, 3.0f, 1.0f, 8.0f),
                PartPose.offsetAndRotation(-0.5f, NIHILEGO_TENTACLE_Y, 0.0f, rad(-175.0f), 0.0f, rad(180.0f)));
        // {x rotation, y rotation, z rotation, mirrored}
        float[][] tentacles = {
                {0.0f, 0.0f, 0.0f, 0}, {-2.5f, -12.5f, 0.0f, 0}, {-5.0f, -25.0f, 0.0f, 0}, {-2.5f, -37.5f, 0.0f, 0},
                {0.0f, -50.0f, 0.0f, 0}, {2.5f, -62.5f, 0.0f, 0}, {5.0f, -75.0f, 0.0f, 0}, {2.5f, -87.5f, 0.0f, 0},
                {0.0f, -100.0f, 0.0f, 0},
                {-177.5f, -17.5f, 180.0f, 0}, {-180.0f, -30.0f, 180.0f, 0}, {177.5f, -42.5f, -180.0f, 0},
                {175.0f, -55.0f, -180.0f, 0}, {177.5f, -67.5f, -180.0f, 0}, {-180.0f, -80.0f, 180.0f, 0},
                {177.5f, 67.5f, 180.0f, 1}, {175.0f, 55.0f, 180.0f, 1}, {177.5f, 42.5f, 180.0f, 1},
                {-180.0f, 30.0f, -180.0f, 1}, {-177.5f, 17.5f, -180.0f, 1}, {0.0f, 100.0f, 0.0f, 1},
                {2.5f, 87.5f, 0.0f, 1}, {5.0f, 75.0f, 0.0f, 1}, {2.5f, 62.5f, 0.0f, 1},
                {0.0f, 50.0f, 0.0f, 1}, {-2.5f, 37.5f, 0.0f, 1}, {-5.0f, 25.0f, 0.0f, 1},
                {-2.5f, 12.5f, 0.0f, 1}, {-180.0f, 80.0f, -180.0f, 1}
        };
        for (int i = 0; i < tentacles.length; i++) {
            float[] t = tentacles[i];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 0);
            if (t[3] != 0) {
                cube.mirror();
            }
            head.addOrReplaceChild("tentacle_" + i, cube.addBox(-1.0f, -1.0f, 0.0f, 2.0f, 1.0f, 8.0f),
                    PartPose.offsetAndRotation(0.0f, NIHILEGO_TENTACLE_Y, 0.0f, rad(t[0]), rad(t[1]), rad(t[2])));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
