package maxigregrze.cobblesafari.client.screen.rotomphone;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import com.mojang.blaze3d.systems.RenderSystem;
import maxigregrze.cobblesafari.network.WonderAppPayload;
import maxigregrze.cobblesafari.network.WonderAppResultPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RotomPhoneWonderScreen extends RotomPhoneBaseScreen {

    private enum SubScreen {
        LOADING,
        BEGIN,
        SELECT,
        TRADE,
        ERROR
    }

    private static final ResourceLocation TEX_LOGO = loc("wonder/rotomphone_gui_icon_wonder.png");
    private static final ResourceLocation TEX_DOUBLE = loc("rotomphone_gui_icon_double.png");
    private static final ResourceLocation TEX_EMPTY = loc("rotomphone_gui_icon_empty.png");
    private static final ResourceLocation TEX_BANNER_DEF = loc("wonder/rotomphone_gui_event_default.png");
    private static final ResourceLocation TEX_TRADEANIM = loc("wonder/rotomphone_gui_tradeanim.png");

    private static final float SCALED_TEXT_Y_OFFSET = -7f;
    private static final int PARTY_SLOT_SIZE = 32;
    private static final float PARTY_SLOT_BASE_SCALE = 3.5f;
    private static final float PARTY_SLOT_MODEL_SCALE = 5.5f;
    private static final float PARTY_SLOT_MODEL_Y_OFFSET = 4f;
    private static final float TRADE_SLOT_BASE_SCALE = 10.5f;
    private static final float TRADE_SLOT_MODEL_SCALE = 6f;
    private static final int TRADE_SLOT_SIZE = 114;
    private static final int TRADE_SLOT_CLIP_WIDTH = TRADE_SLOT_SIZE * 2;
    private static final int TRADE_SLOT_X = 117;
    private static final int TRADE_SLOT_Y = 11;
    private static final int ANIM_FRAME_COUNT = 14;
    private static final int ANIM_FRAME_MS = 100;
    private static final long ANIM_INITIAL_DELAY_MS = 800L;
    private static final long ANIM_PAUSE_MS = 500L;
    private static final long ANIM_TOTAL_MS = ANIM_INITIAL_DELAY_MS
            + (long) ANIM_FRAME_COUNT * ANIM_FRAME_MS
            + ANIM_PAUSE_MS
            + (long) ANIM_FRAME_COUNT * ANIM_FRAME_MS;

    private static final int[] PARTY_SLOT_X = {58, 98, 138, 178, 218, 258};
    private static final Stat[] TOOLTIP_STATS = {
            Stats.HP,
            Stats.ATTACK,
            Stats.DEFENCE,
            Stats.SPECIAL_ATTACK,
            Stats.SPECIAL_DEFENCE,
            Stats.SPEED
    };
    private static final String[] TOOLTIP_STAT_KEYS = {"hp", "atk", "def", "spatk", "spdef", "spd"};

    private SubScreen state = SubScreen.LOADING;
    private int ticketsRemaining;
    private long nextResetEpochSeconds;
    private boolean hasEvent;
    private String eventName = "";
    private String customBannerName = "";
    private int eventDaysLeft;
    private List<WonderAppResultPayload.EventPoolEntry> eventPool = List.of();

    private int selectedSlot = -1;
    private boolean selectedLocked;
    private long lockedAtMillis;

    private long tradeStartedAtMillis;
    private Pokemon offeredPokemon;
    private Pokemon receivedPokemon;
    private final FloatingState[] partyStates = new FloatingState[6];
    private FloatingState offeredState;
    private FloatingState receivedState;

    private String lastErrorKey = "";

    public RotomPhoneWonderScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode, boolean rotoGlide) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.wonder"), rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide);
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 6; i++) {
            partyStates[i] = new FloatingState();
        }
        Services.PLATFORM.sendPayloadToServer(new WonderAppPayload(WonderAppPayload.ACTION_REQUEST_STATE, 0));
    }

    public void applyServerSnapshot(WonderAppResultPayload p) {
        this.ticketsRemaining = p.ticketsRemaining();
        this.nextResetEpochSeconds = p.nextResetEpochSeconds();
        this.hasEvent = p.hasEvent();
        this.eventName = p.eventName() == null ? "" : p.eventName();
        this.customBannerName = p.customBannerName() == null ? "" : p.customBannerName();
        this.eventDaysLeft = p.eventDaysLeft();
        this.eventPool = p.eventPool() == null ? List.of() : p.eventPool();
        this.lastErrorKey = p.errorKey() == null ? "" : p.errorKey();

        switch (p.subscreen()) {
            case WonderAppResultPayload.SUB_BEGIN -> {
                if (state == SubScreen.LOADING || state == SubScreen.BEGIN || state == SubScreen.ERROR) {
                    state = SubScreen.BEGIN;
                }
            }
            case WonderAppResultPayload.SUB_TRADE -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.offeredPokemon = Pokemon.Companion.loadFromNBT(
                            this.minecraft.player.registryAccess(), p.offeredNbt());
                    this.receivedPokemon = Pokemon.Companion.loadFromNBT(
                            this.minecraft.player.registryAccess(), p.receivedNbt());
                }
                this.offeredState = new FloatingState();
                this.receivedState = new FloatingState();
                this.tradeStartedAtMillis = System.currentTimeMillis();
                state = SubScreen.TRADE;
            }
            case WonderAppResultPayload.SUB_ERROR -> state = SubScreen.ERROR;
            default -> {
            }
        }
    }

    @Override
    protected boolean showBackButton() {
        if (state != SubScreen.TRADE) {
            return true;
        }
        return System.currentTimeMillis() - tradeStartedAtMillis >= ANIM_TOTAL_MS;
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        switch (state) {
            case LOADING -> {
            }
            case BEGIN -> {
                renderHeader(graphics);
                renderBegin(graphics, mouseX, mouseY);
            }
            case SELECT -> {
                renderHeader(graphics);
                renderSelect(graphics, mouseX, mouseY, partialTick);
            }
            case TRADE -> renderTrade(graphics, mouseX, mouseY, partialTick);
            case ERROR -> {
                renderHeader(graphics);
                if (!lastErrorKey.isEmpty()) {
                    drawScaledCentered(graphics, Component.translatable(lastErrorKey),
                            originX + 174, originY + 152, 0xFFFFFFFF);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if ((state == SubScreen.BEGIN || state == SubScreen.SELECT) && hasEvent
                && isInBounds(mouseX, mouseY, originX + 46, originY + 136, 255, 32)) {
            graphics.renderComponentTooltip(this.font, buildEventTooltipLines(), mouseX, mouseY);
        }
        if (state == SubScreen.SELECT && hasShiftDown()) {
            int slot = hoveredPartySlot(mouseX, mouseY);
            if (slot >= 0) {
                Pokemon p = getPartyPokemon(slot);
                if (p != null) {
                    graphics.renderComponentTooltip(this.font, buildPokemonTooltip(p), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return switch (state) {
            case BEGIN -> handleBeginClick(mouseX, mouseY);
            case SELECT -> handleSelectClick(mouseX, mouseY);
            case TRADE -> handleTradeClick(mouseX, mouseY);
            default -> super.mouseClicked(mouseX, mouseY, button);
        };
    }

    @Override
    protected void onBackButtonClicked() {
        if (state == SubScreen.SELECT) {
            selectedSlot = -1;
            selectedLocked = false;
            state = SubScreen.BEGIN;
            return;
        }
        super.onBackButtonClicked();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (state == SubScreen.SELECT && (selectedSlot >= 0 || selectedLocked)) {
                selectedSlot = -1;
                selectedLocked = false;
                return true;
            }
            onBackButtonClicked();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderHeader(GuiGraphics g) {
        drawTinted(g, TEX_LOGO, originX + 158, originY + 16, 32, 32, 0xFFFFFFFF);
        drawScaledRightAligned(g, Component.translatable("gui.cobblesafari.rotomphone.wonder.title1"),
                originX + 154, originY + 32, 0xFFFFFFFF);
        drawScaledLeftAligned(g, Component.translatable("gui.cobblesafari.rotomphone.wonder.title2"),
                originX + 194, originY + 32, 0xFFFFFFFF);
    }

    private void renderBegin(GuiGraphics g, int mx, int my) {
        int theme = getTintColor();
        boolean unlimited = ticketsRemaining < 0;

        Component ticketLine = unlimited
                ? Component.translatable("gui.cobblesafari.rotomphone.wonder.ticket.unlimited")
                : Component.translatable("gui.cobblesafari.rotomphone.wonder.ticket",
                        ticketsRemaining, formatCountdown(nextResetEpochSeconds));
        g.drawCenteredString(this.font, ticketLine, originX + 174, originY + 72, theme);

        boolean showStart = unlimited || ticketsRemaining >= 1;
        if (showStart) {
            boolean hov = isInBounds(mx, my, originX + 138, originY + 96, 72, 32);
            drawButton(g, originX + 138, originY + 96, hov, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.wonder.start"));
        } else {
            drawScaledCentered(g,
                    Component.translatable("gui.cobblesafari.rotomphone.wonder.error.ticket"),
                    originX + 174, originY + 112, theme);
        }

        renderEventBanner(g);

        if (!lastErrorKey.isEmpty()) {
            drawScaledCentered(g, Component.translatable(lastErrorKey), originX + 174, originY + 152, 0xFFFFFFFF);
        }
    }

    private void renderSelect(GuiGraphics g, int mx, int my, float partialTick) {
        int theme = getTintColor();
        ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();

        for (int i = 0; i < 6; i++) {
            int x = originX + PARTY_SLOT_X[i];
            int y = originY + 56;
            Pokemon p = party.get(i);
            boolean hovered = !selectedLocked && isInBounds(mx, my, x, y, 32, 32);
            boolean selected = i == selectedSlot;
            int tint = (selected || hovered) ? 0xFFFFFFFF : theme;
            drawTinted(g, TEX_EMPTY, x, y, 32, 32, tint);
            if (p != null) {
                drawPokemonInArea(g, p, x, y, PARTY_SLOT_SIZE, PARTY_SLOT_SIZE, 0,
                        partialTick, partyStates[i], PARTY_SLOT_BASE_SCALE, PARTY_SLOT_MODEL_SCALE,
                        PARTY_SLOT_MODEL_Y_OFFSET);
            }
        }

        int digit = countdownDigit();
        if (selectedSlot < 0) {
            drawScaledCentered(g, Component.translatable("gui.cobblesafari.rotomphone.wonder.choose"),
                    originX + 174, originY + 112, theme);
        } else if (!selectedLocked) {
            boolean h = isInBounds(mx, my, originX + 138, originY + 96, 72, 32);
            drawButton(g, originX + 138, originY + 96, h, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.wonder.trade"));
        } else if (digit > 0) {
            drawTinted(g, TEX_DOUBLE, originX + 138, originY + 96, 72, 32, theme);
            int textY = originY + 96 + (32 - this.font.lineHeight) / 2;
            g.drawCenteredString(this.font, Component.literal(String.valueOf(digit)),
                    originX + 138 + 36, textY, theme);
        } else {
            boolean h = isInBounds(mx, my, originX + 138, originY + 96, 72, 32);
            drawButton(g, originX + 138, originY + 96, h, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.wonder.confirm"));
        }

        renderEventBanner(g);
    }

    private void renderTrade(GuiGraphics g, int mx, int my, float partialTick) {
        int theme = getTintColor();
        long elapsed = System.currentTimeMillis() - tradeStartedAtMillis;
        int sx = originX + TRADE_SLOT_X;
        int sy = originY + TRADE_SLOT_Y;

        boolean showOffered = shouldShowOfferedModel(elapsed);
        boolean showReceived = shouldShowReceivedModel(elapsed);

        if (showOffered && offeredPokemon != null) {
            drawPokemonInArea(g, offeredPokemon, sx, sy, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_CLIP_WIDTH, partialTick, offeredState, TRADE_SLOT_BASE_SCALE,
                    TRADE_SLOT_MODEL_SCALE, 0f);
        } else if (showReceived && receivedPokemon != null) {
            drawPokemonInArea(g, receivedPokemon, sx, sy, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_CLIP_WIDTH, partialTick, receivedState, TRADE_SLOT_BASE_SCALE,
                    TRADE_SLOT_MODEL_SCALE, 0f);
        }

        int animFrame = getTradeAnimFrame(elapsed);
        if (animFrame >= 0) {
            int vOffset = animFrame * TRADE_SLOT_SIZE;
            drawBlitWithAlpha(g, TEX_TRADEANIM, sx, sy, 0, vOffset, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_SIZE, TRADE_SLOT_SIZE * ANIM_FRAME_COUNT);
        }

        if (elapsed >= ANIM_TOTAL_MS) {
            boolean h = isInBounds(mx, my, originX + 138, originY + 136, 72, 32);
            drawButton(g, originX + 138, originY + 136, h, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.wonder.again"));
        }
    }

    private void renderEventBanner(GuiGraphics g) {
        if (!hasEvent) {
            return;
        }
        ResourceLocation bannerTex = getEventBannerTexture();
        int bx = originX + 46;
        int by = originY + 136;
        drawBlitWithAlpha(g, bannerTex, bx, by, 0, 0, 255, 32, 255, 32);
        Component bannerLabel = Component.translatable(
                "gui.cobblesafari.rotomphone.wonder.banner", eventName, eventDaysLeft);
        int labelY = by + (32 - this.font.lineHeight) / 2;
        g.drawCenteredString(this.font, bannerLabel, originX + 174, labelY, 0xFFFFFFFF);
    }

    private boolean shouldShowOfferedModel(long elapsed) {
        if (elapsed < ANIM_INITIAL_DELAY_MS) {
            return true;
        }
        long animElapsed = elapsed - ANIM_INITIAL_DELAY_MS;
        if (animElapsed < (long) ANIM_FRAME_COUNT * ANIM_FRAME_MS) {
            int frame = (int) (animElapsed / ANIM_FRAME_MS);
            return frame < 4;
        }
        return false;
    }

    private boolean shouldShowReceivedModel(long elapsed) {
        long reverseStart = ANIM_INITIAL_DELAY_MS + (long) ANIM_FRAME_COUNT * ANIM_FRAME_MS + ANIM_PAUSE_MS;
        if (elapsed < reverseStart) {
            return false;
        }
        long reverseElapsed = elapsed - reverseStart;
        if (reverseElapsed >= (long) ANIM_FRAME_COUNT * ANIM_FRAME_MS) {
            return true;
        }
        int frameFromStart = (int) (reverseElapsed / ANIM_FRAME_MS);
        int frameFromEnd = ANIM_FRAME_COUNT - 1 - frameFromStart;
        return frameFromEnd <= 4;
    }

    private int getTradeAnimFrame(long elapsed) {
        if (elapsed < ANIM_INITIAL_DELAY_MS) {
            return -1;
        }
        long animElapsed = elapsed - ANIM_INITIAL_DELAY_MS;
        long forwardDuration = (long) ANIM_FRAME_COUNT * ANIM_FRAME_MS;
        if (animElapsed < forwardDuration) {
            return (int) (animElapsed / ANIM_FRAME_MS);
        }
        long afterForward = animElapsed - forwardDuration;
        if (afterForward < ANIM_PAUSE_MS) {
            return -1;
        }
        long reverseElapsed = afterForward - ANIM_PAUSE_MS;
        if (reverseElapsed >= forwardDuration) {
            return -1;
        }
        int reverseFrame = (int) (reverseElapsed / ANIM_FRAME_MS);
        return ANIM_FRAME_COUNT - 1 - reverseFrame;
    }

    private boolean handleBeginClick(double mx, double my) {
        boolean unlimited = ticketsRemaining < 0;
        if ((unlimited || ticketsRemaining >= 1)
                && isInBounds(mx, my, originX + 138, originY + 96, 72, 32)) {
            lastErrorKey = "";
            selectedSlot = -1;
            selectedLocked = false;
            state = SubScreen.SELECT;
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleSelectClick(double mx, double my) {
        if (!selectedLocked) {
            for (int i = 0; i < 6; i++) {
                int x = originX + PARTY_SLOT_X[i];
                int y = originY + 56;
                if (isInBounds(mx, my, x, y, 32, 32) && getPartyPokemon(i) != null) {
                    selectedSlot = i;
                    lastErrorKey = "";
                    return true;
                }
            }
        }

        if (selectedSlot >= 0 && !selectedLocked
                && isInBounds(mx, my, originX + 138, originY + 96, 72, 32)) {
            selectedLocked = true;
            lockedAtMillis = System.currentTimeMillis();
            return true;
        }

        if (selectedLocked && countdownDigit() == 0
                && isInBounds(mx, my, originX + 138, originY + 96, 72, 32)) {
            Services.PLATFORM.sendPayloadToServer(
                    new WonderAppPayload(WonderAppPayload.ACTION_TRADE, selectedSlot));
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleTradeClick(double mx, double my) {
        if (System.currentTimeMillis() - tradeStartedAtMillis < ANIM_TOTAL_MS) {
            return false;
        }
        if (isInBounds(mx, my, originX + 138, originY + 136, 72, 32)) {
            if (ticketsRemaining == 0) {
                state = SubScreen.BEGIN;
            } else {
                selectedSlot = -1;
                selectedLocked = false;
                state = SubScreen.SELECT;
            }
            Services.PLATFORM.sendPayloadToServer(
                    new WonderAppPayload(WonderAppPayload.ACTION_REQUEST_STATE, 0));
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private int hoveredPartySlot(int mx, int my) {
        for (int i = 0; i < 6; i++) {
            if (isInBounds(mx, my, originX + PARTY_SLOT_X[i], originY + 56, 32, 32)) {
                return i;
            }
        }
        return -1;
    }

    private Pokemon getPartyPokemon(int slot) {
        return CobblemonClient.INSTANCE.getStorage().getParty().get(slot);
    }

    private int countdownDigit() {
        if (!selectedLocked) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - lockedAtMillis;
        if (elapsed < 1000L) {
            return 3;
        }
        if (elapsed < 2000L) {
            return 2;
        }
        if (elapsed < 3000L) {
            return 1;
        }
        return 0;
    }

    private ResourceLocation getEventBannerTexture() {
        if (!hasEvent || customBannerName == null || customBannerName.isEmpty()) {
            return TEX_BANNER_DEF;
        }
        return loc("wonder/rotomphone_gui_event_" + customBannerName + ".png");
    }

    private List<Component> buildEventTooltipLines() {
        List<Component> lines = new ArrayList<>();
        int sum = 0;
        for (WonderAppResultPayload.EventPoolEntry e : eventPool) {
            if (e.weight() > 0) {
                sum += e.weight();
            }
        }
        if (sum <= 0) {
            return lines;
        }
        for (WonderAppResultPayload.EventPoolEntry e : eventPool) {
            if (e.weight() <= 0) {
                continue;
            }
            double pct = (e.weight() * 100.0) / sum;
            lines.add(Component.translatable(
                    "gui.cobblesafari.rotomphone.wonder.banner.tooltip_line",
                    e.groupId(),
                    String.format(Locale.ROOT, "%.1f", pct)));
        }
        return lines;
    }

    private List<Component> buildPokemonTooltip(Pokemon p) {
        List<Component> lines = new ArrayList<>();
        lines.add(p.getDisplayName(false));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.level", p.getLevel()));

        String genderKey = switch (p.getGender()) {
            case MALE -> "cobblemon.gender.male";
            case FEMALE -> "cobblemon.gender.female";
            default -> "cobblemon.gender.genderless";
        };
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.gender",
                Component.translatable(genderKey)));

        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.shiny",
                Component.translatable(p.getShiny()
                        ? "gui.cobblesafari.rotomphone.wonder.tt.shiny.yes"
                        : "gui.cobblesafari.rotomphone.wonder.tt.shiny.no")));

        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.ability",
                Component.translatable("cobblemon.ability." + p.getAbility().getName())));

        var ballId = p.getCaughtBall().getName();
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.ball",
                Component.translatable("item." + ballId.getNamespace() + "." + ballId.getPath())));

        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.stats"));
        for (int i = 0; i < TOOLTIP_STATS.length; i++) {
            Stat st = TOOLTIP_STATS[i];
            Integer ev = p.getEvs().get(st);
            Integer iv = p.getIvs().get(st);
            lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.stat_line",
                    Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.stat." + TOOLTIP_STAT_KEYS[i]),
                    ev == null ? 0 : ev,
                    iv == null ? 0 : iv));
        }
        return lines;
    }

    private void drawPokemonInArea(
            GuiGraphics g,
            Pokemon pokemon,
            int x,
            int y,
            int w,
            int h,
            int clipWidth,
            float partialTick,
            FloatingState state,
            float baseScale,
            float modelScale,
            float modelYOffset) {
        int scissorW = clipWidth > 0 ? clipWidth : w;
        int clipX = x + (w - scissorW) / 2;
        g.enableScissor(clipX, y, clipX + scissorW, y + h);
        g.pose().pushPose();
        g.pose().translate(x + w * 0.5, y + 1.0 - modelYOffset, 0.0);
        g.pose().scale(baseScale, baseScale, baseScale);
        Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(
                new Quaternionf(), new Vector3f(13f, 35f, 0f));
        RenderablePokemon renderable = pokemon.asRenderablePokemon();
        state.setCurrentAspects(renderable.getAspects());
        PokemonGuiUtilsKt.drawProfilePokemon(
                renderable,
                g.pose(),
                rotation,
                PoseType.PROFILE,
                state,
                partialTick,
                modelScale,
                true,
                false,
                1f,
                1f,
                1f,
                1f,
                0f,
                0f);
        g.pose().popPose();
        g.disableScissor();
    }

    private void drawScaledRightAligned(GuiGraphics g, Component c, int anchorX, int y, int color) {
        int w = this.font.width(c);
        g.pose().pushPose();
        g.pose().translate(anchorX, y + SCALED_TEXT_Y_OFFSET, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(this.font, c, -w, 0, color, false);
        g.pose().popPose();
    }

    private void drawScaledLeftAligned(GuiGraphics g, Component c, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y + SCALED_TEXT_Y_OFFSET, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(this.font, c, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawScaledCentered(GuiGraphics g, Component c, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y + SCALED_TEXT_Y_OFFSET, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawCenteredString(this.font, c, 0, 0, color);
        g.pose().popPose();
    }

    private void drawBlitWithAlpha(
            GuiGraphics g,
            ResourceLocation tex,
            int x,
            int y,
            int u,
            int v,
            int w,
            int h,
            int texW,
            int texH) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, 1f);
        g.blit(tex, x, y, u, v, w, h, texW, texH);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawTinted(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int argb) {
        float red = ((argb >> 16) & 0xFF) / 255f;
        float green = ((argb >> 8) & 0xFF) / 255f;
        float blue = (argb & 0xFF) / 255f;
        float alpha = ((argb >>> 24) & 0xFF) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(red, green, blue, alpha);
        g.blit(tex, x, y, 0, 0, w, h, w, h);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawButton(GuiGraphics g, int x, int y, boolean hovered, int themeTint, Component label) {
        int tint = hovered ? 0xFFFFFFFF : themeTint;
        drawTinted(g, TEX_DOUBLE, x, y, 72, 32, tint);
        int textY = y + (32 - this.font.lineHeight) / 2;
        g.drawCenteredString(this.font, label, x + 36, textY, tint);
    }

    private static String formatCountdown(long resetEpochSeconds) {
        long delta = Math.max(0L, resetEpochSeconds - (System.currentTimeMillis() / 1000L));
        long h = delta / 3600;
        long m = (delta % 3600) / 60;
        long s = delta % 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s);
    }
}
