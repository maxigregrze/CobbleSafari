package maxigregrze.cobblesafari.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class TimerHudOverlay {
    private static final ResourceLocation TIMER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/timer_background.png");

    private static final int TEXTURE_WIDTH = 88;
    private static final int TEXTURE_HEIGHT = 64;
    private static final int FULL_TEXTURE_HEIGHT = 256;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int MARGIN = 0;
    private static final int TEXT_OFFSET_Y = 7;

    private static String currentTime = "";
    private static boolean visible = false;
    private static int remainingTicks = 0;
    private static boolean bypassed = false;
    private static String activeDimensionId = "";

    public static void updateDisplay(String dimensionId, int ticks, boolean active, boolean isBypassed) {
        if (active) {
            activeDimensionId = dimensionId;
            remainingTicks = ticks;
            bypassed = isBypassed;
            visible = true;

            int totalSeconds = ticks / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            currentTime = String.format("%02d:%02d", minutes, seconds);
        } else if (dimensionId.equals(activeDimensionId)) {
            visible = false;
            currentTime = "";
            activeDimensionId = "";
            bypassed = false;
        }
    }

    public static void updateDisplay(String dimensionId, int ticks, boolean active) {
        updateDisplay(dimensionId, ticks, active, false);
    }

    @Deprecated
    public static void updateDisplay(int ticks, boolean active) {
        updateDisplay("cobblesafari:domedimension", ticks, active, false);
    }

    private static int getTextureIndex() {
        if (bypassed) {
            return 1;
        }

        int totalSeconds = remainingTicks / 20;
        long currentTimeMs = Util.getMillis();

        if (totalSeconds > 60) {
            long cycleMs = currentTimeMs % 10000;
            return (cycleMs >= 9500) ? 1 : 0;
        } else if (totalSeconds > 30) {
            long cycleMs = currentTimeMs % 5000;
            return (cycleMs >= 4500) ? 1 : 0;
        } else {
            long cycleMs = currentTimeMs % 1000;
            return (cycleMs < 500) ? 2 : 3;
        }
    }

    public static void renderBackground(GuiGraphics graphics) {
        if (!visible || currentTime.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (mc.options.hideGui) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int textureX = (screenWidth - TEXTURE_WIDTH) / 2;
        int textureY = screenHeight - HOTBAR_HEIGHT - TEXTURE_HEIGHT - MARGIN;

        int textureIndex = getTextureIndex();
        int vOffset = textureIndex * TEXTURE_HEIGHT;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.blit(
                TIMER_TEXTURE,
                textureX,
                textureY,
                0,
                vOffset,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                FULL_TEXTURE_HEIGHT
        );

        RenderSystem.disableBlend();
    }

    public static void renderText(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!visible || currentTime.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (mc.options.hideGui) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int textureX = (screenWidth - TEXTURE_WIDTH) / 2;
        int textureY = screenHeight - HOTBAR_HEIGHT - TEXTURE_HEIGHT - MARGIN;

        Font font = mc.font;
        int textWidth = font.width(currentTime);
        int textX = textureX + (TEXTURE_WIDTH - textWidth) / 2;
        int textY = textureY + (TEXTURE_HEIGHT - font.lineHeight) / 2 + TEXT_OFFSET_Y;

        int textColor;
        if (bypassed) {
            textColor = 0xAAAAAA;
        } else {
            int totalSeconds = remainingTicks / 20;
            if (totalSeconds < 30) {
                textColor = 0xFF5555;
            } else if (totalSeconds < 60) {
                textColor = 0xFFAA00;
            } else {
                textColor = 0x55FF55;
            }
        }

        graphics.drawString(font, currentTime, textX, textY, textColor, true);
    }
}
