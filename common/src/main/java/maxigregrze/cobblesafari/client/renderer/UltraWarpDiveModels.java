package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Function;

/**
 * The Ultra Warp Dive props, shared by their block entity renderer and the warp ride screen
 * (action plan 189, sections 4.3 and 4.4): model access, the negative's two animations, the
 * rocks' blue glint, and an alpha-capable model writer.
 */
public final class UltraWarpDiveModels {

    public static final ResourceLocation NEGATIVE_CORE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "block/ultrawarpdive_negative_core");
    public static final ResourceLocation NEGATIVE_RING =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "block/ultrawarpdive_negative_ring");

    /** Every model no blockstate references; both loaders register exactly this list. */
    public static final List<ResourceLocation> STANDALONE = List.of(NEGATIVE_CORE, NEGATIVE_RING);

    /** One ring flip every two seconds. A presentation constant: tune freely. */
    private static final float RING_DEG_PER_TICK = 9.0f;

    private static final float GLINT_R = 0.35f;
    private static final float GLINT_G = 0.55f;
    private static final float GLINT_B = 1.0f;

    /**
     * The glint's UV scale. Armour uses 0.16 because its UVs span a whole 64x32 skin texture; a
     * block model's UVs span one sprite of the block atlas, a hundredth of that, so at 0.16 the
     * streaks never cross a face and the glint reads as a flat, near-black wash - i.e. invisible.
     * 8.0 is what vanilla uses for block items, whose UVs are atlas UVs too.
     */
    private static final float GLINT_UV_SCALE = 8.0f;

    /** Six faces then the unculled quads; reused so no array is built per model. */
    private static final Direction[] QUAD_SIDES = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null
    };
    private static final RandomSource QUAD_RANDOM = RandomSource.create();

    private static Function<ResourceLocation, BakedModel> standaloneLookup;

    /**
     * The vanilla glint, tinted through {@code ColorModulator}: the glint shader ignores the vertex
     * colour, but multiplies its texture by the shader colour read at draw time.
     *
     * <p>Built on {@code entityGlintDirect()}, <b>not</b> {@code armorEntityGlint()} (action plan 191,
     * G1). The armour glint carries {@code VIEW_OFFSET_Z_LAYERING}, which nudges the armour and its
     * glint towards the eye together; the rock is drawn with no such offset, so the glint's
     * equal-depth test failed on every pixel and nothing was ever drawn, in the world or the ride.</p>
     */
    private static final float[] ROCK_GLINT_COLOR = new float[3];
    private static final RenderType ROCK_GLINT = tintedGlint("cobblesafari_ultrawarpdive_rock_glint",
            RenderType.entityGlintDirect(), GLINT_UV_SCALE, ROCK_GLINT_COLOR);

    private UltraWarpDiveModels() {}

    /**
     * A copy of a vanilla glint type whose colour is read from {@code rgb} each time it is drawn,
     * and whose texture scrolls at {@code uvScale}. The glint is additive: a darker colour is a
     * fainter glint. Pick the base whose layering matches what the glint covers: the armour glint
     * over armour-layer render types, the direct entity glint over everything else.
     */
    // RenderType is abstract without abstract members: an empty anonymous subclass is enough.
    public static RenderType tintedGlint(String name, RenderType base, float uvScale, float[] rgb) {
        return new RenderType(name, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536,
                false, false,
                () -> {
                    base.setupRenderState();
                    RenderSystem.setTextureMatrix(glintTextureMatrix(uvScale));
                    RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 1.0f);
                },
                () -> {
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    base.clearRenderState();
                }) {
        };
    }

    /** Vanilla's scrolling glint matrix ({@code RenderStateShard#setupGlintTexturing}). */
    private static Matrix4f glintTextureMatrix(float uvScale) {
        long time = (long) (Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * 8.0);
        float u = (time % 110_000L) / 110_000.0f;
        float v = (time % 30_000L) / 30_000.0f;
        return new Matrix4f().translation(-u, v, 0.0f)
                .rotateZ((float) (Math.PI / 18.0))
                .scale(uvScale);
    }

    /** Set once by each loader's client init, which alone knows how standalone models are keyed. */
    public static void setStandaloneLookup(Function<ResourceLocation, BakedModel> lookup) {
        standaloneLookup = lookup;
    }

    public static BakedModel standalone(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        return standaloneLookup != null
                ? standaloneLookup.apply(id)
                : mc.getModelManager().getMissingModel();
    }

    public static BakedModel positive() {
        return blockModel(ModBlocks.ULTRAWARPDIVE_POSITIVE);
    }

    /** The negative's still electric cage: its blockstate model. */
    public static BakedModel negativeCage() {
        return blockModel(ModBlocks.ULTRAWARPDIVE_NEGATIVE);
    }

    public static BakedModel rocks() {
        return blockModel(ModBlocks.ULTRAWARPDIVE_ROCKS);
    }

    private static BakedModel blockModel(net.minecraft.world.level.block.Block block) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(block.defaultBlockState());
    }

    public static RenderType rockGlint() {
        return ROCK_GLINT;
    }

    public static void setGlintAlpha(float alpha) {
        float a = Mth.clamp(alpha, 0.0f, 1.0f);
        ROCK_GLINT_COLOR[0] = GLINT_R * a;
        ROCK_GLINT_COLOR[1] = GLINT_G * a;
        ROCK_GLINT_COLOR[2] = GLINT_B * a;
    }

    static {
        setGlintAlpha(1.0f);
    }

    /** The blockstate rotation (north 0, east 90, south 180, west 270, clockwise from above). */
    public static void applyFacing(PoseStack pose, Direction facing) {
        float degrees = switch (facing) {
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
        if (degrees != 0.0f) {
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(-degrees));
            pose.translate(-0.5f, -0.5f, -0.5f);
        }
    }

    /** The core spins exactly like a shulker bullet, about the model centre. */
    public static void applyCoreSpin(PoseStack pose, float ageTicks) {
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.sin(ageTicks * 0.1f) * 180.0f));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.cos(ageTicks * 0.1f) * 180.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(ageTicks * 0.15f) * 360.0f));
        pose.translate(-0.5f, -0.5f, -0.5f);
    }

    /**
     * The ring turns about the side axis: facing is Z, top is Y, so the axis perpendicular to their
     * plane is X - the ring, drawn in the XY plane, flips like a coin spun on its edge.
     */
    public static void applyRingFlip(PoseStack pose, float ageTicks) {
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees((ageTicks * RING_DEG_PER_TICK) % 360.0f));
        pose.translate(-0.5f, -0.5f, -0.5f);
    }

    /**
     * Everything a placed prop draws outside the chunk mesh, in block space: the negative's core and
     * ring, or the rocks in full with their glint. No-op for any other state. Shared by the props'
     * own block entity renderer and by the CSBoss mimic, whose {@code renderSingleBlock} only ever
     * draws the baked model (the cage, or for the rocks nothing at all).
     *
     * @param ageTicks the animation clock, already phase-offset by the caller
     */
    public static void renderPlacedParts(BlockState state, PoseStack pose, MultiBufferSource buffers,
                                         float ageTicks) {
        boolean negative = state.is(ModBlocks.ULTRAWARPDIVE_NEGATIVE);
        boolean rocks = state.is(ModBlocks.ULTRAWARPDIVE_ROCKS);
        if ((!negative && !rocks) || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }
        pose.pushPose();
        applyFacing(pose, state.getValue(HorizontalDirectionalBlock.FACING));
        if (negative) {
            renderNegativeParts(pose, buffers.getBuffer(Sheets.cutoutBlockSheet()), ageTicks, 1.0f);
        } else {
            setGlintAlpha(1.0f);
            // NOT Sheets.solidBlockSheet(): that is a fixed buffer, flushed at the very end of the
            // block entity pass, after this glint - which then tests depth for equality against a
            // rock that is not drawn yet, and shows nothing. Two shared types instead: asking for
            // the glint buffer flushes the rock first, so the rock is always under its glint.
            putModel(pose.last(), buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)),
                    rocks(), 1.0f);
            putModel(pose.last(), buffers.getBuffer(ROCK_GLINT), rocks(), 1.0f);
        }
        pose.popPose();
    }

    /** The negative's two animated parts, on top of the caller's pose (the cage is drawn elsewhere). */
    public static void renderNegativeParts(PoseStack pose, VertexConsumer consumer, float ageTicks, float alpha) {
        pose.pushPose();
        applyCoreSpin(pose, ageTicks);
        putModel(pose.last(), consumer, standalone(NEGATIVE_CORE), alpha);
        pose.popPose();
        pose.pushPose();
        applyRingFlip(pose, ageTicks);
        putModel(pose.last(), consumer, standalone(NEGATIVE_RING), alpha);
        pose.popPose();
    }

    /**
     * Writes every quad of a baked model with a vertex alpha, full bright. {@code ModelBlockRenderer}
     * cannot do this: it only takes an RGB tint, and the warp ride fades its objects in and out.
     */
    public static void putModel(PoseStack.Pose pose, VertexConsumer consumer, BakedModel model, float alpha) {
        for (Direction side : QUAD_SIDES) {
            QUAD_RANDOM.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, side, QUAD_RANDOM)) {
                consumer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, alpha,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
        }
    }
}
