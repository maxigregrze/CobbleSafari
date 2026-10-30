package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.block.csboss.CsBossUpdraftTileBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Courant ascendant ({@code base_flying_3}) : rend le modèle {@code attack_updraft} (geyser, cratère
 * en béton noir) en remplaçant la texture des rochers (prismarine dans {@code hot_geyser}) par celle du
 * bloc sur lequel la tuile est posée. Les quads re-texturés sont mis en cache par sprite de sol.
 */
public class CsBossUpdraftTileRenderer implements BlockEntityRenderer<CsBossUpdraftTileBlockEntity> {

    private static final ResourceLocation ROCK_TEXTURE = ResourceLocation.withDefaultNamespace("block/prismarine");
    private static final int STRIDE = DefaultVertexFormat.BLOCK.getVertexSize() / 4;
    private static final int UV_OFFSET = 4;

    private final BlockRenderDispatcher dispatcher;
    private final Map<TextureAtlasSprite, List<BakedQuad>> cache = new HashMap<>();
    private BakedModel cachedModel;

    public CsBossUpdraftTileRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CsBossUpdraftTileBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = be.getBlockState();
        BakedModel model = dispatcher.getBlockModel(state);
        if (model != cachedModel) {
            cache.clear(); // rechargement des ressources
            cachedModel = model;
        }
        // Bloc mimic : on prend le bloc qu'il imite, pas son modèle par défaut.
        BlockState floor = maxigregrze.cobblesafari.block.base.ApparentState.at(level, be.getBlockPos().below());
        TextureAtlasSprite floorSprite = floor.isAir() ? null : dispatcher.getBlockModel(floor).getParticleIcon();
        List<BakedQuad> quads = floorSprite == null
                ? collect(model, state, null)
                : cache.computeIfAbsent(floorSprite, s -> collect(model, state, s));
        VertexConsumer vc = buffer.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = ps.last();
        for (BakedQuad quad : quads) {
            vc.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
        }
    }

    /** Quads du modèle ; ceux des rochers sont re-mappés sur {@code floorSprite} (si non null). */
    private static List<BakedQuad> collect(BakedModel model, BlockState state, TextureAtlasSprite floorSprite) {
        List<BakedQuad> out = new ArrayList<>();
        RandomSource random = RandomSource.create(42L);
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        for (Direction side : sides) {
            for (BakedQuad quad : model.getQuads(state, side, random)) {
                boolean rock = quad.getSprite().contents().name().equals(ROCK_TEXTURE);
                out.add(rock && floorSprite != null ? retexture(quad, floorSprite) : quad);
            }
        }
        return out;
    }

    private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite to) {
        TextureAtlasSprite from = quad.getSprite();
        int[] v = quad.getVertices().clone();
        float du = from.getU1() - from.getU0();
        float dv = from.getV1() - from.getV0();
        for (int i = 0; i + STRIDE <= v.length; i += STRIDE) {
            float u = Float.intBitsToFloat(v[i + UV_OFFSET]);
            float w = Float.intBitsToFloat(v[i + UV_OFFSET + 1]);
            float fu = du == 0.0F ? 0.0F : (u - from.getU0()) / du;
            float fv = dv == 0.0F ? 0.0F : (w - from.getV0()) / dv;
            v[i + UV_OFFSET] = Float.floatToRawIntBits(to.getU0() + fu * (to.getU1() - to.getU0()));
            v[i + UV_OFFSET + 1] = Float.floatToRawIntBits(to.getV0() + fv * (to.getV1() - to.getV0()));
        }
        return new BakedQuad(v, quad.getTintIndex(), quad.getDirection(), to, quad.isShade());
    }
}
