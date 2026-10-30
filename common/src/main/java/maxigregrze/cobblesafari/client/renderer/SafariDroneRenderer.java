package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.entity.SafariDroneEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Draws the Safari exit drone (action plan 198, part G): its standalone Java block model, then the
 * translated "EXIT" label on its screen, where the Blockbench {@code TEXT} placeholder plane was.
 * The model's north face, which carries the screen, is turned onto the drone's heading.
 */
public class SafariDroneRenderer extends EntityRenderer<SafariDroneEntity> {

    private static final Direction[] QUAD_SIDES = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final RandomSource QUAD_RANDOM = RandomSource.create();

    private static final String LABEL_KEY = "entity.cobblesafari.safari_drone.screen";
    // The TEXT plane of safari_exit_drone.bbmodel, in model pixels: x 3..13, y 8..12, at z 6.75,
    // facing north. The label sits a hair in front of it, 0.26 px off the screen face (z = 7).
    private static final float LABEL_CENTRE_X = 8.0f;
    private static final float LABEL_CENTRE_Y = 10.0f;
    private static final float LABEL_Z = 6.74f;
    private static final float LABEL_WIDTH = 10.0f;
    private static final float LABEL_HEIGHT = 4.0f;
    /** Keeps the letters off the screen's edges. */
    private static final float LABEL_MARGIN = 0.9f;
    /** Height of a capital in the vanilla font, in font pixels. */
    private static final float CAP_HEIGHT = 7.0f;
    private static final int LABEL_COLOR = 0xFFFFFFFF;

    public SafariDroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(SafariDroneEntity drone, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - Mth.rotLerp(partialTick, drone.yRotO, drone.getYRot())));
        pose.translate(-0.5f, 0.0f, -0.5f);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        BakedModel model = UltraLootModels.get(UltraLootModels.SAFARI_EXIT_DRONE);
        for (Direction side : QUAD_SIDES) {
            QUAD_RANDOM.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, side, QUAD_RANDOM)) {
                consumer.putBulkData(pose.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, packedLight, OverlayTexture.NO_OVERLAY);
            }
        }
        this.renderLabel(pose, buffers);
        pose.popPose();
        super.render(drone, entityYaw, partialTick, pose, buffers, packedLight);
    }

    /**
     * The label in white, without shadow, scaled to fit the TEXT plane whatever its length in the
     * client's language, and lit as a screen (full bright).
     */
    private void renderLabel(PoseStack pose, MultiBufferSource buffers) {
        Font font = this.getFont();
        Component label = Component.translatable(LABEL_KEY);
        // The advance of the last glyph includes one pixel of spacing that is not ink.
        float textWidth = Math.max(1.0f, font.width(label) - 1.0f);
        float scale = Math.min(LABEL_WIDTH / textWidth, LABEL_HEIGHT / CAP_HEIGHT) * LABEL_MARGIN / 16.0f;

        pose.pushPose();
        pose.translate(LABEL_CENTRE_X / 16.0f, LABEL_CENTRE_Y / 16.0f, LABEL_Z / 16.0f);
        // Font quads face +Z; the screen faces north (-Z).
        pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        pose.scale(scale, -scale, scale);
        font.drawInBatch(label, -textWidth / 2.0f, -CAP_HEIGHT / 2.0f, LABEL_COLOR, false, pose.last().pose(),
                buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SafariDroneEntity drone) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
