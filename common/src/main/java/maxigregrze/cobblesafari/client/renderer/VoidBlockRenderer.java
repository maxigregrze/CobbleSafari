package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.block.misc.VoidBlockEntity;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

public class VoidBlockRenderer implements BlockEntityRenderer<VoidBlockEntity> {

    private static final float SHADER_RED = 0.075f;
    private static final float SHADER_GREEN = 0.15f;
    private static final float SHADER_BLUE = 0.2f;

    public VoidBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VoidBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        poseStack.pushPose();

        if (ShaderCompatHelper.isIrisShaderActive()) {
            renderShaderCompat(poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderVanilla(poseStack, bufferSource);
        }

        poseStack.popPose();
    }

    private void renderVanilla(PoseStack poseStack, MultiBufferSource bufferSource) {
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.endPortal());

        renderFace(matrix, vertexConsumer, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        renderFace(matrix, vertexConsumer, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        renderFace(matrix, vertexConsumer, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f);
        renderFace(matrix, vertexConsumer, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f);
        renderFaceXZTop(matrix, vertexConsumer, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f);
        renderFaceXZ(matrix, vertexConsumer, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f);
    }

    private void renderShaderCompat(PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(
                RenderType.entitySolid(TheEndPortalRenderer.END_PORTAL_LOCATION));

        PoseStack.Pose pose = poseStack.last();
        float progress = ((Util.getMillis() / 1000.0f) * 0.01f) % 1.0f;
        int light = LightTexture.FULL_BRIGHT;

        shaderQuad(vertexConsumer, pose, Direction.SOUTH, progress, packedOverlay, light,
                0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f);

        shaderQuad(vertexConsumer, pose, Direction.NORTH, progress, packedOverlay, light,
                1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                1.0f, 1.0f, 0.0f);

        shaderQuad(vertexConsumer, pose, Direction.EAST, progress, packedOverlay, light,
                1.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                1.0f, 1.0f, 1.0f);

        shaderQuad(vertexConsumer, pose, Direction.WEST, progress, packedOverlay, light,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 0.0f);

        shaderQuad(vertexConsumer, pose, Direction.UP, progress, packedOverlay, light,
                0.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f);

        shaderQuad(vertexConsumer, pose, Direction.DOWN, progress, packedOverlay, light,
                0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f);
    }

    private void shaderQuad(VertexConsumer consumer, PoseStack.Pose pose, Direction direction,
                            float progress, int overlay, int light,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float x4, float y4, float z4) {
        float nx = direction.getStepX();
        float ny = direction.getStepY();
        float nz = direction.getStepZ();

        consumer.addVertex(pose, x1, y1, z1).setColor(SHADER_RED, SHADER_GREEN, SHADER_BLUE, 1.0f)
                .setUv(0.0f + progress, 0.0f + progress).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);

        consumer.addVertex(pose, x2, y2, z2).setColor(SHADER_RED, SHADER_GREEN, SHADER_BLUE, 1.0f)
                .setUv(0.0f + progress, 0.2f + progress).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);

        consumer.addVertex(pose, x3, y3, z3).setColor(SHADER_RED, SHADER_GREEN, SHADER_BLUE, 1.0f)
                .setUv(0.2f + progress, 0.2f + progress).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);

        consumer.addVertex(pose, x4, y4, z4).setColor(SHADER_RED, SHADER_GREEN, SHADER_BLUE, 1.0f)
                .setUv(0.2f + progress, 0.0f + progress).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private void renderFace(Matrix4f matrix, VertexConsumer consumer,
                            float x0, float x1, float y0, float y1,
                            float z0, float z1, float z2, float z3) {
        consumer.addVertex(matrix, x0, y0, z0);
        consumer.addVertex(matrix, x1, y0, z1);
        consumer.addVertex(matrix, x1, y1, z2);
        consumer.addVertex(matrix, x0, y1, z3);
    }

    private void renderFaceXZ(Matrix4f matrix, VertexConsumer consumer,
                              float x0, float x1, float z0, float z1, float y) {
        consumer.addVertex(matrix, x0, y, z0);
        consumer.addVertex(matrix, x1, y, z0);
        consumer.addVertex(matrix, x1, y, z1);
        consumer.addVertex(matrix, x0, y, z1);
    }

    private void renderFaceXZTop(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float x1, float z0, float z1, float y) {
        consumer.addVertex(matrix, x0, y, z1);
        consumer.addVertex(matrix, x1, y, z1);
        consumer.addVertex(matrix, x1, y, z0);
        consumer.addVertex(matrix, x0, y, z0);
    }

    @Override
    public boolean shouldRenderOffScreen(VoidBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
