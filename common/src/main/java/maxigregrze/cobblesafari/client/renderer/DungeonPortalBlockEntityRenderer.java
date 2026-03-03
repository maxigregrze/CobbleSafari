package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class DungeonPortalBlockEntityRenderer implements BlockEntityRenderer<DungeonPortalBlockEntity> {

    private static final int CIRCLE_SEGMENTS = 32;
    private static final float PORTAL_RADIUS = 0.95f;
    private static final float CENTER_X = 0.5f;
    private static final float CENTER_Y = 1.0f;
    private static final float CENTER_Z = 0.5f;

    private static final float SHADER_RED = 0.075f;
    private static final float SHADER_GREEN = 0.15f;
    private static final float SHADER_BLUE = 0.2f;

    public DungeonPortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DungeonPortalBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        poseStack.pushPose();

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(DungeonPortalBlock.FACING);

        if (ShaderCompatHelper.isIrisShaderActive()) {
            renderPortalCircleShaderCompat(poseStack, bufferSource, facing, packedLight, packedOverlay);
        } else {
            renderPortalCircle(poseStack, bufferSource, facing);
        }

        poseStack.popPose();
    }

    private void renderPortalCircle(PoseStack poseStack, MultiBufferSource bufferSource, Direction facing) {
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.endPortal());

        boolean isNorthSouth = (facing == Direction.NORTH || facing == Direction.SOUTH);

        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / CIRCLE_SEGMENTS);
            float angle2 = (float) (2 * Math.PI * (i + 1) / CIRCLE_SEGMENTS);

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            if (isNorthSouth) {
                float cx = CENTER_X;
                float cy = CENTER_Y;
                float z = CENTER_Z;

                float x1 = cx + PORTAL_RADIUS * cos1;
                float y1 = cy + PORTAL_RADIUS * sin1;
                float x2 = cx + PORTAL_RADIUS * cos2;
                float y2 = cy + PORTAL_RADIUS * sin2;

                vertexConsumer.addVertex(matrix, cx, cy, z);
                vertexConsumer.addVertex(matrix, x1, y1, z);
                vertexConsumer.addVertex(matrix, x2, y2, z);
                vertexConsumer.addVertex(matrix, x2, y2, z);

                vertexConsumer.addVertex(matrix, x2, y2, z);
                vertexConsumer.addVertex(matrix, x2, y2, z);
                vertexConsumer.addVertex(matrix, x1, y1, z);
                vertexConsumer.addVertex(matrix, cx, cy, z);
            } else {
                float x = CENTER_X;
                float cy = CENTER_Y;
                float cz = CENTER_Z;

                float z1 = cz + PORTAL_RADIUS * cos1;
                float y1 = cy + PORTAL_RADIUS * sin1;
                float z2 = cz + PORTAL_RADIUS * cos2;
                float y2 = cy + PORTAL_RADIUS * sin2;

                vertexConsumer.addVertex(matrix, x, cy, cz);
                vertexConsumer.addVertex(matrix, x, y1, z1);
                vertexConsumer.addVertex(matrix, x, y2, z2);
                vertexConsumer.addVertex(matrix, x, y2, z2);

                vertexConsumer.addVertex(matrix, x, y2, z2);
                vertexConsumer.addVertex(matrix, x, y2, z2);
                vertexConsumer.addVertex(matrix, x, y1, z1);
                vertexConsumer.addVertex(matrix, x, cy, cz);
            }
        }
    }

    private void renderPortalCircleShaderCompat(PoseStack poseStack, MultiBufferSource bufferSource,
                                                Direction facing, int packedLight, int packedOverlay) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(
                RenderType.entitySolid(TheEndPortalRenderer.END_PORTAL_LOCATION));

        PoseStack.Pose pose = poseStack.last();
        float progress = ((Util.getMillis() / 1000.0f) * 0.01f) % 1.0f;
        int light = LightTexture.FULL_BRIGHT;

        boolean isNorthSouth = (facing == Direction.NORTH || facing == Direction.SOUTH);
        float nx;
        float ny;
        float nz;
        if (isNorthSouth) {
            nx = 0.0f;
            ny = 0.0f;
            nz = 1.0f;
        } else {
            nx = 1.0f;
            ny = 0.0f;
            nz = 0.0f;
        }

        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / CIRCLE_SEGMENTS);
            float angle2 = (float) (2 * Math.PI * (i + 1) / CIRCLE_SEGMENTS);

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            float segProgress = progress + (float) i / CIRCLE_SEGMENTS * 0.2f;

            if (isNorthSouth) {
                float cx = CENTER_X;
                float cy = CENTER_Y;
                float z = CENTER_Z;

                float x1 = cx + PORTAL_RADIUS * cos1;
                float y1 = cy + PORTAL_RADIUS * sin1;
                float x2 = cx + PORTAL_RADIUS * cos2;
                float y2 = cy + PORTAL_RADIUS * sin2;

                shaderVertex(vertexConsumer, pose, cx, cy, z, 0.1f + segProgress, 0.1f + segProgress, nx, ny, nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x1, y1, z, 0.0f + segProgress, 0.2f + segProgress, nx, ny, nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x2, y2, z, 0.2f + segProgress, 0.2f + segProgress, nx, ny, nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x2, y2, z, 0.2f + segProgress, 0.2f + segProgress, nx, ny, nz, packedOverlay, light);

                shaderVertex(vertexConsumer, pose, x2, y2, z, 0.2f + segProgress, 0.2f + segProgress, -nx, -ny, -nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x2, y2, z, 0.2f + segProgress, 0.2f + segProgress, -nx, -ny, -nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x1, y1, z, 0.0f + segProgress, 0.2f + segProgress, -nx, -ny, -nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, cx, cy, z, 0.1f + segProgress, 0.1f + segProgress, -nx, -ny, -nz, packedOverlay, light);
            } else {
                float x = CENTER_X;
                float cy = CENTER_Y;
                float cz = CENTER_Z;

                float z1 = cz + PORTAL_RADIUS * cos1;
                float y1 = cy + PORTAL_RADIUS * sin1;
                float z2 = cz + PORTAL_RADIUS * cos2;
                float y2 = cy + PORTAL_RADIUS * sin2;

                shaderVertex(vertexConsumer, pose, x, cy, cz, 0.1f + segProgress, 0.1f + segProgress, nx, ny, nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x, y1, z1, 0.0f + segProgress, 0.2f + segProgress, nx, ny, nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x, y2, z2, 0.2f + segProgress, 0.2f + segProgress, nx, ny, nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x, y2, z2, 0.2f + segProgress, 0.2f + segProgress, nx, ny, nz, packedOverlay, light);

                shaderVertex(vertexConsumer, pose, x, y2, z2, 0.2f + segProgress, 0.2f + segProgress, -nx, -ny, -nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x, y2, z2, 0.2f + segProgress, 0.2f + segProgress, -nx, -ny, -nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x, y1, z1, 0.0f + segProgress, 0.2f + segProgress, -nx, -ny, -nz, packedOverlay, light);
                shaderVertex(vertexConsumer, pose, x, cy, cz, 0.1f + segProgress, 0.1f + segProgress, -nx, -ny, -nz, packedOverlay, light);
            }
        }
    }

    private void shaderVertex(VertexConsumer consumer, PoseStack.Pose pose,
                              float x, float y, float z,
                              float u, float v,
                              float nx, float ny, float nz,
                              int overlay, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(SHADER_RED, SHADER_GREEN, SHADER_BLUE, 1.0f)
                .setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(DungeonPortalBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
