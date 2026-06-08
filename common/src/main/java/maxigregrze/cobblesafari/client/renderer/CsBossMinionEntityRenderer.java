package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minion rendering: Cobblemon species model via {@link CsBossModelRenderer}, idle/walk poses
 * and attack animation on DATA_ATTACK_SEQ.
 */
public class CsBossMinionEntityRenderer extends EntityRenderer<CsBossMinionEntity> {

    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private final Map<UUID, CsBossPosableState> states = new HashMap<>();
    private final Map<String, CsBossModelRenderer.SpeciesInfo> speciesCache = new HashMap<>();
    private final Map<UUID, Integer> lastAttackSeq = new HashMap<>();

    public CsBossMinionEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CsBossMinionEntity entity) {
        return FALLBACK_TEXTURE;
    }

    @Override
    public void render(CsBossMinionEntity minion, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        String specie = minion.getSpecie();
        if (specie != null && !specie.isBlank()) {
            try {
                CsBossModelRenderer.SpeciesInfo info = CsBossModelRenderer.resolve(specie, speciesCache);
                if (info != null) {
                    renderMinion(minion, partialTicks, ps, buffer, packedLight, info);
                }
            } catch (Exception e) {
                CobbleSafari.LOGGER.debug("[CSBoss] minion render failed for specie '{}'", specie, e);
            }
        }
        super.render(minion, yaw, partialTicks, ps, buffer, packedLight);
    }

    private void renderMinion(CsBossMinionEntity minion, float partialTicks, PoseStack ps,
                              MultiBufferSource buffer, int packedLight, CsBossModelRenderer.SpeciesInfo info) {
        CsBossPosableState state = states.computeIfAbsent(minion.getUUID(), u -> new CsBossPosableState());
        // Real-time animation clock (entity tick count), not framerate-dependent accumulated partial
        // ticks. See CsBossPosableState.
        state.setEntity(minion);
        state.updateAge(minion.tickCount);

        float limbSwing = minion.walkAnimation.position(partialTicks);
        float limbSwingAmount = Math.min(1.0f, minion.walkAnimation.speed(partialTicks));
        float scale = info.baseScale() * minion.getRenderScale();
        float bodyYaw = Mth.rotLerp(partialTicks, minion.yBodyRotO, minion.yBodyRot);

        Runnable afterPose = () -> {
            int seq = minion.getAttackSeq();
            Integer lastSeq = lastAttackSeq.get(minion.getUUID());
            // Queue: only start when no primary animation is playing, so we never cut one short.
            if (seq != 0 && (lastSeq == null || lastSeq != seq) && state.getPrimaryAnimation() == null) {
                lastAttackSeq.put(minion.getUUID(), seq);
                // "cry" as fallback if the species has no battle animation (order guaranteed).
                state.addFirstAnimation(new java.util.LinkedHashSet<>(
                        java.util.List.of("physical", "special", "status", "cry")));
            }
        };

        int overlay = minion.isFlashing()
                ? net.minecraft.client.renderer.texture.OverlayTexture.pack(
                        net.minecraft.client.renderer.texture.OverlayTexture.u(1.0F),
                        net.minecraft.client.renderer.texture.OverlayTexture.v(false))
                : net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        CsBossModelRenderer.render(ps, buffer, packedLight, overlay, minion, info, state,
                scale, minion.getAlpha(), bodyYaw, limbSwing, limbSwingAmount,
                minion.tickCount + partialTicks, partialTicks, afterPose);
    }
}
