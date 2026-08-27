package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Draws the tunnel, its objects and the rider, in 1920x1080 virtual-canvas coordinates. */
public final class WarpRideRenderer {

    public static final int CENTER_X = 960;
    public static final int CENTER_Y = 540;
    /** cos(22.5°): the octagon's flat face, which is the lane floor the rider sits on. */
    private static final double INRADIUS_FACTOR = 0.9238795325;

    public static final int LANE_COUNT = 8;

    private static final int GRID_COLOR = 0xB8A7DD;

    private static final int OBJECT_SIZE = 224;
    public static final int PORTAL_SIZE = 512;
    private static final int PLAYER_SIZE = 192;

    private static final String BASE = "textures/gui/warpride/";

    // Lane objects are 4-frame vertical strips, looping on the same animation clock as the portals.
    public static final ResourceLocation POSITIVE = tex("positive");
    public static final ResourceLocation NEGATIVE = tex("negative");
    public static final ResourceLocation PLAYER = tex("player");

    // Portals are layered: a tinted base, an untinted 6-frame overlay, and for ranks 1-3 a rank
    // strip. portal_wrap is a separate 8-frame strip played once over the portal-entry warp.
    private static final ResourceLocation PORTAL_TINT = tex("portal_tint");
    private static final ResourceLocation PORTAL_TOP = tex("portal_top");
    private static final ResourceLocation PORTAL_WRAP = tex("portal_wrap");
    private static final ResourceLocation[] PORTAL_RANKS = {
            null, tex("portal_rank1"), tex("portal_rank2"), tex("portal_rank3")
    };

    private static final int PORTAL_ANIM_FRAMES = 6;
    private static final int OBJECT_ANIM_FRAMES = 4;
    /** ~12.5 fps: a 6-frame loop in 480 ms. */
    private static final long PORTAL_ANIM_FRAME_NANOS = 80_000_000L;
    private static final int PORTAL_WRAP_FRAMES = 8;

    private static boolean filtersApplied;

    private static int viewLeft;
    private static int viewTop;
    private static int viewRight = 1920;
    private static int viewBottom = 1080;

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
        List<ResourceLocation> mipmapped = new ArrayList<>(List.of(
                PLAYER, PORTAL_TINT, WarpRideBackground.BACKDROP, WarpRideBackground.ROCK,
                WarpRideHud.BUZZ, WarpRideHud.BAR_TEXTURE,
                WarpRideHud.KEY_CLOCKWISE, WarpRideHud.KEY_JUMP, WarpRideHud.KEY_COUNTER_CLOCKWISE));
        for (ResourceLocation id : mipmapped) {
            setFilter(id, true);
        }
        // Strips get linear only: mipmapping a vertical strip averages adjacent frames together.
        List<ResourceLocation> strips = new ArrayList<>(List.of(
                POSITIVE, NEGATIVE, PORTAL_TOP, PORTAL_WRAP));
        for (ResourceLocation id : PORTAL_RANKS) {
            if (id != null) {
                strips.add(id);
            }
        }
        for (ResourceLocation id : strips) {
            setFilter(id, false);
        }
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

    /** Blending is disabled by every fill/drawString flush, so this must precede every single blit. */
    public static void beginAlphaBlit() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static int loopFrame(int frameCount) {
        return (int) ((System.nanoTime() / PORTAL_ANIM_FRAME_NANOS) % frameCount);
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

    /** 0 % at spawn, 50 % at ring 1, 100 % from ring 2, back to 0 % at the fifth plane. */
    public static double opacity(WarpRideClientState state, double depth) {
        double spawn = state.visibleDepthLy;                 // 32
        double ring1 = ringDepth(state, 1);                  // 24
        double ring2 = ringDepth(state, 2);                  // 16

        if (depth >= spawn) {
            return 0.0;
        }
        if (depth >= ring1) {
            return 0.5 * ((spawn - depth) / (spawn - ring1));
        }
        if (depth >= ring2) {
            return 0.5 + 0.5 * ((ring1 - depth) / (ring1 - ring2));
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

    /** How far the whole tunnel is swung, in degrees. Everything drawn radially shares this. */
    private static double tunnelRotationDeg(WarpRideClientState state) {
        return -45.0 * state.displayLane;
    }

    /** Depth of one of the decorative octagons, ring 1 being the farthest. */
    private static double ringDepth(WarpRideClientState state, int ring) {
        return state.visibleDepthLy * (state.ringCount - ring) / (double) state.ringCount;
    }

    /** The octagon is geometry, not a texture. The rings are decoration and mark no wave slot. */
    public static void renderTunnel(GuiGraphics graphics, WarpRideClientState state) {
        double offset = tunnelRotationDeg(state);
        for (int ring = 1; ring <= state.ringCount; ring++) {
            double radius = state.ringFourRadius * depthScale(state, ringDepth(state, ring));
            float alpha = ring == state.ringCount ? 0.85f : 0.30f + 0.12f * ring;
            drawOctagon(graphics, radius, packAlpha(GRID_COLOR, alpha), Math.max(1, ring), offset);
        }
        double outer = state.ringFourRadius;
        double inner = state.ringFourRadius * depthScale(state, ringDepth(state, 1));
        for (int i = 0; i < LANE_COUNT; i++) {
            double angle = 90.0 + 45.0 * i + 22.5 + offset;
            drawRadial(graphics, angle, inner, outer, packAlpha(GRID_COLOR, 0.35f), 2);
        }
    }

    private static void drawOctagon(GuiGraphics graphics, double radius, int argb, int thickness,
                                    double offsetDeg) {
        double side = 2.0 * radius * Math.sin(Math.PI / LANE_COUNT);
        double apothem = radius * INRADIUS_FACTOR;
        for (int i = 0; i < LANE_COUNT; i++) {
            double angle = 90.0 + 45.0 * i + offsetDeg;
            graphics.pose().pushPose();
            graphics.pose().translate(CENTER_X, CENTER_Y, 0.0f);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) angle));
            int half = (int) Math.round(side / 2.0);
            int a = (int) Math.round(apothem);
            graphics.fill(a - thickness, -half, a, half, argb);
            graphics.pose().popPose();
        }
    }

