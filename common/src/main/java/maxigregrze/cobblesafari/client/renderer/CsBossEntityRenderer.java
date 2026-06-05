package maxigregrze.cobblesafari.client.renderer;

import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rendu du Boss : modèle d'espèce Cobblemon emprunté via {@link CsBossModelRenderer},
 * avec échelle d'entrée et fondu de mort pilotés par la phase d'entité.
 */
public class CsBossEntityRenderer extends EntityRenderer<CsBossEntity> {

    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private final Map<UUID, PosableState> states = new HashMap<>();
    private final Map<String, CsBossModelRenderer.SpeciesInfo> speciesCache = new HashMap<>();
    private final Map<UUID, Integer> lastAttackSeq = new HashMap<>();
    private final Map<UUID, Integer> lastPhase = new HashMap<>();

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
        PosableState state = states.computeIfAbsent(boss.getUUID(), u -> new FloatingState());

        float limbSwing = boss.walkAnimation.position(partialTicks);
        float limbSwingAmount = Math.min(1.0f, boss.walkAnimation.speed(partialTicks));

        int phase = boss.getPhase();
        float anim = boss.getAnim();
        float scaleMul = phase == CsBossEntity.PHASE_ENTERING ? anim : 1.0f;
        float alpha = phase == CsBossEntity.PHASE_DYING ? Math.max(0.0f, 1.0f - anim) : 1.0f;
        if (scaleMul <= 0.001f) {
            return; // échelle nulle au tout début de l'entrée : rien à dessiner
        }

        float scale = info.baseScale() * boss.getSize() * scaleMul;
        float bodyYaw = Mth.rotLerp(partialTicks, boss.yBodyRotO, boss.yBodyRot);

        Runnable afterPose = () -> triggerAnimations(boss, state, phase);

        CsBossModelRenderer.render(ps, buffer, packedLight, boss, info, state,
                scale, alpha, bodyYaw, limbSwing, limbSwingAmount,
                boss.tickCount + partialTicks, partialTicks, afterPose);
    }

    /** Déclenche les animations nommées (attaque sur DATA_ATTACK_SEQ, faint à l'entrée en mort). */
    private void triggerAnimations(CsBossEntity boss, PosableState state, int phase) {
        int seq = boss.getAttackSeq();
        Integer lastSeq = lastAttackSeq.get(boss.getUUID());
        if (lastSeq == null || lastSeq != seq) {
            lastAttackSeq.put(boss.getUUID(), seq);
            if (seq != 0) {
                // « cry » en repli si l'espèce n'a pas d'animation de combat (ordre garanti via LinkedHashSet).
                state.addFirstAnimation(new java.util.LinkedHashSet<>(
                        java.util.List.of("physical", "special", "status", "cry")));
            }
        }
        Integer prevPhase = lastPhase.put(boss.getUUID(), phase);
        if (phase == CsBossEntity.PHASE_DYING && (prevPhase == null || prevPhase != CsBossEntity.PHASE_DYING)) {
            state.addFirstAnimation(Set.of("faint", "physical"));
        }
    }
}
