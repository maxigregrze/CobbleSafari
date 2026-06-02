package maxigregrze.cobblesafari.client.renderer;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.pose.Pose;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rendu du Boss : modèle d'espèce Cobblemon emprunté via {@link VaryingModelRepository}
 * (plan 100 § 5.2), sans instancier de PokemonEntity réelle.
 */
public class CsBossEntityRenderer extends EntityRenderer<CsBossEntity> {

    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final float MOVE_THRESHOLD = 0.05f;

    private final Map<UUID, PosableState> states = new HashMap<>();
    private final Map<String, CachedSpecies> speciesCache = new HashMap<>();
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
        if (specie == null || specie.isBlank()) {
            return;
        }
        try {
            CachedSpecies info = resolve(specie);
            if (info == null) {
                return;
            }
            ResourceLocation species = info.species;
            Set<String> aspects = info.aspects;

            PosableState state = states.computeIfAbsent(boss.getUUID(), u -> new FloatingState());
            state.setCurrentAspects(aspects);

            PosableModel model = VaryingModelRepository.INSTANCE.getPoser(species, state);
            ResourceLocation texture = VaryingModelRepository.INSTANCE.getTexture(species, state);

            RenderContext ctx = new RenderContext();
            model.setContext(ctx);
            ctx.put(RenderContext.Companion.getRENDER_STATE(), RenderContext.RenderState.WORLD);
            ctx.put(RenderContext.Companion.getSPECIES(), species);
            ctx.put(RenderContext.Companion.getASPECTS(), aspects);
            ctx.put(RenderContext.Companion.getPOSABLE_STATE(), state);
            state.setCurrentModel(model);

            // Entrées d'animation : la marche est pilotée par walkAnimation (mise à jour par LivingEntity).
            float limbSwing = boss.walkAnimation.position(partialTicks);
            float limbSwingAmount = Math.min(1.0f, boss.walkAnimation.speed(partialTicks));
            boolean moving = limbSwingAmount > MOVE_THRESHOLD;

            // Pose : WALK en mouvement, STAND à l'arrêt (moveToPose seulement au changement).
            PoseType targetPose = moving ? PoseType.WALK : PoseType.STAND;
            Pose desired = model.getFirstSuitablePose(state, targetPose);
            if (desired != null && !desired.getPoseName().equals(state.getCurrentPose())) {
                model.moveToPose(state, desired);
            }

            // Animation d'attaque déclenchée par le serveur (DATA_ATTACK_SEQ).
            int seq = boss.getAttackSeq();
            Integer last = lastAttackSeq.get(boss.getUUID());
            if (last == null || last != seq) {
                lastAttackSeq.put(boss.getUUID(), seq);
                if (seq != 0) {
                    state.addFirstAnimation(Set.of("physical", "special", "status"));
                }
            }

            // Phase d'entité (entrée / actif / mort) : échelle d'apparition + fondu de mort.
            int phase = boss.getPhase();
            float anim = boss.getAnim();
            Integer prevPhase = lastPhase.put(boss.getUUID(), phase);
            if (phase == CsBossEntity.PHASE_DYING && (prevPhase == null || prevPhase != CsBossEntity.PHASE_DYING)) {
                state.addFirstAnimation(Set.of("faint", "physical")); // animation de mort si l'espèce en a une
            }
            float scaleMul = phase == CsBossEntity.PHASE_ENTERING ? anim : 1.0f;
            float alpha = phase == CsBossEntity.PHASE_DYING ? Math.max(0.0f, 1.0f - anim) : 1.0f;
            if (scaleMul <= 0.001f) {
                // Rien à dessiner (échelle nulle au tout début de l'entrée).
                super.render(boss, yaw, partialTicks, ps, buffer, packedLight);
                return;
            }

            state.updatePartialTicks(partialTicks);

            ps.pushPose();
            float s = info.baseScale * boss.getSize() * scaleMul;
            float bodyYaw = Mth.rotLerp(partialTicks, boss.yBodyRotO, boss.yBodyRot);
            ps.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
            ps.scale(-s, -s, s);                 // flip vanilla combiné à l'échelle du form
            ps.translate(0.0, -0.001, 0.0);      // net de -1.501 (vanilla) + 1.5 (Cobblemon)

            model.applyAnimations(boss, state, limbSwing, limbSwingAmount,
                    boss.tickCount + partialTicks, 0f, 0f);

            int color = (Mth.clamp((int) (alpha * 255.0f), 0, 255) << 24) | 0x00FFFFFF;
            RenderType renderType = alpha < 1.0f
                    ? RenderType.entityTranslucent(texture)
                    : RenderType.entityCutout(texture);
            VertexConsumer vc = buffer.getBuffer(renderType);
            model.setLayerContext(buffer, state, VaryingModelRepository.INSTANCE.getLayers(species, state));
            model.render(ctx, ps, vc, packedLight, OverlayTexture.NO_OVERLAY, color);
            model.resetLayerContext();

            ps.popPose();
        } catch (Exception e) {
            CobbleSafari.LOGGER.debug("[CSBoss] render failed for specie '{}'", specie, e);
        }
        super.render(boss, yaw, partialTicks, ps, buffer, packedLight);
    }

    private CachedSpecies resolve(String specie) {
        CachedSpecies cached = speciesCache.get(specie);
        if (cached != null) {
            return cached;
        }
        try {
            Pokemon mon = PokemonProperties.Companion.parse(specie).create(null);
            RenderablePokemon renderable = mon.asRenderablePokemon();
            CachedSpecies info = new CachedSpecies(
                    renderable.getSpecies().getResourceIdentifier(),
                    renderable.getAspects(),
                    mon.getForm().getBaseScale());
            speciesCache.put(specie, info);
            return info;
        } catch (Exception e) {
            CobbleSafari.LOGGER.debug("[CSBoss] could not resolve specie '{}'", specie, e);
            return null;
        }
    }

    private record CachedSpecies(ResourceLocation species, Set<String> aspects, float baseScale) {}
}
