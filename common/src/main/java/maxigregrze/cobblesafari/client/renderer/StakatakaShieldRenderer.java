package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.StakatakaShieldAnimator;
import maxigregrze.cobblesafari.item.ultraloot.StakatakaShieldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Stacked Shield item renderer (action plan 192, section 8.4): vanilla's shield model, with one of
 * twenty textures picked from the animation frame and the active / resting state. Each loader plugs
 * it into its own built-in item renderer hook.
 */
public final class StakatakaShieldRenderer {

    private static final int FRAMES = 10;
    private static final ResourceLocation[] ACTIVE = new ResourceLocation[FRAMES];
    private static final ResourceLocation[] IDLE = new ResourceLocation[FRAMES];

    static {
        for (int i = 0; i < FRAMES; i++) {
            ACTIVE[i] = texture("stakataka_shield_" + i);
            IDLE[i] = texture("stakataka_shield_" + i + "_idle");
        }
    }

    private static ShieldModel model;

    private StakatakaShieldRenderer() {}

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/stakataka_shield/" + name + ".png");
    }

    private static ShieldModel model() {
        // Layer definitions are code, not resources: one bake is valid for the whole session.
        if (model == null) {
            model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        }
        return model;
    }

    public static void render(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ClientLevel level = Minecraft.getInstance().level;
        boolean resting = level != null && StakatakaShieldItem.isResting(stack, level);
        int frame = Math.floorMod(StakatakaShieldAnimator.frame(stack), FRAMES);
        ResourceLocation texture = resting ? IDLE[frame] : ACTIVE[frame];

        ShieldModel shield = model();
        pose.pushPose();
        pose.scale(1.0f, -1.0f, -1.0f);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffers, shield.renderType(texture), true, stack.hasFoil());
        shield.renderToBuffer(pose, consumer, packedLight, packedOverlay);
        pose.popPose();
    }

    /** {@code minecraft:blocking} item property, as vanilla registers it for its own shield. */
    public static float blockingProperty(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f;
    }
}