    private static void drawRadial(GuiGraphics graphics, double angleDeg, double from, double to,
                                   int argb, int thickness) {
        graphics.pose().pushPose();
        graphics.pose().translate(CENTER_X, CENTER_Y, 0.0f);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) angleDeg));
        graphics.fill((int) Math.round(from), -thickness / 2 - 1,
                (int) Math.round(to), -thickness / 2 - 1 + thickness, argb);
        graphics.pose().popPose();
    }

    /** Draws objects on one side of the z-order flip. Portals are exempt and always draw first. */
    public static void renderObjects(GuiGraphics graphics, WarpRideClientState state, boolean past) {
        beginAlphaBlit();
        long now = System.nanoTime();
        // Painter's algorithm, and the list is nearest-first: walking it forwards would draw the
        // most distant wave last and therefore on top.
        List<WarpRideClientState.ClientWave> waves = state.waves();
        for (int i = waves.size() - 1; i >= 0; i--) {
            WarpRideClientState.ClientWave wave = waves.get(i);
            double waveDepth = wave.depth(state.odometer);
            for (WarpRideClientState.ClientObject object : wave.objects) {
                // A parked portal uses its own hover/fade depth, decoupled from the odometer.
                double depth = object.isPortal() && wave.portalCrossNanos != 0
                        ? wave.portalDepth(now, state.fifthPlaneDepth)
                        : waveDepth;
                boolean isPast = !object.isPortal() && depth <= 0.0;
                if (isPast != past) {
                    continue;
                }
                // Only *missed* lane objects fly past; a hit one vanished at the plane.
                if (isPast && wave.consumedLanes.contains(object.lane())) {
                    continue;
                }
                renderObject(graphics, state, object, depth);
            }
        }
        RenderSystem.disableBlend();
    }

    private static void renderObject(GuiGraphics graphics, WarpRideClientState state,
                                     WarpRideClientState.ClientObject object, double depth) {
        double alpha = opacity(state, depth);
        if (alpha <= 0.01) {
            return;
        }
        double scale = depthScale(state, depth);

        if (object.isPortal()) {
            // The taken portal is drawn by the warp sequence, not here.
            if (state.phase == WarpRideClientState.PHASE_PORTAL_TAKEN) {
                return;
            }
            int size = (int) Math.round(PORTAL_SIZE * scale);
            drawPortal(graphics, state, object.rank(), object.typeId(), CENTER_X, CENTER_Y, size, alpha);
            return;
        }

        double radius;
        if (depth >= 0.0) {
            radius = state.ringFourRadius * INRADIUS_FACTOR * scale;
        } else {
            double t = flyPastProgress(state, depth);
            radius = state.ringFourRadius * INRADIUS_FACTOR * (1.0 + 1.5 * t);
        }
        double angleDeg = laneAngleDeg(object.lane(), state);
        double angle = Math.toRadians(angleDeg);
        int x = CENTER_X + (int) Math.round(Math.cos(angle) * radius);
        int y = CENTER_Y + (int) Math.round(Math.sin(angle) * radius);

        ResourceLocation texture = object.kind() == 0 ? POSITIVE : NEGATIVE;
        // The sprite's top points at the axis; a texture's up is 270° here, hence angle - 90°.
        drawSprite(graphics, texture, x, y, (int) Math.round(OBJECT_SIZE * scale),
                0xFFFFFFFF, alpha, angleDeg - 90.0, loopFrame(OBJECT_ANIM_FRAMES), OBJECT_ANIM_FRAMES);
    }

    /** The rider's sprite *centre* sits on the octagon's flat face, the anchor lane objects use. */
    public static void renderPlayer(GuiGraphics graphics, WarpRideClientState state) {
        double floorRadius = state.ringFourRadius * INRADIUS_FACTOR;
        double radius = floorRadius;

        double jump = state.jumpProgress();
        if (jump >= 0.0) {
            // Out to the axis and back on a cosine: radius = R * (1 + cos 2*pi*t) / 2.
            radius = floorRadius * (1.0 + Math.cos(2.0 * Math.PI * jump)) / 2.0;
        } else if (state.drifting) {
            double elapsed = (System.nanoTime() - state.driftStartNanos) / 1_000_000_000.0;
            double t = Math.max(0.0, Math.min(1.0, elapsed / state.driftSeconds));
            radius = floorRadius * (1.0 - t);
        }

        int centerY = CENTER_Y + (int) Math.round(radius);
        beginAlphaBlit();
        drawSprite(graphics, PLAYER, CENTER_X, centerY, PLAYER_SIZE, 0xFFFFFFFF, 1.0, 0.0);
        RenderSystem.disableBlend();
    }

    /** The layered portal: tinted base, 6-frame overlay, and for ranks 1-3 a 6-frame rank strip. */
    public static void drawPortal(GuiGraphics graphics, WarpRideClientState state, int rank, int typeId,
                                  int cx, int cy, int size, double alpha) {
        if (size <= 0) {
            return;
        }
        int tint = state.tintFor(typeId);
        int frame = loopFrame(PORTAL_ANIM_FRAMES);
        drawSprite(graphics, PORTAL_TINT, cx, cy, size, tint, alpha, 0.0);
        drawSpriteFrame(graphics, PORTAL_TOP, cx, cy, size, alpha, frame, PORTAL_ANIM_FRAMES);
        int r = Math.max(0, Math.min(PORTAL_RANKS.length - 1, rank));
        ResourceLocation rankStrip = PORTAL_RANKS[r];
        if (rankStrip != null) {
            drawSpriteFrame(graphics, rankStrip, cx, cy, size, alpha, frame, PORTAL_ANIM_FRAMES);
        }
    }

    /** The rider sprite shrinking at the axis during the warp. */
    public static void renderWarpPlayer(GuiGraphics graphics, double shrink) {
        int size = (int) Math.round(PLAYER_SIZE * Math.max(0.0, shrink));
        if (size <= 0) {
            return;
        }
        beginAlphaBlit();
        drawSprite(graphics, PLAYER, CENTER_X, CENTER_Y, size, 0xFFFFFFFF, 1.0, 0.0);
        RenderSystem.disableBlend();
    }

    /** One frame of portal_wrap, played once over the shrink; {@code warpProgress} runs 0..1. */
    public static void renderWarpOverlay(GuiGraphics graphics, double warpProgress) {
        int frame = (int) Math.floor(Math.max(0.0, Math.min(1.0, warpProgress)) * PORTAL_WRAP_FRAMES);
        if (frame >= PORTAL_WRAP_FRAMES) {
            frame = PORTAL_WRAP_FRAMES - 1;
        }
        beginAlphaBlit();
        drawSpriteFrame(graphics, PORTAL_WRAP, CENTER_X, CENTER_Y, PORTAL_SIZE, 1.0, frame, PORTAL_WRAP_FRAMES);
        RenderSystem.disableBlend();
    }

    /** A single-frame sprite, centred, optionally tinted and rotated. */
    private static void drawSprite(GuiGraphics graphics, ResourceLocation texture,
                                   int centerX, int centerY, int size, int tint, double alpha,
                                   double rotationDeg) {
        drawSprite(graphics, texture, centerX, centerY, size, tint, alpha, rotationDeg, 0, 1);
    }

    /** One frame of a vertical animation strip, untinted and unrotated. */
    private static void drawSpriteFrame(GuiGraphics graphics, ResourceLocation texture,
                                        int centerX, int centerY, int size, double alpha,
                                        int frame, int frameCount) {
        drawSprite(graphics, texture, centerX, centerY, size, 0xFFFFFFFF, alpha, 0.0, frame, frameCount);
    }

    /** Texture height is declared as {@code size * frameCount} so the v-range covers one frame. */
    private static void drawSprite(GuiGraphics graphics, ResourceLocation texture,
                                   int centerX, int centerY, int size, int tint, double alpha,
                                   double rotationDeg, int frame, int frameCount) {
        if (size <= 0) {
            return;
        }
        float r = ((tint >> 16) & 0xFF) / 255.0f;
        float g = ((tint >> 8) & 0xFF) / 255.0f;
        float b = (tint & 0xFF) / 255.0f;

        beginAlphaBlit();
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0f);
        if (rotationDeg != 0.0) {
            graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) rotationDeg));
        }
        graphics.setColor(r, g, b, (float) Math.max(0.0, Math.min(1.0, alpha)));
        graphics.blit(texture, -size / 2, -size / 2, 0.0f, (float) (frame * size),
                size, size, size, size * frameCount);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.pose().popPose();
    }

    private static int packAlpha(int rgb, float alpha) {
        int a = (int) (Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
