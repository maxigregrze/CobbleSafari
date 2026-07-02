package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.teleporter.TeleportPadBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the teleport pad per-frame in the depth-sorted translucent buffer instead of the
 * chunk mesh. Tint-index quads receive the block entity colour; others render at full brightness.
 */
public class TeleportPadBlockEntityRenderer implements BlockEntityRenderer<TeleportPadBlockEntity> {

    private static final Direction[] QUAD_DIRECTIONS = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private static final String TINTABLE_AREA = "teleportpad_area_regu";
    private static final String TINTABLE_ARROW = "teleportpad_arrow_regu";
    private static final String TINTABLE_BASE_OVERLAY = "teleportpad_base_regu_overlay";

    private final BlockRenderDispatcher blockRenderDispatcher;

    public TeleportPadBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(TeleportPadBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        BakedModel model = this.blockRenderDispatcher.getBlockModel(state);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());

        boolean survival = state.is(ModBlocks.SURVIVAL_TELEPORT_PAD);
        int c = blockEntity.getTintColor();
        float r = ((c >> 16) & 0xFF) / 255f;
        float g = ((c >> 8) & 0xFF) / 255f;
        float b = (c & 0xFF) / 255f;

        RandomSource random = RandomSource.create();
        random.setSeed(42L);

        // FRONT pad linked to a laterally off-axis partner (±leeway window): yaw the model
        // around its vertical centre axis so the arrow points exactly at the partner pad.
        // TOP/BOTTOM and perfectly-aligned/unlinked pads keep right == 0 ⇒ no rotation.
        boolean rotate = blockEntity.isLinked() && blockEntity.getLinkRight() != 0;
        if (rotate) {
            float deg = (float) -Math.toDegrees(
                    Math.atan2(blockEntity.getLinkRight(), blockEntity.getLinkForward()));
            poseStack.pushPose();
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(deg));
            poseStack.translate(-0.5, 0.0, -0.5);
        }

        var pose = poseStack.last();
        for (Direction dir : QUAD_DIRECTIONS) {
            for (BakedQuad quad : model.getQuads(state, dir, random)) {
                if (survival && shouldTint(quad)) {
                    consumer.putBulkData(pose, quad, r, g, b, 1f, packedLight, OverlayTexture.NO_OVERLAY);
                } else {
                    consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY);
                }
            }
        }

        if (rotate) {
            poseStack.popPose();
        }
    }

    /** Tint-index quads plus the base overlay layer (overlay sprites may not retain tint flags after bake). */
    private static boolean shouldTint(BakedQuad quad) {
        if (quad.isTinted() || quad.getTintIndex() >= 0) {
            return true;
        }
        ResourceLocation sprite = quad.getSprite().contents().name();
        String path = sprite.getPath();
        return path.contains(TINTABLE_AREA)
                || path.contains(TINTABLE_ARROW)
                || path.contains(TINTABLE_BASE_OVERLAY);
    }
}
