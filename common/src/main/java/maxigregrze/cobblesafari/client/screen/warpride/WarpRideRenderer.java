package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.dungeon.UltraWormholeBlockEntity;
import maxigregrze.cobblesafari.client.renderer.UltraWarpDiveModels;
import maxigregrze.cobblesafari.client.renderer.UltraWormholeVortex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the tunnel, its objects, the portals and the rider, in 1920x1080 virtual-canvas coordinates.
 *
 * <p>Since action plan 189 the objects, portals and rider are 3D models drawn in perspective passes
 * ({@link WarpRideScene3D}) whose projection reproduces this class's {@link #depthScale} law to the
 * pixel; since action plan 191 the tube is a 3D pass too ({@link WarpRideTube}).</p>
 */
public final class WarpRideRenderer {

    public static final int CENTER_X = 960;
    public static final int CENTER_Y = 540;
    /** cos(22.5°): the lane floor the rider and the lane objects sit on, inside the tube. */
    private static final double INRADIUS_FACTOR = 0.9238795325;

    public static final int LANE_COUNT = 8;

    /** The flat portal sprites (rank strip, portal_wrap) at the rider plane: the original size. */
    public static final int PORTAL_SPRITE_SIZE = 512;
    /** The 3D vortex's front layer at the rider plane: 25 % smaller than the sprites around it. */
    private static final int VORTEX_SIZE = 384;
    /** The vortex spans 3 blocks, so its front layer is VORTEX_SIZE wide at this many px per block. */
    private static final float PORTAL_BLOCK_PX = VORTEX_SIZE / 3.0f;

    /** Lane objects and decorative rocks are drawn this much larger than their first sizing. */
    static final float PROP_SCALE = 1.25f;
    /** One shared scale for the lane objects: 6.65 px per model pixel (the positive ring = 213 px), x1.25. */
    private static final float MODEL_BLOCK_PX = 16.0f * 6.65f * PROP_SCALE;

    /** The rider's on-screen reference size (the old sprite), which the energy aura scales from. */
    public static final int RIDER_SCREEN_PX = 192;
    /** Presentation constants: tune so the elytra span reads ~RIDER_SCREEN_PX wide. */
    private static final float RIDER_PX_PER_BLOCK = 128.0f;
    private static final float RIDER_CENTER_Y = 0.0f;
    /** The vanilla pivot is the feet; the flying body extends 1.8 blocks into the screen from it. */
    private static final float RIDER_CENTER_Z = 0.9f;

    /** Opacity keyframes by ring: the extra outer step fades 0 -> 25 %, then the original curve. */
    private static final double OPACITY_RING_1 = 0.25;
    private static final double OPACITY_RING_2 = 0.5;

    private static final String BASE = "textures/gui/warpride/";

    // The energy aura reuses the positive's flat 4-frame strip (plan 189, section 9.2).
    public static final ResourceLocation POSITIVE = tex("positive");

    // The rank strip still sits over the 3D portal; portal_wrap is played once over the warp.
    private static final ResourceLocation PORTAL_WRAP = tex("portal_wrap");
    private static final ResourceLocation[] PORTAL_RANKS = {
            null, tex("portal_rank1"), tex("portal_rank2"), tex("portal_rank3")
    };

    private static final int PORTAL_ANIM_FRAMES = 6;
    private static final int OBJECT_ANIM_FRAMES = 4;
    /** ~12.5 fps: a 6-frame loop in 480 ms. */
    private static final long PORTAL_ANIM_FRAME_NANOS = 80_000_000L;
    private static final int PORTAL_WRAP_FRAMES = 8;

    /** Animation clock in ticks, wrapped hourly so a float never loses its precision. */
    private static final long CLOCK_WRAP_MS = 3_600_000L;

    private static boolean filtersApplied;

    private static int viewLeft;
    private static int viewTop;
    private static int viewRight = 1920;
    private static int viewBottom = 1080;

    private static int physicalWidth = 1920;
    private static int physicalHeight = 1080;
    private static double fitScale = 1.0;

    // Baked models, looked up once per screen init rather than per object per frame (section 13.2).
    private static BakedModel positiveModel;
    private static BakedModel negativeCageModel;
    private static BakedModel negativeCoreModel;
    private static BakedModel negativeRingModel;
    private static BakedModel rocksModel;

