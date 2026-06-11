package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Boss rendering: borrowed Cobblemon species model via {@link CsBossModelRenderer},
 * with entry scale and death fade driven by entity phase.
 */
public class CsBossEntityRenderer extends EntityRenderer<CsBossEntity> {

    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    /** Échelle minimale atteinte en fin d'animation de départ (plan 122) : le boss devient très petit. */
    private static final float DEATH_MIN_SCALE = 0.05f;

    private final Map<UUID, CsBossPosableState> states = new HashMap<>();
    private final Map<String, CsBossModelRenderer.SpeciesInfo> speciesCache = new HashMap<>();
    private final Map<UUID, Integer> lastAttackSeq = new HashMap<>();
    private final Map<UUID, Integer> lastPhase = new HashMap<>();
    /** Last rendered {@code specie} string; used to reset animation when a chained phase changes it. */
    private final Map<UUID, String> lastSpecie = new HashMap<>();

    public CsBossEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CsBossEntity entity) {
        return FALLBACK_TEXTURE;
    }

    @Override
    public void render(CsBossEntity boss, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        String specie = boss.getSpecie();
        if (specie != null && !specie.isBlank()) {
            try {
                CsBossModelRenderer.SpeciesInfo info = CsBossModelRenderer.resolve(specie, speciesCache);
                if (info != null) {
                    renderBoss(boss, partialTicks, ps, buffer, packedLight, info);
                }
            } catch (Exception e) {
                CobbleSafari.LOGGER.debug("[CSBoss] render failed for specie '{}'", specie, e);
            }
        }
        super.render(boss, yaw, partialTicks, ps, buffer, packedLight);
    }

    private void renderBoss(CsBossEntity boss, float partialTicks, PoseStack ps,
                            MultiBufferSource buffer, int packedLight, CsBossModelRenderer.SpeciesInfo info) {
        int phase = boss.getPhase();
        String specie = boss.getSpecie();
        UUID id = boss.getUUID();
        Integer prevPhase = lastPhase.get(id);
        String prevSpecie = lastSpecie.get(id);
        if (needsAnimationReset(prevPhase, phase, prevSpecie, specie)) {
            states.remove(id);
            lastAttackSeq.remove(id);
            CsBossPosableState fresh = new CsBossPosableState();
            CsBossModelRenderer.resetSharedModel(info, fresh);
            states.put(id, fresh);
        }
        lastPhase.put(id, phase);
        lastSpecie.put(id, specie);

        CsBossPosableState state = states.computeIfAbsent(id, u -> new CsBossPosableState());
        // Drive the animation clock from the entity tick count (real-time), not from accumulated
        // partial ticks; otherwise playback speed scales with the framerate. See CsBossPosableState.
        state.setEntity(boss);
        state.updateAge(boss.tickCount);

        float limbSwing = boss.walkAnimation.position(partialTicks);
        float limbSwingAmount = Math.min(1.0f, boss.walkAnimation.speed(partialTicks));
        float anim = boss.getAnim();
        // Entrée : grandit de 0 à 1. Départ (plan 122) : rétrécit jusqu'à très petit pendant la mort.
        float scaleMul;
        if (phase == CsBossEntity.PHASE_ENTERING) {
            scaleMul = anim;
        } else if (phase == CsBossEntity.PHASE_DYING) {
            scaleMul = Mth.lerp(anim, 1.0f, DEATH_MIN_SCALE);
        } else {
            scaleMul = 1.0f;
        }
        float alpha = phase == CsBossEntity.PHASE_DYING ? Math.max(0.0f, 1.0f - anim) : 1.0f;
        if (scaleMul <= 0.001f) {
            return; // zero scale at the very start of entry: nothing to draw
        }

        float scale = info.baseScale() * boss.getSize() * scaleMul;
        float bodyYaw = Mth.rotLerp(partialTicks, boss.yBodyRotO, boss.yBodyRot);

        Runnable afterPose = () -> triggerAnimations(boss, state, phase, prevPhase);

        CsBossModelRenderer.render(ps, buffer, packedLight, boss, info, state,
                scale, alpha, bodyYaw, limbSwing, limbSwingAmount,
                boss.tickCount + partialTicks, partialTicks, afterPose);
    }

    /**
     * Chained boss phases reuse the same entity: the server cuts the faint animation at mid-dying
     * and restarts entrance. Drop client animation state and reset the shared Cobblemon model so
     * bone transforms from the interrupted faint do not carry over (even when the species is unchanged).
     */
    private static boolean needsAnimationReset(
            @Nullable Integer prevPhase, int phase, @Nullable String prevSpecie, String specie) {
        if (prevPhase != null
                && prevPhase == CsBossEntity.PHASE_DYING
                && phase == CsBossEntity.PHASE_ENTERING) {
            return true;
        }
        return prevSpecie != null && specie != null && !specie.isBlank() && !specie.equals(prevSpecie);
    }

    /** Triggers named animations (attack on DATA_ATTACK_SEQ, faint on entering death). */
    private void triggerAnimations(CsBossEntity boss, CsBossPosableState state, int phase,
                                   @Nullable Integer prevPhase) {
        int seq = boss.getAttackSeq();
        UUID id = boss.getUUID();
        Integer lastSeq = lastAttackSeq.get(id);
        // Only start the attack animation when no primary animation is playing: this queues it (plays
        // as soon as the model is free) instead of cutting one short, which visually breaks the model.
        if (seq != 0 && (lastSeq == null || lastSeq != seq) && state.getPrimaryAnimation() == null) {
            lastAttackSeq.put(id, seq);
            // "cry" as fallback if the species has no battle animation (order guaranteed via LinkedHashSet).
            state.addFirstAnimation(new java.util.LinkedHashSet<>(
                    java.util.List.of("physical", "special", "status", "cry")));
        }
        if (phase == CsBossEntity.PHASE_DYING && (prevPhase == null || prevPhase != CsBossEntity.PHASE_DYING)) {
            state.addFirstAnimation(Set.of("faint", "physical"));
        }
    }
}
