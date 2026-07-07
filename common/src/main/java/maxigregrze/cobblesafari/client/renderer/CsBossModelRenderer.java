package maxigregrze.cobblesafari.client.renderer;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.pose.Pose;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Species;
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
 * Shared rendering helper: draws a borrowed Cobblemon species model via
 * {@link VaryingModelRepository}, upright, at the desired scale, with idle/walk poses and fade.
 * Reused by the Boss and Minions to avoid any drift in the rendering recipe.
 */
public final class CsBossModelRenderer {

    /** Speed threshold above which the pose switches to WALK. */
    public static final float MOVE_THRESHOLD = 0.05f;

    private CsBossModelRenderer() {}

    /** Resolved species + aspects + baseScale, cached by {@code specie} string. */
    public record SpeciesInfo(ResourceLocation species, Set<String> aspects, float baseScale) {}

    /**
     * Resets the shared {@link PosableModel} singleton for {@code info}. Required when a chained
     * boss phase interrupts an in-progress animation (e.g. faint cut short at mid-dying): bone
     * transforms and tints would otherwise carry over even with a fresh {@link PosableState}.
     */
    public static void resetSharedModel(SpeciesInfo info, PosableState state) {
        state.setCurrentAspects(info.aspects());
        PosableModel model = VaryingModelRepository.INSTANCE.getPoser(info.species(), state);
        model.resetLayerContext();
        model.setDefault();
        model.setRed(1.0f);
        model.setGreen(1.0f);
        model.setBlue(1.0f);
        model.setAlpha(1.0f);
    }

    @Nullable
    public static SpeciesInfo resolve(String specie, Map<String, SpeciesInfo> cache) {
        SpeciesInfo cached = cache.get(specie);
        if (cached != null) {
            return cached;
        }
        try {
            // Resolve species + aspects straight from the parsed properties rather than building a full
            // Pokemon via create(). parse() computes aspects through the *properties* aspect providers
            // (AspectProvider.provide(PokemonProperties)) — the client-safe path Cobblemon itself uses.
            // This resolves tag/aspect-triggered forms (e.g. "rowlet hisuian") that the Pokemon-level
            // providers drop on the client, and it avoids create()'s roll()/events/held-item form logic,
            // which throws for some modded (held-item) special forms.
            PokemonProperties props = PokemonProperties.Companion.parse(specie);
            String speciesId = props.getSpecies();
            if (speciesId == null) {
                CobbleSafari.LOGGER.debug("[CSBoss] specie '{}' has no species", specie);
                return null;
            }
            ResourceLocation id = speciesId.indexOf(':') >= 0
                    ? ResourceLocation.parse(speciesId)
                    : ResourceLocation.fromNamespaceAndPath("cobblemon", speciesId);
            Species species = PokemonSpecies.INSTANCE.getByIdentifier(id);
            if (species == null) {
                CobbleSafari.LOGGER.debug("[CSBoss] unknown species '{}' for specie '{}'", id, specie);
                return null;
            }
            Set<String> aspects = props.getAspects();
            SpeciesInfo info = new SpeciesInfo(
                    species.getResourceIdentifier(),
                    aspects,
                    species.getForm(aspects).getBaseScale());
            cache.put(specie, info);
            return info;
        } catch (Exception e) {
            CobbleSafari.LOGGER.debug("[CSBoss] could not resolve specie '{}'", specie, e);
            return null;
        }
    }

    /**
     * Draws the posed model. {@code afterPose} runs after pose selection and before
     * animations are applied (used to trigger named animations: attack, faint…).
     *
     * @param scale final scale (baseScale × size × optional animation multiplier).
     * @param alpha 1 = opaque; &lt; 1 = fade (translucent).
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

    /** Variant with explicit overlay (minion white flash.3). */
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
        var layers = VaryingModelRepository.INSTANCE.getLayers(species, state);

        RenderContext ctx = new RenderContext();
        model.setContext(ctx);
        ctx.put(RenderContext.Companion.getRENDER_STATE(), RenderContext.RenderState.WORLD);
        ctx.put(RenderContext.Companion.getSPECIES(), species);
        ctx.put(RenderContext.Companion.getASPECTS(), info.aspects());
        ctx.put(RenderContext.Companion.getPOSABLE_STATE(), state);
        ctx.put(RenderContext.Companion.getTEXTURE(), texture);
        ctx.put(RenderContext.Companion.getSCALE(), info.baseScale());
        if (entity != null) {
            ctx.setEntity(entity);
        }
        state.setCurrentModel(model);

        // PosableModel instances are shared via VaryingModelRepository; reset tint like Cobblemon renderers do.
        model.setRed(1.0f);
        model.setGreen(1.0f);
        model.setBlue(1.0f);
        model.setAlpha(1.0f);

        boolean moving = limbSwingAmount > MOVE_THRESHOLD;
        Pose desired = model.getFirstSuitablePose(state, moving ? PoseType.WALK : PoseType.STAND);
        if (desired != null && !desired.getPoseName().equals(state.getCurrentPose())) {
            model.moveToPose(state, desired);
        }
        if (afterPose != null) {
            afterPose.run();
        }

        state.updatePartialTicks(partialTicks);

        // try/finally guarantees the PoseStack is balanced even if the borrowed Cobblemon model
        // throws while posing/rendering (notably on an entity's first frame right after it spawns).
        // An unbalanced PoseStack would corrupt every subsequent draw that frame → screen-wide flicker.
        ps.pushPose();
        try {
            ps.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
            ps.scale(-scale, -scale, scale); // vanilla flip combined with form scale
            ps.translate(0.0, -0.001, 0.0); // net of -1.501 (vanilla) + 1.5 (Cobblemon)

            model.applyAnimations(entity, state, limbSwing, limbSwingAmount, ageInTicks, 0f, 0f);

            int color = (Mth.clamp((int) (alpha * 255.0f), 0, 255) << 24) | 0x00FFFFFF;
            RenderType renderType = alpha < 1.0f
                    ? RenderType.entityTranslucent(texture)
                    : RenderType.entityCutout(texture);
            VertexConsumer vc = buffer.getBuffer(renderType);
            // Emissive / translucent resolver layers use PosableModel.getLayer() internally (same as Cobblemon mobs).
            model.setLayerContext(buffer, state, layers);
            try {
                model.render(ctx, ps, vc, packedLight, packedOverlay, color);
                model.setDefault();
            } finally {
                model.resetLayerContext();
            }
        } finally {
            model.setRed(1.0f);
            model.setGreen(1.0f);
            model.setBlue(1.0f);
            model.setAlpha(1.0f);
            ps.popPose();
        }
    }
}
