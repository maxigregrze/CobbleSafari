package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Types de rendu partagés des visuels d'attaque CSBoss.
 */
public final class CsBossRenderTypes {

    /**
     * Textures des visuels « décalque ». Leur type de rendu est déclaré comme tampon fixe
     * ({@code RenderBuffersCsBossDecalMixin}) : il n'est vidé qu'après les block entities
     * ({@link #flushDecals}), au lieu de l'être au milieu de la passe des entités.
     */
    private static final List<ResourceLocation> DECAL_TEXTURES = List.of(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"),
            csboss("attack_shadow"),
            csboss("attack_shadow_large"),
            csboss("attack_shadowball"),
            csboss("attack_airslash"),
            csboss("attack_wave"));

    private CsBossRenderTypes() {}

    private static ResourceLocation csboss(String name) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/" + name + ".png");
    }

    /**
     * Visuel translucide qui <b>n'écrit pas la profondeur</b> (type « ombre d'entité » vanilla :
     * translucide, test de profondeur, léger décalage anti z-fighting). Un translucide qui écrit la
     * profondeur ({@code entityTranslucent}) masquait les blocs translucides dessinés après lui
     * (verre, blocs mimic transparents) : ils disparaissaient sous les zones et ombres d'attaque.
     * Cull actif : les quads doivent être dessinés dans les deux sens.
     *
     * <p>Sans écriture de profondeur, tout ce qui est dessiné <em>après</em> le décalque le recouvre :
     * c'était le cas des blocs mimic, rendus par leur block entity après les entités. D'où le tampon
     * fixe vidé après eux.
     */
    public static RenderType decal(ResourceLocation texture) {
        return RenderType.entityShadow(texture);
    }

    /** Ajoute un tampon fixe par type décalque (appelé à la construction de {@code RenderBuffers}). */
    public static void registerDecalBuffers(Map<RenderType, ByteBufferBuilder> fixedBuffers) {
        for (ResourceLocation texture : DECAL_TEXTURES) {
            RenderType type = decal(texture);
            fixedBuffers.putIfAbsent(type, new ByteBufferBuilder(type.bufferSize()));
        }
    }

    /**
     * Dessine les décalques : après la géométrie opaque et découpée des block entities (blocs mimic),
     * avant les translucides (terrain, verre imité), qui se fondent donc par-dessus.
     */
    public static void flushDecals(MultiBufferSource.BufferSource bufferSource) {
        for (ResourceLocation texture : DECAL_TEXTURES) {
            bufferSource.endBatch(decal(texture));
        }
    }
}
