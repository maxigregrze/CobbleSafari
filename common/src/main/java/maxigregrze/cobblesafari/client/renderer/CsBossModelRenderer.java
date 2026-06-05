package maxigregrze.cobblesafari.client.renderer;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Helper de rendu partagé : dessine un modèle d'espèce Cobblemon emprunté via
 * {@link VaryingModelRepository}, à l'endroit, à l'échelle voulue, avec poses idle/walk et fondu.
 * Réutilisé par le Boss et les Minions pour éviter toute dérive de la recette de rendu.
 */
public final class CsBossModelRenderer {

    /** Seuil de vitesse au-delà duquel on bascule en pose WALK. */
    public static final float MOVE_THRESHOLD = 0.05f;

    private CsBossModelRenderer() {}

    /** Espèce résolue + aspects + baseScale, mise en cache par chaîne {@code specie}. */
    public record SpeciesInfo(ResourceLocation species, Set<String> aspects, float baseScale) {}

    @Nullable
    public static SpeciesInfo resolve(String specie, Map<String, SpeciesInfo> cache) {
        SpeciesInfo cached = cache.get(specie);
        if (cached != null) {
            return cached;
        }
        try {
            Pokemon mon = PokemonProperties.Companion.parse(specie).create(null);
            RenderablePokemon renderable = mon.asRenderablePokemon();
            SpeciesInfo info = new SpeciesInfo(
                    renderable.getSpecies().getResourceIdentifier(),
                    renderable.getAspects(),
                    mon.getForm().getBaseScale());
            cache.put(specie, info);
            return info;
        } catch (Exception e) {
            CobbleSafari.LOGGER.debug("[CSBoss] could not resolve specie '{}'", specie, e);
            return null;
        }
    }

    /**
     * Dessine le modèle posé. {@code afterPose} est exécuté après le choix de pose et avant
     * l'application des animations (sert à déclencher des animations nommées : attaque, faint…).
     *
     * @param scale échelle finale (baseScale × size × éventuel multiplicateur d'animation).
     * @param alpha 1 = opaque ; &lt; 1 = fondu (translucide).
     */
    public static void render(
            PoseStack ps, MultiBufferSource buffer, int packedLight,
            @Nullable Entity entity, SpeciesInfo info, PosableState state,
            float scale, float alpha, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks, float partialTicks,
            @Nullable Runnable afterPose) {
        render(ps, buffer, packedLight, OverlayTexture.NO_OVERLAY, entity, info, state,
                scale, alpha, bodyYaw, limbSwing, limbSwingAmount, ageInTicks, partialTicks, afterPose);
    }

    /** Variante avec overlay explicite (flash blanc des minions, plan 107 § 5.3). */
    public static void render(
            PoseStack ps, MultiBufferSource buffer, int packedLight, int packedOverlay,
            @Nullable Entity entity, SpeciesInfo info, PosableState state,
            float scale, float alpha, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks, float partialTicks,
            @Nullable Runnable afterPose) {

        ResourceLocation species = info.species();
        state.setCurrentAspects(info.aspects());

        PosableModel model = VaryingModelRepository.INSTANCE.getPoser(species, state);
        ResourceLocation texture = VaryingModelRepository.INSTANCE.getTexture(species, state);

        RenderContext ctx = new RenderContext();
        model.setContext(ctx);
        ctx.put(RenderContext.Companion.getRENDER_STATE(), RenderContext.RenderState.WORLD);
        ctx.put(RenderContext.Companion.getSPECIES(), species);
        ctx.put(RenderContext.Companion.getASPECTS(), info.aspects());
        ctx.put(RenderContext.Companion.getPOSABLE_STATE(), state);
        state.setCurrentModel(model);

        boolean moving = limbSwingAmount > MOVE_THRESHOLD;
        Pose desired = model.getFirstSuitablePose(state, moving ? PoseType.WALK : PoseType.STAND);
        if (desired != null && !desired.getPoseName().equals(state.getCurrentPose())) {
            model.moveToPose(state, desired);
        }
        if (afterPose != null) {
            afterPose.run();
        }

        state.updatePartialTicks(partialTicks);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        ps.scale(-scale, -scale, scale);     // flip vanilla combiné à l'échelle du form
        ps.translate(0.0, -0.001, 0.0);      // net de -1.501 (vanilla) + 1.5 (Cobblemon)

        model.applyAnimations(entity, state, limbSwing, limbSwingAmount, ageInTicks, 0f, 0f);

        int color = (Mth.clamp((int) (alpha * 255.0f), 0, 255) << 24) | 0x00FFFFFF;
        RenderType renderType = alpha < 1.0f
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutout(texture);
        VertexConsumer vc = buffer.getBuffer(renderType);
        model.setLayerContext(buffer, state, VaryingModelRepository.INSTANCE.getLayers(species, state));
        model.render(ctx, ps, vc, packedLight, packedOverlay, color);
        model.resetLayerContext();
        ps.popPose();
    }
}
