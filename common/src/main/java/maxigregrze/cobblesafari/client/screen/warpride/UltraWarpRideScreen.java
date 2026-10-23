package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.platform.InputConstants;
import maxigregrze.cobblesafari.init.ModSounds;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.warpride.network.WarpRidePayloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

/**
 * The Ultra Warp Ride screen: the GUI scale is undone, then a fixed 1920x1080 canvas is fitted. The
 * rocks, portals, objects and rider are 3D models drawn in perspective passes (action plan 189);
 * the start tunnel, the tube and the space behind them are procedural, and an intro with its own
 * camera replaces the countdown (action plan 191).
 */
public class UltraWarpRideScreen extends Screen {

    private final WarpRideClientState state = new WarpRideClientState();
    private final WarpRideBackground background = new WarpRideBackground();
    private final WarpRideHud hud = new WarpRideHud();
    private final WarpRideExitPrompt exitPrompt = new WarpRideExitPrompt();
    private final WarpRideCamera camera = new WarpRideCamera();
    private final WarpRideStartTunnel startTunnel = new WarpRideStartTunnel();
    /** Set once the rider has left the start tunnel far enough behind that it draws nothing. */
    private boolean startTunnelPassed;
    /** Tint and glint on the rider; null once the screen has released its buffers. */
    private WarpRideRiderEffects riderEffects = new WarpRideRiderEffects();
    /** The local player's look-alike in elytra flight; null only if there is no level or player. */
    private WarpRideRiderProxy rider;

    private static final java.util.random.RandomGenerator RANDOM =
            java.util.concurrent.ThreadLocalRandom.current();

    private boolean showTutorial = true;
    private boolean readySent;
    /** When the last ready went out, for the resend below. */
    private long readySentNanos;
    private boolean closeSent;
    private long lastFrameNanos;

    public UltraWarpRideScreen() {
        super(Component.translatable("gui.cobblesafari.warpride.tutorial.title"));
    }

    public static void open(WarpRidePayloads.OpenPayload payload) {
        UltraWarpRideScreen screen = new UltraWarpRideScreen();
        screen.state.applyOpen(payload);
        screen.showTutorial = payload.showTutorial();
        Minecraft.getInstance().setScreen(screen);
    }

    public static UltraWarpRideScreen active() {
        return Minecraft.getInstance().screen instanceof UltraWarpRideScreen s ? s : null;
    }

    /** Tints arrive independently of any session, so they are cached even with no screen open. */
    private static java.util.List<WarpRidePayloads.TintData> pendingTints = java.util.List.of();

    /** Wall-clock deadline for the panic banner; 0 when the parent clock has not panicked. */
    private long panicWarningUntil;

    /** True once the server says a sub-dungeon is generating: hold black, never show a result. */
    private boolean handoffPending;

    public static void acceptTints(java.util.List<WarpRidePayloads.TintData> tints) {
        pendingTints = tints;
        // The exit portal is tinted outside any ride, so the table also lives on its own.
        maxigregrze.cobblesafari.client.renderer.WarpRideClientTints.accept(tints);
        UltraWarpRideScreen screen = active();
        if (screen != null) {
            screen.state.applyTints(tints);
        }
    }

    public void acceptState(WarpRidePayloads.StatePayload payload) {
        state.applyState(payload);
    }

    public void acceptWaves(WarpRidePayloads.WavesPayload payload) {
        state.applyWaves(payload);
    }

    public void acceptEvent(WarpRidePayloads.EventPayload payload) {
        switch (payload.event()) {
            case WarpRidePayloads.EVENT_POSITIVE -> {
                hud.onPositive();
                state.consumeObject(payload.waveIndex(), payload.a());
                // b = 1: this bonus raised the energy ceiling, the source's "maxup" aura.
                if (riderEffects != null) {
                    riderEffects.onPositive(payload.b() == 1);
                }
                camera.onPositive();
                playSound(ModSounds.WARPRIDE_POSITIVE);
            }
            case WarpRidePayloads.EVENT_NEGATIVE -> {
                hud.onNegative();
                state.consumeObject(payload.waveIndex(), payload.a());
                if (riderEffects != null) {
                    riderEffects.onNegative();
                }
                camera.onNegative();
                playSound(ModSounds.WARPRIDE_NEGATIVE);
            }
            case WarpRidePayloads.EVENT_PANIC ->
                    // The energy bar draining ten times faster with no explanation reads as a bug, so
                    // this warning has to be unmissable (action plan 183, section 8.2). Silent since
                    // plan 191: the source has no such sound.
                    panicWarningUntil = System.nanoTime() + 4_000_000_000L;
            case WarpRidePayloads.EVENT_LAUNCH -> {
                state.onLaunch();
                if (riderEffects != null) {
                    riderEffects.onLaunch();
                }
                playSound(ModSounds.WARPRIDE_LAUNCH);
            }
            case WarpRidePayloads.EVENT_HANDOFF -> handoffPending = true;
            case WarpRidePayloads.EVENT_PORTAL_TAKEN -> {
                state.onPortalTaken(payload.a(), payload.b(), (int) payload.value());
                playSound(ModSounds.WARPRIDE_PORTAL);
            }
            case WarpRidePayloads.EVENT_RIDE_ENDED -> {
                // END_REASON_HANDOFF means the server already teleported us into the sub-dungeon
                // while the screen held on black: close outright rather than showing a result the
                // player is no longer anywhere near (W6 - the server, never the client, decides).
                if (payload.a() == WarpRidePayloads.END_REASON_HANDOFF) {
                    onClose();
                } else {
                    state.phase = WarpRideClientState.PHASE_ENDED;
                }
            }
            default -> {
                // Unknown event ids are ignored: a newer server must not break an older client.
            }
        }
    }

