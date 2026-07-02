package maxigregrze.cobblesafari.client.screen.rotomphone;

import com.mojang.blaze3d.systems.RenderSystem;
import maxigregrze.cobblesafari.network.UnionAppPayload;
import maxigregrze.cobblesafari.network.UnionAppResultPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RotomPhoneUnionScreen extends RotomPhoneBaseScreen {

    private enum SubScreen {
        LOADING,
        BEGIN,
        CHOOSE_TYPE,
        UNION_ROOM,
        JOIN,
        ERROR,
        GUEST
    }

    private static final ResourceLocation TEX_LOGO = loc("union/rotomphone_gui_icon_union.png");
    private static final ResourceLocation TEX_DOUBLE = loc("rotomphone_gui_icon_double.png");
    private static final ResourceLocation TEX_RIGHT = loc("rotomphone_gui_icon_right.png");
    private static final ResourceLocation TEX_EMPTY = loc("rotomphone_gui_icon_empty.png");
    private static final ResourceLocation TEX_RESET = loc("union/rotomphone_gui_icon_code_reset.png");
    private static final ResourceLocation TEX_ERROR = loc("rotomphone_gui_error.png");

    /** Added to anchor Y before scaling; negative moves the drawn text upward by 7 screen pixels. */
    private static final float SCALED_TEXT_Y_OFFSET = -7f;

    private static final ResourceLocation[] TEX_DIGITS = {
            null,
            loc("union/rotomphone_gui_icon_code_1.png"),
            loc("union/rotomphone_gui_icon_code_2.png"),
            loc("union/rotomphone_gui_icon_code_3.png"),
            loc("union/rotomphone_gui_icon_code_4.png"),
            loc("union/rotomphone_gui_icon_code_5.png"),
            loc("union/rotomphone_gui_icon_code_6.png"),
    };

    private SubScreen state = SubScreen.LOADING;
    private int instancesUsed;
    private int instancesMax;
    private int[] currentRoomCode = new int[0];
    private final int[] inputCode = new int[4];
    private int inputFilled;
    private String lastErrorKey = "";
    private long openedAt;

    public RotomPhoneUnionScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode, boolean rotoGlide) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.union"), rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide);
    }

    private RotomPhoneUnionScreen(RotomPhoneShell shell) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.union"), "", false, "", false, false, shell);
    }

    public static RotomPhoneUnionScreen forOnlinePc() {
        return new RotomPhoneUnionScreen(RotomPhoneShell.ONLINE_PC);
    }

    @Override
    protected void init() {
        super.init();
        openedAt = System.currentTimeMillis();
        Services.PLATFORM.sendPayloadToServer(new UnionAppPayload(UnionAppPayload.ACTION_REQUEST_STATE, new int[0]));
    }

    public void applyServerSnapshot(UnionAppResultPayload p) {
        if (p.subscreen() == UnionAppResultPayload.SUB_CLOSE_GUI) {
            return;
        }
        this.instancesUsed = p.instancesUsed();
        this.instancesMax = p.instancesMax();
        this.lastErrorKey = p.errorKey() == null ? "" : p.errorKey();
        int[] cc = p.currentCode();
        this.currentRoomCode = cc == null ? new int[0] : cc.clone();

        if (state == SubScreen.JOIN && p.subscreen() == UnionAppResultPayload.SUB_BEGIN) {
            return;
        }
        if (state == SubScreen.CHOOSE_TYPE && p.subscreen() == UnionAppResultPayload.SUB_BEGIN) {
            return;
        }

        switch (p.subscreen()) {
            case UnionAppResultPayload.SUB_BEGIN -> state = SubScreen.BEGIN;
            case UnionAppResultPayload.SUB_UNION_HOST -> state = SubScreen.UNION_ROOM;
            case UnionAppResultPayload.SUB_ERROR -> state = SubScreen.ERROR;
            case UnionAppResultPayload.SUB_GUEST -> state = SubScreen.GUEST;
            default -> {
                // SUB_CLOSE_GUI handled above; unknown values ignored
            }
        }
    }

    @Override
    protected boolean showBackButton() {
        return state != SubScreen.LOADING;
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        switch (state) {
            case BEGIN, CHOOSE_TYPE, UNION_ROOM, JOIN, GUEST -> renderHeader(graphics);
            case ERROR -> {
                renderHeader(graphics);
                renderErrorIcon(graphics);
                if (!lastErrorKey.isEmpty()) {
                    drawScaledCentered(graphics, Component.translatable(lastErrorKey),
                            originX + 174, originY + 152, 0xFFFFFFFF);
                }
                return;
            }
            case LOADING -> {
                // Waiting for UnionAppResultPayload from server
            }
            default -> {
            }
        }
        switch (state) {
            case BEGIN -> renderBegin(graphics, mouseX, mouseY);
            case CHOOSE_TYPE -> renderChooseType(graphics, mouseX, mouseY);
            case UNION_ROOM -> renderUnionRoom(graphics, mouseX, mouseY);
            case JOIN -> renderJoin(graphics, mouseX, mouseY);
            case GUEST -> renderGuest(graphics);
            default -> {
            }
        }
    }

    private void renderHeader(GuiGraphics g) {
        drawTinted(g, TEX_LOGO, originX + 158, originY + 16, 32, 32, 0xFFFFFFFF);
        drawScaledRightAligned(g, Component.translatable("gui.cobblesafari.rotomphone.union.title1"), originX + 154, originY + 32, 0xFFFFFFFF);
        drawScaledLeftAligned(g, Component.translatable("gui.cobblesafari.rotomphone.union.title2"), originX + 194, originY + 32, 0xFFFFFFFF);
    }

    /** Renders text at 2× scale, right-aligned so its right edge touches anchorX. */
    private void drawScaledRightAligned(GuiGraphics g, Component c, int anchorX, int y, int color) {
        int w = this.font.width(c);
        g.pose().pushPose();
        g.pose().translate(anchorX, y + SCALED_TEXT_Y_OFFSET, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(this.font, c, -w, 0, color, false);
        g.pose().popPose();
    }

    /** Renders text at 2× scale, left-aligned so its left edge starts at x. */
    private void drawScaledLeftAligned(GuiGraphics g, Component c, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y + SCALED_TEXT_Y_OFFSET, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(this.font, c, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Renders text at 2× scale, centered on x. */
    private void drawScaledCentered(GuiGraphics g, Component c, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y + SCALED_TEXT_Y_OFFSET, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawCenteredString(this.font, c, 0, 0, color);
        g.pose().popPose();
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

    private void renderChooseType(GuiGraphics g, int mx, int my) {
        int theme = getTintColor();
        drawScaledCentered(g,
                Component.translatable("gui.cobblesafari.rotomphone.union.choosetype"),
                originX + 174, originY + 72, theme);

        boolean roomHover = isInBounds(mx, my, originX + 98, originY + 96, 72, 32);
        drawButton(g, originX + 98, originY + 96, roomHover, theme,
                Component.translatable("gui.cobblesafari.rotomphone.union.room"));

        boolean plazaHover = isInBounds(mx, my, originX + 178, originY + 96, 72, 32);
        drawButton(g, originX + 178, originY + 96, plazaHover, theme,
                Component.translatable("gui.cobblesafari.rotomphone.union.plaza"));

        if (!lastErrorKey.isEmpty()) {
            drawScaledCentered(g, Component.translatable(lastErrorKey), originX + 174, originY + 152, 0xFFFFFFFF);
        }
    }

    private void renderBegin(GuiGraphics g, int mx, int my) {
        int theme = getTintColor();
        int available = Math.max(0, instancesMax - instancesUsed);
        drawScaledCentered(g,
                Component.translatable("gui.cobblesafari.rotomphone.union.vacancy", available, instancesMax),
                originX + 174, originY + 72, theme);

        boolean cHover = isInBounds(mx, my, originX + 98, originY + 96, 72, 32);
        drawButton(g, originX + 98, originY + 96, cHover, theme,
                Component.translatable("gui.cobblesafari.rotomphone.union.create"));

        boolean jHover = isInBounds(mx, my, originX + 178, originY + 96, 72, 32);
        drawButton(g, originX + 178, originY + 96, jHover, theme,
                Component.translatable("gui.cobblesafari.rotomphone.union.join"));

        if (!lastErrorKey.isEmpty()) {
            drawScaledCentered(g, Component.translatable(lastErrorKey), originX + 174, originY + 152, 0xFFFFFFFF);
        }
    }

    private void drawButton(GuiGraphics g, int x, int y, boolean hovered, int themeTint, Component label) {
        int tint = hovered ? 0xFFFFFFFF : themeTint;
        drawTinted(g, TEX_DOUBLE, x, y, 72, 32, tint);
        int textY = y + (32 - this.font.lineHeight) / 2;
        g.drawCenteredString(this.font, label, x + 36, textY, tint);
    }

    private void renderUnionRoom(GuiGraphics g, int mx, int my) {
        int theme = getTintColor();
        drawScaledCentered(g, Component.translatable("gui.cobblesafari.rotomphone.union.current"),
                originX + 174, originY + 72, theme);
        final int[] xs = {98, 138, 178, 218};
        for (int i = 0; i < 4 && i < currentRoomCode.length; i++) {
            int d = currentRoomCode[i];
            if (d >= 1 && d <= 6) {
                drawTinted(g, TEX_DIGITS[d], originX + xs[i], originY + 96, 32, 32, 0xFFFFFFFF);
            }
        }
        boolean h = isInBounds(mx, my, originX + 138, originY + 136, 72, 32);
        drawButton(g, originX + 138, originY + 136, h, theme,
                Component.translatable("gui.cobblesafari.rotomphone.union.close"));
    }

    private void renderGuest(GuiGraphics g) {
        int theme = getTintColor();
        drawScaledCentered(g, Component.translatable("gui.cobblesafari.rotomphone.union.current"),
                originX + 174, originY + 72, theme);
        final int[] xs = {98, 138, 178, 218};
        for (int i = 0; i < 4 && i < currentRoomCode.length; i++) {
            int d = currentRoomCode[i];
            if (d >= 1 && d <= 6) {
                drawTinted(g, TEX_DIGITS[d], originX + xs[i], originY + 96, 32, 32, 0xFFFFFFFF);
            }
        }
    }

    private void renderJoin(GuiGraphics g, int mx, int my) {
        int theme = getTintColor();
        boolean resetH = isInBounds(mx, my, originX + 58, originY + 56, 32, 32);
        drawTinted(g, TEX_RESET, originX + 58, originY + 56, 32, 32, resetH ? 0xFFFFFFFF : theme);

        final int[] xs = {98, 138, 178, 218};
        for (int i = 0; i < 4; i++) {
            ResourceLocation tex = i < inputFilled ? TEX_DIGITS[inputCode[i]] : TEX_EMPTY;
            drawTinted(g, tex, originX + xs[i], originY + 56, 32, 32, 0xFFFFFFFF);
        }

        if (inputFilled == 4) {
            boolean joinH = isInBounds(mx, my, originX + 258, originY + 56, 32, 32);
            drawTinted(g, TEX_RIGHT, originX + 258, originY + 56, 32, 32, joinH ? 0xFFFFFFFF : theme);
        }

        final int[] padX = {58, 98, 138, 178, 218, 258};
        for (int d = 1; d <= 6; d++) {
            int x = padX[d - 1];
            boolean h = isInBounds(mx, my, originX + x, originY + 96, 32, 32);
            drawTinted(g, TEX_DIGITS[d], originX + x, originY + 96, 32, 32, h ? 0xFFFFFFFF : theme);
        }

        if (!lastErrorKey.isEmpty()) {
            drawScaledCentered(g, Component.translatable(lastErrorKey), originX + 174, originY + 152, 0xFFFFFFFF);
        }
    }

    private void renderErrorIcon(GuiGraphics g) {
        long elapsed = System.currentTimeMillis() - openedAt;
        long cycleTicks = (elapsed / 50) % 20;
        if (cycleTicks < 10) {
            // Centered in the phone's main area ((348-120)/2, (184-120)/2), matching RotomPhoneErrorScreen.
            g.blit(TEX_ERROR, originX + 114, originY + 32, 0, 0, 120, 120, 120, 120);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return switch (state) {
            case BEGIN -> handleBeginClick(mouseX, mouseY);
            case CHOOSE_TYPE -> handleChooseTypeClick(mouseX, mouseY);
            case JOIN -> handleJoinClick(mouseX, mouseY);
            case UNION_ROOM -> handleUnionRoomClick(mouseX, mouseY);
            default -> super.mouseClicked(mouseX, mouseY, button);
        };
    }

    private boolean handleBeginClick(double mx, double my) {
        if (isInBounds(mx, my, originX + 98, originY + 96, 72, 32)) {
            lastErrorKey = "";
            state = SubScreen.CHOOSE_TYPE;
            return true;
        }
        if (isInBounds(mx, my, originX + 178, originY + 96, 72, 32)) {
            lastErrorKey = "";
            inputFilled = 0;
            state = SubScreen.JOIN;
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleChooseTypeClick(double mx, double my) {
        if (isInBounds(mx, my, originX + 98, originY + 96, 72, 32)) {
            lastErrorKey = "";
            Services.PLATFORM.sendPayloadToServer(new UnionAppPayload(UnionAppPayload.ACTION_CREATE, new int[]{0}));
            return true;
        }
        if (isInBounds(mx, my, originX + 178, originY + 96, 72, 32)) {
            lastErrorKey = "";
            Services.PLATFORM.sendPayloadToServer(new UnionAppPayload(UnionAppPayload.ACTION_CREATE, new int[]{1}));
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleJoinClick(double mx, double my) {
        if (isInBounds(mx, my, originX + 58, originY + 56, 32, 32)) {
            inputFilled = 0;
            lastErrorKey = "";
            return true;
        }
        if (inputFilled == 4 && isInBounds(mx, my, originX + 258, originY + 56, 32, 32)) {
            int[] code = new int[4];
            System.arraycopy(inputCode, 0, code, 0, 4);
            Services.PLATFORM.sendPayloadToServer(new UnionAppPayload(UnionAppPayload.ACTION_JOIN, code));
            return true;
        }
        final int[] padX = {58, 98, 138, 178, 218, 258};
        for (int d = 1; d <= 6; d++) {
            if (isInBounds(mx, my, originX + padX[d - 1], originY + 96, 32, 32)) {
                if (inputFilled < 4) {
                    inputCode[inputFilled++] = d;
                    lastErrorKey = "";
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleUnionRoomClick(double mx, double my) {
        if (isInBounds(mx, my, originX + 138, originY + 136, 72, 32)) {
            Services.PLATFORM.sendPayloadToServer(new UnionAppPayload(UnionAppPayload.ACTION_CLOSE, new int[0]));
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    @Override
    protected void onBackButtonClicked() {
        if (state == SubScreen.CHOOSE_TYPE) {
            lastErrorKey = "";
            state = SubScreen.BEGIN;
            return;
        }
        if (state == SubScreen.JOIN) {
            inputFilled = 0;
            lastErrorKey = "";
            state = SubScreen.BEGIN;
            return;
        }
        super.onBackButtonClicked();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (state == SubScreen.JOIN && inputFilled > 0) {
                inputFilled = 0;
                lastErrorKey = "";
                return true;
            }
            onBackButtonClicked();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