    private WarpRideRenderer() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, BASE + name + ".png");
    }

    /** Linear filtering plus mipmaps on the ride's own textures only; the default GL_NEAREST shimmers. */
    public static void applyFilters() {
        if (filtersApplied) {
            return;
        }
        filtersApplied = true;
        // The HUD textures are in the list too: the canvas fit scale is fractional on most window
        // sizes, so they are never blitted at exactly 1:1 either.
        setFilter(WarpRideHud.BUZZ, true);
        // Strips get linear only: mipmapping a vertical strip averages adjacent frames together.
        List<ResourceLocation> strips = new ArrayList<>(List.of(POSITIVE, PORTAL_WRAP));
        for (ResourceLocation id : PORTAL_RANKS) {
            if (id != null) {
                strips.add(id);
            }
        }
        for (ResourceLocation id : strips) {
            setFilter(id, false);
        }
    }

    /** Called from the screen's init: every model the passes draw, resolved once. */
    public static void cacheModels() {
        positiveModel = UltraWarpDiveModels.positive();
        negativeCageModel = UltraWarpDiveModels.negativeCage();
        negativeCoreModel = UltraWarpDiveModels.standalone(UltraWarpDiveModels.NEGATIVE_CORE);
        negativeRingModel = UltraWarpDiveModels.standalone(UltraWarpDiveModels.NEGATIVE_RING);
        rocksModel = UltraWarpDiveModels.rocks();
    }

    static BakedModel rocksModel() {
        return rocksModel;
    }

    /**
     * The framebuffer in canvas coordinates, refreshed once per frame by the screen. A full-screen
     * dim, fade or flash fills this instead of 0,0..canvas, or the letterbox bands left by a window
     * that is not exactly 16:9 would stay uncovered.
     */
    public static void setViewport(double fit, double offsetX, double offsetY, int pw, int ph) {
        viewLeft = (int) Math.floor(-offsetX / fit);
        viewTop = (int) Math.floor(-offsetY / fit);
        viewRight = (int) Math.ceil((pw - offsetX) / fit);
        viewBottom = (int) Math.ceil((ph - offsetY) / fit);
        physicalWidth = pw;
        physicalHeight = ph;
        fitScale = fit;
    }

    /** Fills the whole window, in canvas coordinates, letterbox bands included. */
    public static void fillViewport(GuiGraphics graphics, int argb) {
        graphics.fill(viewLeft, viewTop, viewRight, viewBottom, argb);
    }

    /** The window's edges in canvas coordinates, for anything that has to sit flush against one. */
    public static int viewLeft() {
        return viewLeft;
    }

    public static int viewTop() {
        return viewTop;
    }

    public static int viewRight() {
        return viewRight;
    }

    public static int viewBottom() {
        return viewBottom;
    }

    /** Opens a perspective pass over the whole window. {@code graphics} is flushed first. */
    public static MultiBufferSource.BufferSource beginScene(GuiGraphics graphics) {
        graphics.flush();
        WarpRideScene3D.begin(physicalWidth, physicalHeight, fitScale);
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    public static void endScene(MultiBufferSource.BufferSource buffers) {
        WarpRideScene3D.end(buffers);
    }

    /** Blending is disabled by every fill/drawString flush, so this must precede every single blit. */
    public static void beginAlphaBlit() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static int loopFrame(int frameCount) {
        return (int) ((System.nanoTime() / PORTAL_ANIM_FRAME_NANOS) % frameCount);
    }

    /** Wall-clock animation time in ticks, for the model animations shared with the blocks. */
    static float animationTicks() {
        return (System.nanoTime() / 1_000_000L % CLOCK_WRAP_MS) / 50.0f;
    }

    private static void setFilter(ResourceLocation id, boolean mipmap) {
        try {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
            texture.setFilter(true, mipmap);
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("[WarpRide] Could not set the filter on {}", id, e);
        }
    }

    /** Perspective scale for a depth in front of the rider. */
    public static double depthScale(WarpRideClientState state, double depth) {
        if (depth >= 0.0) {
            return state.projectionK / (depth + state.projectionK);
        }
        // The formula has a pole at d = -k, so a fly-past is driven off a bounded parameter instead.
        double t = flyPastProgress(state, depth);
        return 1.0 + (state.flyPastMaxScale - 1.0) * t;
    }

    /** 0 at the rider's plane, 1 at the notional fifth plane. */
    public static double flyPastProgress(WarpRideClientState state, double depth) {
        if (depth >= 0.0) {
            return 0.0;
        }
        double span = Math.abs(state.fifthPlaneDepth);
        return Math.max(0.0, Math.min(1.0, -depth / span));
    }

    /**
     * 0 % at spawn, 25 % at ring 1 (the step added by plan 189), 50 % at ring 2, 100 % from ring 3,
     * back to 0 % at the fifth plane. From ring 1 inward the curve is the original one.
     */
    public static double opacity(WarpRideClientState state, double depth) {
        double spawn = state.visibleDepthLy;                 // 40
        double ring1 = ringDepth(state, 1);                  // 32
        double ring2 = ringDepth(state, 2);                  // 24
        double ring3 = ringDepth(state, 3);                  // 16

        if (depth >= spawn) {
            return 0.0;
        }
        if (depth >= ring1) {
            return OPACITY_RING_1 * ((spawn - depth) / (spawn - ring1));
        }
        if (depth >= ring2) {
            return OPACITY_RING_1 + (OPACITY_RING_2 - OPACITY_RING_1) * ((ring1 - depth) / (ring1 - ring2));
        }
        if (depth >= ring3) {
            return OPACITY_RING_2 + (1.0 - OPACITY_RING_2) * ((ring2 - depth) / (ring2 - ring3));
        }
        if (depth >= 0.0) {
            return 1.0;
        }
        return 1.0 - flyPastProgress(state, depth);
    }

    /** Screen angle of a lane. Driven by the continuous displayLane, so turns and jumps sweep. */
    private static double laneAngleDeg(int objectLane, WarpRideClientState state) {
        return 90.0 + 45.0 * (objectLane - state.displayLane);
    }

    /** Depth of one of the decorative rings, ring 1 being the farthest. Drives the opacity curve. */
    private static double ringDepth(WarpRideClientState state, int ring) {
        return state.visibleDepthLy * (state.ringCount - ring) / (double) state.ringCount;
    }

    /**
     * The cloud tube (plan 191, V1), in its own perspective pass, behind every object.
     *
     * @param minDepth the start tunnel's exit during the intro (nothing nearer is drawn), else
     *                 {@code Double.NEGATIVE_INFINITY}
     */
    public static void renderTube(GuiGraphics graphics, WarpRideClientState state, double minDepth) {
        if (minDepth >= state.visibleDepthLy) {
            return;
        }
        MultiBufferSource.BufferSource buffers = beginScene(graphics);
        WarpRideTube.render(buffers, state, minDepth);
        endScene(buffers);
    }

    /** The intro's start tunnel (plan 191, T1), in its own perspective pass, behind everything 3D. */
    /**
     * @param exitSeconds intro time at which the rider flies out of the tunnel's open end
     * @return false once the tunnel is entirely behind the drawn range: no need to draw it again
     */
    public static boolean renderStartTunnel(GuiGraphics graphics, WarpRideStartTunnel tunnel,
                                            WarpRideClientState state, double seconds, double exitSeconds) {
        MultiBufferSource.BufferSource buffers = beginScene(graphics);
        // The tunnel's mouth narrows onto the cloud tube's radius, so the two line up at the exit.
        boolean visible = tunnel.render(buffers, new org.joml.Matrix4f(), seconds, exitSeconds,
                WarpRideScene3D.zFor(1.0), (float) state.ringFourRadius);
        endScene(buffers);
        return visible;
    }

    /** The rider's centre in world units, which the intro camera orbits around. */
    public static org.joml.Vector3f riderCenter(WarpRideClientState state) {
        return new org.joml.Vector3f(0.0f, (float) -riderRadius(state), WarpRideScene3D.zFor(1.0));
    }

    // --- portals -------------------------------------------------------------------------------

    /** The depth a portal is drawn at: parked portals follow their own hover/fade clock. */
    private static double objectDepth(WarpRideClientState.ClientWave wave,
                                      WarpRideClientState.ClientObject object,
                                      WarpRideClientState state, long now) {
        return object.isPortal() && wave.portalCrossNanos != 0
                ? wave.portalDepth(now, state.fifthPlaneDepth)
                : wave.depth(state.odometer);
    }

    /** The wormhole block's vortex, on the axis, front layer at the given depth, facing the rider. */
    private static void drawVortex(PoseStack pose, MultiBufferSource buffers, WarpRideClientState state,
                                   int typeId, double depthScale, double sizeFactor, float alpha) {
        float size = (float) (PORTAL_BLOCK_PX * sizeFactor);
        if (size <= 0.0f) {
            return;
        }
        float spin = (animationTicks() * UltraWormholeBlockEntity.SPIN_Z_DEGREES_PER_TICK) % 360.0f;
        pose.pushPose();
        pose.translate(0.0f, 0.0f, WarpRideScene3D.zFor(depthScale));
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        // Layer 0 (the front) towards the rider, the deeper layers away into the screen.
        pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        pose.scale(size, size, size);
        pose.translate(-0.5f, -0.5f, 0.0f);
        // Turned 180 degrees, the stack's authored normal would face away from the eye and the
        // layers would sit at the ambient floor of the lighting - dim enough to read as see-through.
        UltraWormholeVortex.render(pose.last(), buffers, state.tintFor(typeId), alpha,
                UltraWormholeVortex.BLOCK_LAYER_SPACING, LightTexture.FULL_BRIGHT, true);
        pose.popPose();
    }

    /** The flat rank strip over a portal; rank 0 has none. */
    private static void drawPortalRank(GuiGraphics graphics, int rank, int size, double alpha) {
        int r = Math.max(0, Math.min(PORTAL_RANKS.length - 1, rank));
        ResourceLocation rankStrip = PORTAL_RANKS[r];
        if (rankStrip != null && size > 0) {
            drawSpriteFrame(graphics, rankStrip, CENTER_X, CENTER_Y, size, alpha,
                    loopFrame(PORTAL_ANIM_FRAMES), PORTAL_ANIM_FRAMES);
        }
    }

    // --- lane objects and rider ------------------------------------------------------------------

    /** One thing to draw in front of the rider: a lane object, or a portal (lane -1). */
    private record AheadItem(WarpRideClientState.ClientWave wave, WarpRideClientState.ClientObject object,
                             double depth, double orderDepth) {}

    /** Reused every frame; only the ride's render thread touches it. */
    private static final List<AheadItem> AHEAD = new ArrayList<>();

    /**
     * The main perspective walk, strictly far to near: lane objects still ahead and portals sorted
     * together by depth, then the rider, then the missed objects flying past.
     *
     * <p>A portal is a vortex (3D) topped by a flat rank strip (2D), and 2D cannot sit inside a
     * depth-tested pass. So the pass is closed after each portal, its strip blitted, and a fresh
     * pass opened for whatever is nearer. An object farther than the portal is therefore drawn
     * before it and hidden behind it; a nearer one is drawn after it, in front. (Portals used to
     * have a pass of their own drawn first, so every object landed on top whatever its depth.)</p>
     *
     * <p>A portal never comes in front of the rider: while it hovers it shares the rider's plane,
     * and as it fades out past the plane it keeps that place - as the 2D ride always drew it.</p>
     */
    public static void renderScene(GuiGraphics graphics, WarpRideClientState state,
                                   WarpRideRiderProxy rider, float partialTick,
                                   WarpRideCamera camera, WarpRideRiderEffects effects) {
        long now = System.nanoTime();
        collectAhead(state, now);
        MultiBufferSource.BufferSource buffers = beginScene(graphics);
        PoseStack pose = new PoseStack();
        for (AheadItem item : AHEAD) {
            if (!item.object().isPortal()) {
                drawLaneObject(pose, buffers, state, item.wave(), item.object(), item.depth());
                continue;
            }
            double alpha = opacity(state, item.depth());
            double scale = depthScale(state, item.depth());
            drawVortex(pose, buffers, state, item.object().typeId(), scale, 1.0, (float) alpha);
            endScene(buffers);
            beginAlphaBlit();
            // The 2D strip follows the camera feedback, or it would slide off its 3D vortex.
            graphics.pose().pushPose();
            camera.apply2D(graphics.pose(), now);
            drawPortalRank(graphics, item.object().rank(), (int) Math.round(PORTAL_SPRITE_SIZE * scale), alpha);
            graphics.pose().popPose();
            RenderSystem.disableBlend();
            buffers = beginScene(graphics);
        }
        AHEAD.clear();
        if (rider != null) {
            drawRider(pose, buffers, rider, riderRadius(state), 1.0, state.bankDegrees(), partialTick, effects);
        }
        renderPastObjects(pose, buffers, state);
        endScene(buffers);
    }

    /** Fills {@link #AHEAD} with the visible portals and not-yet-crossed lane objects, far first. */
    private static void collectAhead(WarpRideClientState state, long now) {
        AHEAD.clear();
        for (WarpRideClientState.ClientWave wave : state.waves()) {
            double waveDepth = wave.depth(state.odometer);
            for (WarpRideClientState.ClientObject object : wave.objects) {
                double depth = objectDepth(wave, object, state, now);
                if (opacity(state, depth) <= 0.01) {
                    continue;
                }
                if (object.isPortal()) {
                    // Clamped to the plane: drawn before the rider even while fading past it.
                    AHEAD.add(new AheadItem(wave, object, depth, Math.max(0.0, depth)));
                } else if (waveDepth > 0.0) {
                    AHEAD.add(new AheadItem(wave, object, depth, depth));
                }
            }
        }
        // Farthest first; on a tie the portal goes first, behind the objects sharing its depth.
        AHEAD.sort((a, b) -> {
            int byDepth = Double.compare(b.orderDepth(), a.orderDepth());
            if (byDepth != 0) {
                return byDepth;
            }
            return Boolean.compare(b.object().isPortal(), a.object().isPortal());
        });
    }

    /**
     * Draws the missed lane objects flying past, behind the rider's plane, far to near. Objects
     * still ahead and the portals are walked by {@link #renderScene}.
     */
    private static void renderPastObjects(PoseStack pose, MultiBufferSource buffers, WarpRideClientState state) {
        // Painter's algorithm, and the list is nearest-first: walking it forwards would draw the
        // most distant wave last and therefore on top.
        List<WarpRideClientState.ClientWave> waves = state.waves();
        for (int i = waves.size() - 1; i >= 0; i--) {
            WarpRideClientState.ClientWave wave = waves.get(i);
            double depth = wave.depth(state.odometer);
            if (depth > 0.0) {
                continue;
            }
            for (WarpRideClientState.ClientObject object : wave.objects) {
                // Only *missed* lane objects fly past; a hit one vanished at the plane.
                if (object.isPortal() || wave.consumedLanes.contains(object.lane())) {
                    continue;
                }
                drawLaneObject(pose, buffers, state, wave, object, depth);
            }
        }
    }

    private static void drawLaneObject(PoseStack pose, MultiBufferSource buffers, WarpRideClientState state,
                                       WarpRideClientState.ClientWave wave,
                                       WarpRideClientState.ClientObject object, double depth) {
        float alpha = (float) opacity(state, depth);
        if (alpha <= 0.01f) {
            return;
        }
        double angleDeg = laneAngleDeg(object.lane(), state);
        double angle = Math.toRadians(angleDeg);
        // The unscaled position at the rider plane: depth alone (eye-space z) shrinks it on screen,
        // exactly as the 2D radius * depthScale did - fly-past included.
        double floor = state.ringFourRadius * INRADIUS_FACTOR;

        pose.pushPose();
        pose.translate((float) (Math.cos(angle) * floor), (float) (-Math.sin(angle) * floor),
                WarpRideScene3D.zFor(depthScale(state, depth)));
        // The model's top points at the axis, as the sprite's did; its front (north) faces the rider.
        pose.mulPose(Axis.ZP.rotationDegrees((float) (90.0 - angleDeg)));
        pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        pose.scale(MODEL_BLOCK_PX, MODEL_BLOCK_PX, MODEL_BLOCK_PX);
        pose.translate(-0.5f, -0.5f, -0.5f);

        if (object.kind() == 0) {
            UltraWarpDiveModels.putModel(pose.last(), translucentBuffer(buffers), positiveModel, alpha);
        } else {
            UltraWarpDiveModels.putModel(pose.last(), translucentBuffer(buffers), negativeCageModel, alpha);
            // Phase offset per wave and lane, so two negatives never spin in lockstep.
            float age = animationTicks() + (wave.index * 8 + object.lane()) * 37.0f;
            pose.pushPose();
            UltraWarpDiveModels.applyCoreSpin(pose, age);
            UltraWarpDiveModels.putModel(pose.last(), translucentBuffer(buffers), negativeCoreModel, alpha);
            pose.popPose();
            pose.pushPose();
            UltraWarpDiveModels.applyRingFlip(pose, age);
            UltraWarpDiveModels.putModel(pose.last(), translucentBuffer(buffers), negativeRingModel, alpha);
            pose.popPose();
        }
        pose.popPose();
    }

    static com.mojang.blaze3d.vertex.VertexConsumer translucentBuffer(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    }

    /** The rider's distance below the axis: the floor, the jump's cosine arc, or the drift inward. */
    public static double riderRadius(WarpRideClientState state) {
        double floorRadius = state.ringFourRadius * INRADIUS_FACTOR;
        double jump = state.jumpProgress();
        if (jump >= 0.0) {
            // Out to the axis and back on a cosine: radius = R * (1 + cos 2*pi*t) / 2.
            return floorRadius * (1.0 + Math.cos(2.0 * Math.PI * jump)) / 2.0;
        }
        if (state.drifting) {
            double elapsed = (System.nanoTime() - state.driftStartNanos) / 1_000_000_000.0;
            double t = Math.max(0.0, Math.min(1.0, elapsed / state.driftSeconds));
            return floorRadius * (1.0 - t);
        }
        return floorRadius;
    }

    /**
     * The player in elytra flight, centred {@code radius} below the axis at the rider plane, rolled
     * by {@code bankDegrees} about the depth axis through its own centre (+ = counter-clockwise).
     */
    @SuppressWarnings("java:S107")
    private static void drawRider(PoseStack pose, MultiBufferSource.BufferSource buffers, WarpRideRiderProxy rider,
                                  double radius, double sizeFactor, double bankDegrees, float partialTick,
                                  WarpRideRiderEffects effects) {
        float scale = (float) (RIDER_PX_PER_BLOCK * sizeFactor);
        if (scale <= 0.0f) {
            return;
        }
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        boolean hitBoxes = dispatcher.shouldRenderHitBoxes();
        dispatcher.setRenderShadow(false);
        dispatcher.setRenderHitBoxes(false);
        pose.pushPose();
        pose.translate(0.0f, (float) -radius, WarpRideScene3D.zFor(1.0));
        if (bankDegrees != 0.0) {
            pose.mulPose(Axis.ZP.rotationDegrees((float) bankDegrees));
        }
        pose.scale(scale, scale, scale);
        pose.translate(0.0f, RIDER_CENTER_Y, RIDER_CENTER_Z);
        if (effects == null) {
            dispatcher.render(rider, 0.0, 0.0, 0.0, 0.0f, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
        } else {
            // Everything queued so far is farther than the rider: draw it first, then the rider
            // through its own buffers, where tint and glint are layered on (plan 191, E1).
            buffers.endBatch();
            dispatcher.render(rider, 0.0, 0.0, 0.0, 0.0f, partialTick, pose, effects.buffers(),
                    LightTexture.FULL_BRIGHT);
            effects.flush();
        }
        pose.popPose();
        dispatcher.setRenderHitBoxes(hitBoxes);
        dispatcher.setRenderShadow(true);
    }

    // --- portal-entry warp --------------------------------------------------------------------------

    /**
     * The world-frozen warp: missed objects still flying past, the taken portal shrinking to
     * {@code shrink} at the axis with its rank strip, then the rider at {@code riderRadius}, scaled
     * by {@code shrink} as well.
     */
    @SuppressWarnings("java:S107")
    public static void renderWarpScene(GuiGraphics graphics, WarpRideClientState state, WarpRideRiderProxy rider,
                                       double shrink, double riderRadius, float partialTick,
                                       WarpRideRiderEffects effects) {
        MultiBufferSource.BufferSource buffers = beginScene(graphics);
        PoseStack pose = new PoseStack();
        drawVortex(pose, buffers, state, state.resultTypeId, 1.0, shrink, 1.0f);
        endScene(buffers);

        beginAlphaBlit();
        drawPortalRank(graphics, state.resultRank, (int) Math.round(PORTAL_SPRITE_SIZE * shrink), 1.0);
        RenderSystem.disableBlend();

        // The rider and the objects flying past are both nearer than the portal and its strip.
        buffers = beginScene(graphics);
        if (rider != null && shrink > 0.0) {
            drawRider(pose, buffers, rider, riderRadius, shrink, state.bankDegrees(), partialTick, effects);
        }
        renderPastObjects(pose, buffers, state);
        endScene(buffers);
    }

    /** One frame of portal_wrap, played once over the shrink; {@code warpProgress} runs 0..1. */
    public static void renderWarpOverlay(GuiGraphics graphics, double warpProgress) {
        int frame = (int) Math.floor(Math.max(0.0, Math.min(1.0, warpProgress)) * PORTAL_WRAP_FRAMES);
        if (frame >= PORTAL_WRAP_FRAMES) {
            frame = PORTAL_WRAP_FRAMES - 1;
        }
        beginAlphaBlit();
        drawSpriteFrame(graphics, PORTAL_WRAP, CENTER_X, CENTER_Y, PORTAL_SPRITE_SIZE, 1.0, frame,
                PORTAL_WRAP_FRAMES);
        RenderSystem.disableBlend();
    }

    // --- energy aura ------------------------------------------------------------------------------

    /**
     * The flat positive strip as the energy aura, in front of the rider (plan 189, section 9.2):
     * tinted, then optionally brightened by an additive second pass of the same frame.
     */
    public static void drawAura(GuiGraphics graphics, int centerY, int size, float r, float g, float b,
                                double additiveWhite) {
        if (size <= 0) {
            return;
        }
        int frame = loopFrame(OBJECT_ANIM_FRAMES);
        drawSprite(graphics, POSITIVE, CENTER_X, centerY, size, packRgb(r, g, b), 1.0, frame, OBJECT_ANIM_FRAMES);
        if (additiveWhite > 0.0) {
            // drawSprite arms the default blend; re-arm additive by hand for the brightening pass.
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            graphics.pose().pushPose();
            graphics.pose().translate(CENTER_X, centerY, 0.0f);
            graphics.setColor(1.0f, 1.0f, 1.0f, (float) Math.min(1.0, additiveWhite));
            graphics.blit(POSITIVE, -size / 2, -size / 2, 0.0f, (float) (frame * size),
                    size, size, size, size * OBJECT_ANIM_FRAMES);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.pose().popPose();
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableBlend();
    }

    private static int packRgb(float r, float g, float b) {
        return ((int) (r * 255.0f) << 16) | ((int) (g * 255.0f) << 8) | (int) (b * 255.0f);
    }

    // --- sprites ----------------------------------------------------------------------------------

    /** One frame of a vertical animation strip, untinted and unrotated. */
    private static void drawSpriteFrame(GuiGraphics graphics, ResourceLocation texture,
                                        int centerX, int centerY, int size, double alpha,
                                        int frame, int frameCount) {
        drawSprite(graphics, texture, centerX, centerY, size, 0xFFFFFFFF, alpha, frame, frameCount);
    }

    /** Texture height is declared as {@code size * frameCount} so the v-range covers one frame. */
    private static void drawSprite(GuiGraphics graphics, ResourceLocation texture,
                                   int centerX, int centerY, int size, int tint, double alpha,
                                   int frame, int frameCount) {
        if (size <= 0) {
            return;
        }
        float r = ((tint >> 16) & 0xFF) / 255.0f;
        float g = ((tint >> 8) & 0xFF) / 255.0f;
        float b = (tint & 0xFF) / 255.0f;

        beginAlphaBlit();
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0f);
        graphics.setColor(r, g, b, (float) Math.max(0.0, Math.min(1.0, alpha)));
        graphics.blit(texture, -size / 2, -size / 2, 0.0f, (float) (frame * size),
                size, size, size, size * frameCount);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.pose().popPose();
    }

}
