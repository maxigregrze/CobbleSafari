package maxigregrze.cobblesafari.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.basepc.BasePCBlockEntity;
import maxigregrze.cobblesafari.block.basepc.BasePCMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class BasePCScreen extends AbstractContainerScreen<BasePCMenu> {

    private static final ResourceLocation GUI_TOP = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/basepc/pc_top.png");
    private static final ResourceLocation GUI_GLOW = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/basepc/pc_glow.png");
    private static final ResourceLocation ARROW_L = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/basepc/pc_arrow_l.png");
    private static final ResourceLocation ARROW_R = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/basepc/pc_arrow_r.png");
    private static final ResourceLocation CHARGEBAR = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/basepc/pc_chargebar.png");
    private static final ResourceLocation BTN_EFFECT = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/basepc/pc_button_effect.png");
    private static final ResourceLocation BTN_FLAG = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/basepc/pc_button_flag.png");

    private static final int GUI_WIDTH = 272;
    private static final int GUI_HEIGHT = 251;

    private static final String[] EFFECT_KEYS = {
            "gui.cobblesafari.basepc.effect.repel",
            "gui.cobblesafari.basepc.effect.uncommon_boost",
            "gui.cobblesafari.basepc.effect.rare_boost",
            "gui.cobblesafari.basepc.effect.ultra_rare_boost",
            "gui.cobblesafari.basepc.effect.shiny_boost"
    };

    public BasePCScreen(BasePCMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = -9999;
        this.titleLabelY = -9999;
        this.inventoryLabelX = -9999;
        this.inventoryLabelY = -9999;
    }

    private int getRank() { return menu.getData().get(0); }
    private int getCurrentEffect() { return menu.getData().get(1); }
    private int getBattery() { return menu.getData().get(2); }
    private boolean isActive() { return menu.getData().get(3) != 0; }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int x = leftPos;
        int y = topPos;
        int rank = getRank();
        int effect = getCurrentEffect();

        graphics.blit(GUI_TOP, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        ResourceLocation rankTexture = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                "textures/gui/basepc/pc_rank_" + rank + ".png");
        graphics.blit(rankTexture, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(GUI_GLOW, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        RenderSystem.disableBlend();

        ResourceLocation boostIcon = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                "textures/gui/basepc/pc_boosticon_" + effect + ".png");
        graphics.blit(boostIcon, x + 118, y + 53, 0, 0, 36, 36, 36, 36);

        drawArrows(graphics, x, y, mouseX, mouseY);
        drawChargeBar(graphics, x, y);
        drawEffectButton(graphics, x, y, mouseX, mouseY);
        drawFlagButton(graphics, x, y, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int rank = getRank();
        int effect = getCurrentEffect();
        boolean active = isActive();
        int battery = getBattery();
        int maxBattery = BasePCBlockEntity.getMaxBattery(rank);

        Component statusText = active
                ? Component.translatable("gui.cobblesafari.basepc.status_active")
                : Component.translatable("gui.cobblesafari.basepc.status_inactive");
        int statusColor = active ? 0x55FF55 : 0xAAAAAA;

        Component header = Component.translatable("gui.cobblesafari.basepc.header", statusText);
        int headerWidth = font.width(header);
        graphics.drawString(font, header, 136 - headerWidth / 2, 8, 0xFFFFFF, true);

        if (active) {
            graphics.drawString(font, statusText, 136 - headerWidth / 2 + headerWidth - font.width(statusText), 8, statusColor, true);
        }

        Component effectName = Component.translatable(EFFECT_KEYS[effect]);
        int effectWidth = font.width(effectName);
        graphics.drawString(font, effectName, 136 - effectWidth / 2, 40, 0x3A96B6, false);

        int cost = BasePCBlockEntity.getEffectCost(rank, effect);
        Component costText;
        if (cost == -1) {
            costText = Component.translatable("gui.cobblesafari.basepc.locked");
        } else {
            double costPerSecond = maxBattery > 0 ? ((double) cost / 4.0) / maxBattery * 100.0 : 0;
            costText = Component.translatable("gui.cobblesafari.basepc.cost", String.format("%.2f", costPerSecond));
        }
        int costWidth = font.width(costText);
        graphics.drawString(font, costText, 136 - costWidth / 2, 96, 0x3A96B6, false);

        String pctStr;
        if (rank >= 5) {
            pctStr = Component.translatable("gui.cobblesafari.basepc.unlimited").getString();
        } else {
            int percentage = maxBattery > 0 ? (battery * 100 / maxBattery) : 0;
            pctStr = percentage + "%";
        }
        int pctWidth = font.width(pctStr);
        graphics.drawString(font, pctStr, 4 + 264 / 2 - pctWidth / 2, 125 + (16 - font.lineHeight) / 2, 0x3A96B6, false);
    }

    private void drawArrows(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int effect = getCurrentEffect();

        boolean hoverL = isInBounds(mouseX, mouseY, x + 88, y + 61, 22, 22) && effect > 0;
        int arrowLV = hoverL ? 22 : 0;
        graphics.blit(ARROW_L, x + 88, y + 61, 0, arrowLV, 22, 22, 22, 44);

        boolean hoverR = isInBounds(mouseX, mouseY, x + 162, y + 61, 22, 22) && effect < 4;
        int arrowRV = hoverR ? 22 : 0;
        graphics.blit(ARROW_R, x + 162, y + 61, 0, arrowRV, 22, 22, 22, 44);
    }

    private void drawChargeBar(GuiGraphics graphics, int x, int y) {
        int rank = getRank();
        int battery = getBattery();
        int maxBattery = BasePCBlockEntity.getMaxBattery(rank);
        int barWidth;
        if (rank >= 5) {
            barWidth = 264;
        } else {
            barWidth = maxBattery > 0 ? (int) (264.0 * battery / maxBattery) : 0;
        }
        barWidth = Math.max(0, Math.min(barWidth, 264));

        if (barWidth > 0) {
            graphics.blit(CHARGEBAR, x + 4, y + 123, 0, 0, barWidth, 16, 264, 16);
        }
    }

    private void drawEffectButton(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int rank = getRank();
        int effect = getCurrentEffect();
        boolean active = isActive();
        boolean locked = BasePCBlockEntity.isEffectLocked(rank, effect);
        boolean hover = isInBounds(mouseX, mouseY, x, y + 141, 126, 24);

        int btnV;
        if (hover) {
            btnV = (active || locked) ? 48 : 24;
        } else {
            btnV = 0;
        }
        graphics.blit(BTN_EFFECT, x, y + 141, 0, btnV, 126, 24, 126, 72);

        Component btnText;
        if (locked) {
            btnText = Component.translatable("gui.cobblesafari.basepc.locked");
        } else if (active) {
            btnText = Component.translatable("gui.cobblesafari.basepc.deactivate");
        } else {
            btnText = Component.translatable("gui.cobblesafari.basepc.activate");
        }
        int textWidth = font.width(btnText);
        graphics.drawString(font, btnText, x + 63 - textWidth / 2, y + 142 + (24 - font.lineHeight) / 2, 0xFFFFFF, true);
    }

    private void drawFlagButton(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int rank = getRank();
        int battery = getBattery();
        int maxBattery = BasePCBlockEntity.getMaxBattery(rank);
        net.minecraft.world.item.Item flagItem = menu.slots.get(0).getItem().getItem();
        boolean canUpgrade = BasePCMenu.wouldUpgrade(rank, flagItem);
        boolean isFull = rank >= 5 || (battery >= maxBattery && !canUpgrade);
        boolean hover = isInBounds(mouseX, mouseY, x + 146, y + 141, 126, 24);

        int btnV = (hover || isFull) ? 24 : 0;
        graphics.blit(BTN_FLAG, x + 146, y + 141, 0, btnV, 126, 24, 126, 48);

        Component btnText;
        if (rank >= 5) {
            btnText = Component.translatable("gui.cobblesafari.basepc.unlimited");
        } else if (isFull) {
            btnText = Component.translatable("gui.cobblesafari.basepc.full");
        } else {
            btnText = Component.translatable("gui.cobblesafari.basepc.capture");
        }
        int textWidth = font.width(btnText);
        graphics.drawString(font, btnText, x + 146 + 63 - textWidth / 2, y + 142 + (24 - font.lineHeight) / 2, 0xFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int x = leftPos;
        int y = topPos;

        if (isInBounds(mouseX, mouseY, x + 88, y + 61, 22, 22) && getCurrentEffect() > 0) {
            clickButton(0);
            return true;
        }

        if (isInBounds(mouseX, mouseY, x + 162, y + 61, 22, 22) && getCurrentEffect() < 4) {
            clickButton(1);
            return true;
        }

        if (isInBounds(mouseX, mouseY, x, y + 141, 126, 24)) {
            int rank = getRank();
            int effect = getCurrentEffect();
            if (!BasePCBlockEntity.isEffectLocked(rank, effect)) {
                clickButton(2);
            }
            return true;
        }

        if (isInBounds(mouseX, mouseY, x + 146, y + 141, 126, 24)) {
            int rank = getRank();
            int battery = getBattery();
            int maxBattery = BasePCBlockEntity.getMaxBattery(rank);
            net.minecraft.world.item.Item flagItem = menu.slots.get(0).getItem().getItem();
            boolean canUpgrade = BasePCMenu.wouldUpgrade(rank, flagItem);
            if (rank < 5 && (battery < maxBattery || canUpgrade)) {
                clickButton(3);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
            playClickSound();
        }
    }

    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private boolean isInBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
