package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
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

/** The Ultra Warp Ride screen: the GUI scale is undone, then a fixed 1920x1080 canvas is fitted. */
public class UltraWarpRideScreen extends Screen {

    private final WarpRideClientState state = new WarpRideClientState();
    private final WarpRideBackground background = new WarpRideBackground();
    private final WarpRideHud hud = new WarpRideHud();
    private final WarpRidePauseOverlay pause = new WarpRidePauseOverlay();

    private static final java.util.random.RandomGenerator RANDOM =
            java.util.concurrent.ThreadLocalRandom.current();

    private boolean showTutorial = true;
    private int countdownStep = -1;
    private long countdownShownUntil;
    private boolean readySent;
    private boolean closeSent;
    private long lastFrameNanos;

    /** Last computed transform, so mouse coordinates can be mapped back into canvas units. */
    private double lastFit = 1.0;
    private double lastOffsetX;
    private double lastOffsetY;
    private double lastGuiScale = 1.0;

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
                playSound(ModSounds.WARPRIDE_POSITIVE);
            }
            case WarpRidePayloads.EVENT_NEGATIVE -> {
                hud.onNegative();
                state.consumeObject(payload.waveIndex(), payload.a());
                playSound(ModSounds.WARPRIDE_NEGATIVE);
            }
            case WarpRidePayloads.EVENT_DRIFT -> playSound(ModSounds.WARPRIDE_WARNING);
            case WarpRidePayloads.EVENT_PANIC -> {
                // The energy bar draining ten times faster with no explanation reads as a bug, so
                // this warning has to be unmissable (action plan 183, section 8.2).
                panicWarningUntil = System.nanoTime() + 4_000_000_000L;
                playSound(ModSounds.WARPRIDE_WARNING);
            }
            case WarpRidePayloads.EVENT_COUNTDOWN -> {
                countdownStep = payload.a();
                countdownShownUntil = System.nanoTime() + 950_000_000L;
                playSound(ModSounds.WARPRIDE_COUNTDOWN);
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
        state.applyTints(pendingTints);
        background.init(state);
        hud.setLabels(readKeyLabels());
        lastFrameNanos = System.nanoTime();
    }

    private WarpRideHud.WarpRideHudLabels readKeyLabels() {
        var options = Minecraft.getInstance().options;
        return new WarpRideHud.WarpRideHudLabels(
                labelOf(options.keyLeft, "A"),
                labelOf(options.keyUp, "W"),
                labelOf(options.keyRight, "D"));
    }

    private static String labelOf(KeyMapping mapping, String fallback) {
        if (mapping == null || mapping.isUnbound()) {
            return fallback;
        }
        return mapping.getTranslatedKeyMessage().getString();
    }

    @Override
    public boolean isPauseScreen() {
        // NOT a constant: returning the overlay's state is what pauses single-player but not multiplayer.
        return pause.isOpen();
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
            pause.toggle();
            return true;
        }

        if (showTutorial) {
            // Any key dismisses the placeholder tutorial page.
            dismissTutorial();
            return true;
        }
        if (pause.isOpen()) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pause.isOpen()) {
            double[] canvas = toCanvas(mouseX, mouseY);
            int result = pause.hit(canvas[0], canvas[1], state.canvasWidth, state.canvasHeight);
            if (result == 1) {
                pause.close();
            } else if (result == 2) {
                onClose();
            }
            return true;
        }
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
        Services.PLATFORM.sendPayloadToServer(new WarpRidePayloads.ReadyPayload(state.sessionId));
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
        if (input == WarpRidePayloads.INPUT_JUMP) {
            state.startLocalCooldown(state.jumpTransitMs);
            state.startLocalJump();
            // Taking a portal sends the rider into the axis, so it neither swaps lane nor spins.
            if (!takingPortal) {
                state.lane = state.lane ^ 4;
                state.aimAtJump(RANDOM.nextBoolean(), state.jumpTransitMs);
            }
            playSound(ModSounds.WARPRIDE_JUMP);
        } else if (input == WarpRidePayloads.INPUT_LEFT) {
            state.startLocalCooldown(state.inputCooldownMs);
            state.lane = Math.floorMod(state.lane + 1, WarpRideRenderer.LANE_COUNT);
            state.aimAtLane(state.lane, state.laneRotationMs);
        } else {
            state.startLocalCooldown(state.inputCooldownMs);
            state.lane = Math.floorMod(state.lane - 1, WarpRideRenderer.LANE_COUNT);
            state.aimAtLane(state.lane, state.laneRotationMs);
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
        if (!warping && (!pause.isOpen() || !mc.isLocalServer())) {
            state.advance(delta);
            background.advance(state, delta);
        }

        double guiScale = mc.getWindow().getGuiScale();
        int pw = mc.getWindow().getWidth();
        int ph = mc.getWindow().getHeight();
        double fit = Math.min(pw / (double) state.canvasWidth, ph / (double) state.canvasHeight);
        double offsetX = (pw - state.canvasWidth * fit) / 2.0;
        double offsetY = (ph - state.canvasHeight * fit) / 2.0;

        this.lastGuiScale = guiScale;
        this.lastFit = fit;
        this.lastOffsetX = offsetX;
        this.lastOffsetY = offsetY;
        WarpRideRenderer.setViewport(fit, offsetX, offsetY, pw, ph);

        graphics.pose().pushPose();
        // 1. Undo the GUI scale: from here on the screen draws in physical pixels.
        graphics.pose().scale((float) (1.0 / guiScale), (float) (1.0 / guiScale), 1.0f);

        // Full framebuffer, before the canvas transform, so the background fills ultrawide too.
        background.renderBackdrop(graphics, pw, ph, state);

        graphics.pose().pushPose();
        graphics.pose().translate((float) offsetX, (float) offsetY, 0.0f);
        graphics.pose().scale((float) fit, (float) fit, 1.0f);
        background.renderProps(graphics, state, WarpRideRenderer.CENTER_X, WarpRideRenderer.CENTER_Y);
        graphics.pose().popPose();

        background.renderTube(graphics, pw, ph, state);       // colour tube, in front of the rocks
        background.renderStars(graphics, state, pw, ph);      // starfield, fills the whole screen

        // 2. Fit the fixed virtual canvas, uniformly and centred.
        graphics.pose().translate((float) offsetX, (float) offsetY, 0.0f);
        graphics.pose().scale((float) fit, (float) fit, 1.0f);

        WarpRideRenderer.renderTunnel(graphics, state);
        WarpRideRenderer.renderObjects(graphics, state, false);   // still ahead: behind the rider

        if (warping) {
            renderWarp(graphics, now);
        } else {
            WarpRideRenderer.renderPlayer(graphics, state);
            WarpRideRenderer.renderObjects(graphics, state, true);    // gone by: in front of the rider
            hud.render(graphics, this.font, state);
            renderCountdown(graphics);
            renderPanicWarning(graphics);
            if (showTutorial) {
                renderTutorial(graphics);
            }
        }
        pause.render(graphics, this.font, state.canvasWidth, state.canvasHeight);

        graphics.pose().popPose();
    }

    /** The portal-entry warp, world frozen: drift to the axis, shrink, fade to black, then recap. */
    private void renderWarp(GuiGraphics graphics, long now) {
        WarpRideRenderer.renderObjects(graphics, state, true);

        if (state.warpInDriftWait(now)) {
            // Blending is armed per blit now; this pair only restores what renderObjects left behind.
            WarpRideRenderer.beginAlphaBlit();
            WarpRideRenderer.drawPortal(graphics, state, state.resultRank, state.resultTypeId,
                    WarpRideRenderer.CENTER_X, WarpRideRenderer.CENTER_Y, WarpRideRenderer.PORTAL_SIZE, 1.0);
            RenderSystem.disableBlend();
            WarpRideRenderer.renderPlayer(graphics, state);   // still drifting to the axis
            return;
        }

        double warpP = state.warpProgress(now);
        if (warpP <= 1.0) {
            double shrink = 1.0 - warpP;
            int size = (int) Math.round(WarpRideRenderer.PORTAL_SIZE * shrink);
            WarpRideRenderer.beginAlphaBlit();
            WarpRideRenderer.drawPortal(graphics, state, state.resultRank, state.resultTypeId,
                    WarpRideRenderer.CENTER_X, WarpRideRenderer.CENTER_Y, size, 1.0);
            RenderSystem.disableBlend();
            WarpRideRenderer.renderWarpPlayer(graphics, shrink);
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

    private void renderCountdown(GuiGraphics graphics) {
        if (countdownStep < 0 || System.nanoTime() > countdownShownUntil) {
            return;
        }
        String text = countdownStep == 0
                ? Component.translatable("gui.cobblesafari.warpride.countdown.go").getString()
                : Integer.toString(countdownStep);
        centered(graphics, text, 470, 9.0f, 0xFFFFF0B0);
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

    /** Maps a GUI-scaled mouse position back into canvas units. */
    private double[] toCanvas(double mouseX, double mouseY) {
        double px = mouseX * lastGuiScale;
        double py = mouseY * lastGuiScale;
        return new double[]{(px - lastOffsetX) / lastFit, (py - lastOffsetY) / lastFit};
    }
}
