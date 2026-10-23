package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * Draws a projectile as a standalone Java block model (action plan 192, section 7.4). The model's
 * north (-Z) is its nose, unless a yaw offset says otherwise (the paper plane's nose is south: 180°).
 * It is turned onto the entity's heading and pitch, using the convention of
 * {@code StickyProjectileEntity#faceMotion}. The 16³ model is centred horizontally on the entity.
 */
public class BakedModelProjectileRenderer<T extends Entity> extends EntityRenderer<T> {

    private static final Direction[] QUAD_SIDES = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final RandomSource QUAD_RANDOM = RandomSource.create();

    private final ResourceLocation modelId;
    private final float yCentre;
    private final float yawOffset;

    public BakedModelProjectileRenderer(EntityRendererProvider.Context context, ResourceLocation modelId, float yCentre) {
        this(context, modelId, yCentre, 0.0f);
    }

    /**
     * @param yCentre   height, in model pixels, of the point placed on the entity's position (the
     *                  model turns around it)
     * @param yawOffset extra yaw, in degrees, for a model whose nose is not on its north side
     */
    public BakedModelProjectileRenderer(EntityRendererProvider.Context context, ResourceLocation modelId, float yCentre,
                                        float yawOffset) {
        super(context);
        this.modelId = modelId;
        this.yCentre = yCentre;
        this.yawOffset = yawOffset;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        // Innermost, so the model's nose is brought to north before the pitch is applied.
        pose.mulPose(Axis.YP.rotationDegrees(yawOffset));
        pose.translate(-0.5f, -yCentre / 16.0f, -0.5f);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        BakedModel model = UltraLootModels.get(modelId);
        for (Direction side : QUAD_SIDES) {
            QUAD_RANDOM.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, side, QUAD_RANDOM)) {
                consumer.putBulkData(pose.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, packedLight, OverlayTexture.NO_OVERLAY);
            }
        }
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
