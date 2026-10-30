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
    /** Wall-clock ms of the last frame each minion UUID was rendered; drives eviction of stale state (C3). */
    private final Map<UUID, Long> lastSeenMs = new HashMap<>();
    private long lastPurgeMs;
    private static final long STATE_TTL_MS = 30_000L;

    public CsBossMinionEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CsBossMinionEntity entity) {
        return FALLBACK_TEXTURE;
    }

    /** Evicts per-UUID animation state for minions not rendered for a while so these maps stay bounded (C3). */
    private void purgeStaleStates() {
        long now = System.currentTimeMillis();
        if (now - lastPurgeMs < 1_000L) {
            return;
        }
        lastPurgeMs = now;
        lastSeenMs.entrySet().removeIf(e -> {
            if (now - e.getValue() <= STATE_TTL_MS) {
                return false;
            }
            states.remove(e.getKey());
            lastAttackSeq.remove(e.getKey());
            return true;
        });
    }

    @Override
    public void render(CsBossMinionEntity minion, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        purgeStaleStates();
        if (minion.isInvisible()) {
            return; // clignotement de fuite / réapparition (CsBossVanish)
        }
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
        lastSeenMs.put(minion.getUUID(), System.currentTimeMillis());
        CsBossPosableState state = states.computeIfAbsent(minion.getUUID(), u -> new CsBossPosableState());
        // Real-time animation clock (entity tick count), not framerate-dependent accumulated partial
        // ticks. See CsBossPosableState.
        state.setEntity(minion);
        state.updateAge(minion.tickCount);

        float limbSwing = minion.walkAnimation.position(partialTicks);
        float limbSwingAmount = Math.min(1.0f, minion.walkAnimation.speed(partialTicks));
        com.cobblemon.mod.common.entity.PoseType forcedPose = switch (minion.getForcedPose()) {
            case CsBossMinionEntity.POSE_WALK -> com.cobblemon.mod.common.entity.PoseType.WALK;
            case CsBossMinionEntity.POSE_FLY -> com.cobblemon.mod.common.entity.PoseType.FLY;
            default -> null;
        };
        if (forcedPose != null) {
            // Déplacé par code (setPos) : on anime les membres à pleine allure plutôt que via la vitesse mesurée.
            limbSwing = (minion.tickCount + partialTicks) * 0.8f;
            limbSwingAmount = 1.0f;
        }
        float scale = info.baseScale() * minion.getRenderScale();
        float bodyYaw = Mth.rotLerp(partialTicks, minion.yBodyRotO, minion.yBodyRot);

        Runnable afterPose = () -> {
            int seq = minion.getAttackSeq();
            Integer lastSeq = lastAttackSeq.get(minion.getUUID());
            if (seq != 0 && (lastSeq == null || lastSeq != seq)) {
                // Consume the bump even when the model is busy. Minion bumps are *periodic* (every
                // 30 ticks), so queueing a bump that arrived mid-animation (boss behavior, where bumps
                // are per-attack and spaced out) would chain animations back-to-back: the next one
                // starts from the previous animation's end state instead of the idle pose, visibly
                // snapping models whose attack animation ends away from rest. Dropped bumps just mean
                // the next animation starts on the next bump after the model is free.
                lastAttackSeq.put(minion.getUUID(), seq);
                if (state.getPrimaryAnimation() == null && state.getActiveAnimations().isEmpty()) {
                    startBattleAnimation(state);
                }
            }
        };

        int overlay = minion.isFlashing()
                ? net.minecraft.client.renderer.texture.OverlayTexture.pack(
                        net.minecraft.client.renderer.texture.OverlayTexture.u(1.0F),
                        net.minecraft.client.renderer.texture.OverlayTexture.v(false))
                : net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        CsBossModelRenderer.render(ps, buffer, packedLight, overlay, minion, info, state,
                scale, minion.getAlpha(), bodyYaw, limbSwing, limbSwingAmount,
                minion.tickCount + partialTicks, partialTicks, afterPose, forcedPose);
    }

    /** Battle-animation priority: first existing wins; if none exist, no animation plays. */
    private static final java.util.List<String> BATTLE_ANIMATION_PRIORITY =
            java.util.List.of("physical", "special", "cry");

    /**
     * Plays the first battle animation the species defines, in {@link #BATTLE_ANIMATION_PRIORITY}
     * order. Resolved manually instead of {@code state.addFirstAnimation(...)}: standard species
     * posers define physical/special as {@code q.bedrock_primary(...)} - a {@code PrimaryAnimation}
     * whose lifecycle expects Cobblemon's per-tick entity delegate ({@code incrementAge}), which
     * this borrowed-model pipeline never runs, so primaries were observed to never play. Unwrapping
     * the primary and playing its inner bedrock animation as an <b>active</b> animation uses the
     * same path as the boss's stateful cry, which renders correctly.
     */
    private static void startBattleAnimation(CsBossPosableState state) {
        com.cobblemon.mod.common.client.render.models.blockbench.PosableModel model = state.getCurrentModel();
        if (model == null) {
            return;
        }
        for (String name : BATTLE_ANIMATION_PRIORITY) {
            com.cobblemon.mod.common.client.render.models.blockbench.animation.ActiveAnimation anim =
                    model.getAnimation(state, name, state.getRuntime());
            if (anim == null) {
                continue;
            }
            if (anim instanceof com.cobblemon.mod.common.client.render.models.blockbench.animation.PrimaryAnimation primary) {
                anim = primary.getAnimation();
            }
            state.addActiveAnimation(anim, s -> kotlin.Unit.INSTANCE);
            return;
        }
    }

}