    private void playSound(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(sound, 1.0f, 0.8f));
    }

    @Override
    protected void init() {
        super.init();
        WarpRideRenderer.applyFilters();
        WarpRideRenderer.cacheModels();
        // Built here, behind the tutorial page, rather than as a hitch on the first riding frame.
        WarpRideCloudTextures.ensure();
        state.applyTints(pendingTints);
        background.init(state);
        Minecraft mc = Minecraft.getInstance();
        if (rider == null && mc.level != null && mc.player != null) {
            rider = new WarpRideRiderProxy(mc.level, mc.player);
        }
        lastFrameNanos = System.nanoTime();
    }

    /** The ride never pauses, single-player included: Esc only arms the exit prompt (plan 189). */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        sendClose();
        super.onClose();
    }

    @Override
    public void removed() {
        sendClose();
        if (riderEffects != null) {
            riderEffects.close();
            riderEffects = null;
        }
        WarpRideScene3D.setView(null);
        super.removed();
    }

    private void sendClose() {
        if (closeSent || state.sessionId == null) {
            return;
        }
        closeSent = true;
        Services.PLATFORM.sendPayloadToServer(new WarpRidePayloads.ClosePayload(state.sessionId));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onEscape();
            return true;
        }

        if (showTutorial) {
            // Any key but Esc dismisses the placeholder tutorial page.
            dismissTutorial();
            return true;
        }

        var options = Minecraft.getInstance().options;
        if (matches(options.keyLeft, keyCode, scanCode)) {
            sendInput(WarpRidePayloads.INPUT_LEFT);
            return true;
        }
        if (matches(options.keyRight, keyCode, scanCode)) {
            sendInput(WarpRidePayloads.INPUT_RIGHT);
            return true;
        }
        if (matches(options.keyUp, keyCode, scanCode)) {
            sendInput(WarpRidePayloads.INPUT_JUMP);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Esc twice within three seconds leaves without saving; the attempt stays spent, because closing
     * ends the session aborted on the server. Ignored while the warp or the hand-off hold is running,
     * so a taken portal always completes. On the result screen, one Esc closes.
     */
    private void onEscape() {
        if (handoffPending || state.phase == WarpRideClientState.PHASE_PORTAL_TAKEN) {
            return;
        }
        if (state.phase == WarpRideClientState.PHASE_ENDED || exitPrompt.onEscape(System.nanoTime())) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showTutorial) {
            dismissTutorial();
            return true;
        }
        // A movement key can be bound to a mouse button; the ride has to honour that too.
        var options = Minecraft.getInstance().options;
        if (matchesMouse(options.keyLeft, button)) {
            sendInput(WarpRidePayloads.INPUT_LEFT);
            return true;
        }
        if (matchesMouse(options.keyRight, button)) {
            sendInput(WarpRidePayloads.INPUT_RIGHT);
            return true;
        }
        if (matchesMouse(options.keyUp, button)) {
            sendInput(WarpRidePayloads.INPUT_JUMP);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** {@code KeyMapping#isDown()} is not updated while a Screen is open, so compare raw key codes. */
    private static boolean matches(KeyMapping mapping, int keyCode, int scanCode) {
        if (mapping == null || mapping.isUnbound()) {
            return false;
        }
        return mapping.matches(keyCode, scanCode);
    }

    private static boolean matchesMouse(KeyMapping mapping, int button) {
        if (mapping == null || mapping.isUnbound()) {
            return false;
        }
        return mapping.matchesMouse(button);
    }

    private void dismissTutorial() {
        if (readySent || state.sessionId == null) {
            return;
        }
        showTutorial = false;
        readySent = true;
        readySentNanos = System.nanoTime();
        Services.PLATFORM.sendPayloadToServer(new WarpRidePayloads.ReadyPayload(state.sessionId));
    }

    /** How long a ready may go unanswered before it is sent again. */
    private static final long READY_RESEND_NANOS = 1_000_000_000L;

    /**
     * Resends the ready while the server still reports the tutorial phase (action plan 190, P4).
     *
     * <p>The ready was sent exactly once, and anything that dropped it - a rate limit, a packet
     * arriving before the session existed server-side - left the ride on its tutorial page for good,
     * with the player able only to quit. The server re-checks the phase and accepts one ready per
     * session, so a resend is inert once the intro has started.</p>
     */
    @Override
    public void tick() {
        super.tick();
        if (readySent && state.sessionId != null
                && state.phase == WarpRideClientState.PHASE_AWAITING_READY
                && System.nanoTime() - readySentNanos >= READY_RESEND_NANOS) {
            readySentNanos = System.nanoTime();
            Services.PLATFORM.sendPayloadToServer(new WarpRidePayloads.ReadyPayload(state.sessionId));
        }
    }

    private void sendInput(int input) {
        if (state.sessionId == null || state.phase != WarpRideClientState.PHASE_RIDING
                || state.drifting) {
            return;
        }
        // A jump onto a hovering portal bypasses the steering cooldown, mirroring the server.
        boolean takingPortal = input == WarpRidePayloads.INPUT_JUMP && state.hasHoveringPortal();
        if (!takingPortal && state.isOnCooldown()) {
            return;
        }
        // Predict locally so the ride feels immediate; the next snapshot corrects the lane if wrong.
        // Every duration follows the predicted energy, as the server's lock does (plan 191, 3.3).
        if (input == WarpRidePayloads.INPUT_JUMP) {
            int jumpMs = state.jumpTransitMs();
            state.startLocalCooldown(jumpMs);
            state.startLocalJump();
            // Taking a portal sends the rider into the axis, so it neither swaps lane nor spins.
            if (!takingPortal) {
                state.lane = state.lane ^ 4;
                state.aimAtJump(RANDOM.nextBoolean(), jumpMs);
            }
        } else if (input == WarpRidePayloads.INPUT_LEFT) {
            state.startLocalCooldown(state.inputCooldownMs());
            state.lane = Math.floorMod(state.lane + 1, WarpRideRenderer.LANE_COUNT);
            state.aimAtLane(state.lane, state.laneRotationMs());
            state.startBank(1);
        } else {
            state.startLocalCooldown(state.inputCooldownMs());
            state.lane = Math.floorMod(state.lane - 1, WarpRideRenderer.LANE_COUNT);
            state.aimAtLane(state.lane, state.laneRotationMs());
            state.startBank(-1);
        }
        Services.PLATFORM.sendPayloadToServer(new WarpRidePayloads.InputPayload(
                state.sessionId, input, state.estimatedServerTick()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        long now = System.nanoTime();
        double delta = Math.min(0.1, (now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;
        boolean warping = state.phase == WarpRideClientState.PHASE_PORTAL_TAKEN;
        // The warp freezes the world; only sprite frame animations and the warp itself keep moving.
        if (!warping) {
            state.advance(delta);
            background.advance(state, delta);
        }
        camera.advance(delta);
        if (riderEffects != null) {
            riderEffects.update(now, state);
        }
        // The intro's orbit until it hands over, then the chase view with its feedback (plan 191, C1).
        org.joml.Matrix4f introView = WarpRideCamera.introView(state.introFrame(now),
                WarpRideRenderer.riderCenter(state));
        WarpRideScene3D.setView(introView != null ? introView : camera.rideView(now));
        // While the start tunnel is still in sight, the ride's world exists only beyond its exit.
        double exitDepth = startTunnelPassed
                ? Double.NEGATIVE_INFINITY
                : WarpRideStartTunnel.exitDepthLy(state.introSeconds(now), state.introLaunchTick / 20.0,
                        state.projectionK);

        double guiScale = mc.getWindow().getGuiScale();
        int pw = mc.getWindow().getWidth();
        int ph = mc.getWindow().getHeight();
        double fit = Math.min(pw / (double) state.canvasWidth, ph / (double) state.canvasHeight);
        double offsetX = (pw - state.canvasWidth * fit) / 2.0;
        double offsetY = (ph - state.canvasHeight * fit) / 2.0;

        WarpRideRenderer.setViewport(fit, offsetX, offsetY, pw, ph);
        if (rider != null && mc.player != null) {
            rider.syncFrom(mc.player, mc.gameRenderer.getMainCamera().getPosition());
        }

        graphics.pose().pushPose();
        // 1. Undo the GUI scale: from here on the screen draws in physical pixels.
        graphics.pose().scale((float) (1.0 / guiScale), (float) (1.0 / guiScale), 1.0f);

        // Full framebuffer, before the canvas transform, so the background fills ultrawide too.
        background.renderBackdrop(graphics, pw, ph, state);                 // procedural space
        background.renderStars(graphics, state, pw, ph, 1.0f);              // starfield, whole screen
        // The ride's world - rocks, cloud tube - is drawn first, cut at the start tunnel's exit; the
        // tunnel then paints over it, its opaque walls hiding all but what shows through the open
        // end. At the launch the rider flies out into a tube that was already there, streaming at
        // the tunnel's pace (WarpRideClientState#decorOdometer), and the tunnel falls behind.
        background.renderRocks(graphics, state, exitDepth);   // 3D passes: the projection spans the window
        WarpRideRenderer.renderTube(graphics, state, exitDepth);
        if (!startTunnelPassed) {
            startTunnelPassed = !WarpRideRenderer.renderStartTunnel(graphics, startTunnel, state,
                    state.introSeconds(now), state.introLaunchTick / 20.0);
        }

        // 2. Fit the fixed virtual canvas, uniformly and centred.
        graphics.pose().translate((float) offsetX, (float) offsetY, 0.0f);
        graphics.pose().scale((float) fit, (float) fit, 1.0f);

        if (warping) {
            renderWarp(graphics, now, partialTick);
        } else {
            WarpRideRenderer.renderScene(graphics, state, rider, partialTick, camera, riderEffects);
            hud.render(graphics, state, camera, now);                          // aura, hit effect
            renderPanicWarning(graphics);
            if (showTutorial) {
                renderTutorial(graphics);
            }
        }
        exitPrompt.render(graphics, this.font, state.canvasWidth, now);

        graphics.pose().popPose();
    }

    /** The portal-entry warp, world frozen: drift to the axis, shrink, fade to black, then recap. */
    private void renderWarp(GuiGraphics graphics, long now, float partialTick) {
        if (state.warpInDriftWait(now)) {
            // Full-size portal; the rider is still drifting to the axis.
            WarpRideRenderer.renderWarpScene(graphics, state, rider, 1.0,
                    WarpRideRenderer.riderRadius(state), partialTick, riderEffects);
            return;
        }

        double warpP = state.warpProgress(now);
        if (warpP <= 1.0) {
            double shrink = 1.0 - warpP;
            WarpRideRenderer.renderWarpScene(graphics, state, rider, shrink, 0.0, partialTick, riderEffects);
            WarpRideRenderer.renderWarpOverlay(graphics, warpP);
            return;
        }

        double fadeP = state.fadeProgress(now);
        int alpha = (int) Math.round(Math.max(0.0, Math.min(1.0, fadeP)) * 255.0);
        WarpRideRenderer.fillViewport(graphics, alpha << 24);
        if (fadeP >= 1.0 && !handoffPending) {
            renderResult(graphics);
        }
        // handoffPending: stay on black. A black screen is the one frame that can be held for an
        // unknown duration without anyone noticing, which is exactly why the generation is allowed
        // to take as long as it takes. The server closes this screen when it is done.
    }

    /** The parent clock's panic banner. Deliberately loud: see the Decision at section 8.2. */
    private void renderPanicWarning(GuiGraphics graphics) {
        if (panicWarningUntil == 0L || System.nanoTime() > panicWarningUntil) {
            return;
        }
        centered(graphics, Component.translatable("cobblesafari.wormhole.panic_warning").getString(),
                240, 3.0f, 0xFFFF5555);
    }

    /** Placeholder page. The phase, its payload and the once-only guard are the real deliverable. */
    private void renderTutorial(GuiGraphics graphics) {
        WarpRideRenderer.fillViewport(graphics, 0xD8060412);
        centered(graphics, Component.translatable("gui.cobblesafari.warpride.tutorial.title").getString(),
                420, 4.0f, 0xFFBFA8FF);
        centered(graphics, Component.translatable("gui.cobblesafari.warpride.tutorial.start").getString(),
                600, 2.5f, 0xFFFFFFFF);
    }

    private void renderResult(GuiGraphics graphics) {
        WarpRideRenderer.fillViewport(graphics, 0xE0060412);
        centered(graphics, Component.translatable("gui.cobblesafari.warpride.result.title").getString(),
                420, 4.0f, 0xFFBFA8FF);
        String line = Component.translatable("gui.cobblesafari.warpride.result.line",
                Integer.toString(state.resultDistance),
                Integer.toString(state.resultTypeId),
                Integer.toString(state.resultRank)).getString();
        centered(graphics, line, 560, 2.5f, 0xFFFFFFFF);
    }

    private void centered(GuiGraphics graphics, String text, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(state.canvasWidth / 2.0f, y, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(this.font, text, -this.font.width(text) / 2, 0, color, true);
        graphics.pose().popPose();
    }

}
