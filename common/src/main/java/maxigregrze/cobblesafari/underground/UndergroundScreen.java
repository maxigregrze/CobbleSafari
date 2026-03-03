package maxigregrze.cobblesafari.underground;

import com.mojang.blaze3d.systems.RenderSystem;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.underground.logic.MiningCell;
import maxigregrze.cobblesafari.underground.logic.MiningGrid;
import maxigregrze.cobblesafari.underground.logic.TreasureDefinition;
import maxigregrze.cobblesafari.underground.logic.TreasureRegistry;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.underground.network.UndergroundPayloads;
import maxigregrze.cobblesafari.underground.screen.UndergroundScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side screen for the Underground Mining minigame.
 */
public class UndergroundScreen extends AbstractContainerScreen<UndergroundScreenHandler> {
    
    // Texture identifiers
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/underground/underground_gui.png");
    private static final ResourceLocation BTN_PICKAXE = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/underground/btn_pickaxe.png");
    private static final ResourceLocation BTN_PICKAXE_SELECTED = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/underground/btn_pickaxe_selected.png");
    private static final ResourceLocation BTN_HAMMER = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/underground/btn_hammer.png");
    private static final ResourceLocation BTN_HAMMER_SELECTED = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/underground/btn_hammer_selected.png");
    private static final ResourceLocation PREVIEW_1DMG = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/underground/preview_1dmg.png");
    private static final ResourceLocation PREVIEW_2DMG = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/underground/preview_2dmg.png");
    private static final ResourceLocation FINAL_MESSAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/underground/textbox.png");
    private static final ResourceLocation STATUS_BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/underground/statusbar.png");

    // GUI dimensions (based on the texture)
    private static final int GUI_WIDTH = 224;
    private static final int GUI_HEIGHT = 200;
    
    // Actual texture dimensions
    private static final int TEXTURE_WIDTH = 224;
    private static final int TEXTURE_HEIGHT = 200;
    
    // Grid positioning within the GUI
    private static final int GRID_OFFSET_X = 8;
    private static final int GRID_OFFSET_Y = 32;
    private static final int CELL_SIZE = 16;
    
    // Stability bar: single texture 208×1024, 64 frames of 16px height
    private static final int STATUS_BAR_X = 8;
    private static final int STATUS_BAR_Y = 14;
    private static final int STATUS_BAR_WIDTH = 208;
    private static final int STATUS_BAR_HEIGHT = 16;
    private static final int STATUS_BAR_FRAME_COUNT = 64;
    private static final int STATUS_BAR_TEXTURE_HEIGHT = 1024; // 64 * 16

    // Final message bar positioning
    private static final int FINAL_MESSAGE_X = 3;
    private static final int FINAL_MESSAGE_Y = 94;
    private static final int FINAL_MESSAGE_WIDTH = 218;
    private static final int FINAL_MESSAGE_HEIGHT = 16;
    
    // Button positioning (relative to GUI top-left corner)
    private static final int BTN_PICKAXE_X = 224; // 224 pixels next to GUI side
    private static final int BTN_PICKAXE_Y = 3; 
    private static final int BTN_HAMMER_X = 224;
    private static final int BTN_HAMMER_Y = 101; // 224 pixels next to GUI side
    private static final int BTN_WIDTH = 48;
    private static final int BTN_HEIGHT = 96;
    
    // Client-side state
    private final UUID sessionId;
    private final MiningCell[][] localGrid;
    /** Placed treasures: (treasureId, startX, startY) = matrix top-left, from server. */
    private final List<PlacedTreasureOrigin> placedTreasureOrigins = new ArrayList<>();
    private boolean usingHammer = false;
    private int currentStability;
    private int maxStability;
    private int treasureCount;
    private boolean gameEnded = false;
    private Component endMessage = null;
    private static final double SHAKE_DURATION_SEC = 3.0;
    private boolean shaking = false;
    private long shakeStartTime = 0;
    private Component pendingEndMessage = null;
    private int shakeOffsetX = 0;
    private int shakeOffsetY = 0;
    private static final long TREASURE_BLINK_MS = 500;
    private static final int TREASURE_BLINK_COUNT = 3;
    private final Map<PlacedTreasureOrigin, Long> blinkingTreasures = new HashMap<>();
    
    public UndergroundScreen(UndergroundScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        
        this.sessionId = handler.getSessionId();
        this.treasureCount = handler.getTreasureCount();
        this.currentStability = handler.getInitialStability();
        this.maxStability = handler.getInitialMaxStability();
        
        // Initialize local grid from serialized data
        this.localGrid = new MiningCell[MiningGrid.HEIGHT][MiningGrid.WIDTH];
        deserializeGrid(handler.getInitialGridData());
    }
    
    private void deserializeGrid(byte[] data) {
        if (data == null || data.length < MiningGrid.WIDTH * MiningGrid.HEIGHT * 2) {
            // Initialize with defaults if data is invalid
            for (int y = 0; y < MiningGrid.HEIGHT; y++) {
                for (int x = 0; x < MiningGrid.WIDTH; x++) {
                    localGrid[y][x] = new MiningCell(MiningCell.WallTier.TIER_3);
                }
            }
            return;
        }
        
        int index = 0;
        for (int y = 0; y < MiningGrid.HEIGHT; y++) {
            for (int x = 0; x < MiningGrid.WIDTH; x++) {
                int tier = data[index++] & 0xFF;
                int content = data[index++] & 0xFF;
                
                localGrid[y][x] = new MiningCell(MiningCell.WallTier.fromValue(tier));
                if (content < MiningCell.SecondLayerContent.values().length) {
                    localGrid[y][x].setSecondLayerContent(MiningCell.SecondLayerContent.values()[content]);
                }
            }
        }
        
        // Read placed treasures (startX, startY, treasureId) = matrix top-left
        placedTreasureOrigins.clear();
        if (index + 2 <= data.length) {
            int count = ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
            index += 2;
            for (int i = 0; i < count && index + 4 <= data.length; i++) {
                int startX = data[index++] & 0xFF;
                int startY = data[index++] & 0xFF;
                int utfLen = ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
                index += 2;
                if (index + utfLen <= data.length && utfLen > 0 && startX < MiningGrid.WIDTH && startY < MiningGrid.HEIGHT) {
                    String treasureId = new String(data, index, utfLen, java.nio.charset.StandardCharsets.UTF_8);
                    index += utfLen;
                    placedTreasureOrigins.add(new PlacedTreasureOrigin(treasureId, startX, startY));
                    TreasureDefinition def = TreasureRegistry.getById(treasureId);
                    if (def != null) {
                        boolean[][] shape = def.getShapeMatrix();
                        for (int row = 0; row < shape.length; row++) {
                            for (int col = 0; col < shape[row].length; col++) {
                                if (shape[row][col]) {
                                    int gx = startX + col;
                                    int gy = startY + row;
                                    if (gx < MiningGrid.WIDTH && gy < MiningGrid.HEIGHT) {
                                        localGrid[gy][gx].setTreasureId(treasureId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private record PlacedTreasureOrigin(String treasureId, int startX, int startY) {}
    
    @Override
    protected void init() {
        super.init();
        // Disable the title and inventory labels
        this.titleLabelX = -9999;
        this.titleLabelY = -9999;
        this.inventoryLabelX = -9999;
        this.inventoryLabelY = -9999;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (shaking) {
            double elapsed = (System.currentTimeMillis() - shakeStartTime) / 1000.0;
            if (elapsed >= SHAKE_DURATION_SEC) {
                shaking = false;
                gameEnded = true;
                endMessage = pendingEndMessage;
                pendingEndMessage = null;
                shakeOffsetX = 0;
                shakeOffsetY = 0;
            } else {
                float intensity = (float) (1.0 - elapsed / SHAKE_DURATION_SEC);
                shakeOffsetX = (int) ((ThreadLocalRandom.current().nextInt(7) - 3) * intensity);
                shakeOffsetY = (int) ((ThreadLocalRandom.current().nextInt(7) - 3) * intensity);
            }
        }
        super.render(graphics, mouseX, mouseY, delta);
        if (gameEnded && endMessage != null) {
            int guiX = (width - imageWidth) / 2;
            int guiY = (height - imageHeight) / 2;
            graphics.blit(FINAL_MESSAGE_TEXTURE,
                    guiX + FINAL_MESSAGE_X, guiY + FINAL_MESSAGE_Y,
                    0, 0, FINAL_MESSAGE_WIDTH, FINAL_MESSAGE_HEIGHT,
                    FINAL_MESSAGE_WIDTH, FINAL_MESSAGE_HEIGHT);
            int textWidth = font.width(endMessage);
            int textX = guiX + FINAL_MESSAGE_X + (FINAL_MESSAGE_WIDTH - textWidth) / 2;
            int textY = guiY + FINAL_MESSAGE_Y + (FINAL_MESSAGE_HEIGHT - font.lineHeight) / 2 + 1;
            graphics.drawString(font, endMessage, textX, textY, 0xFFFFFF, true);
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int guiX = (width - imageWidth) / 2 + shakeOffsetX;
        int guiY = (height - imageHeight) / 2 + shakeOffsetY;
        graphics.blit(GUI_TEXTURE, guiX, guiY, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        drawStabilityBar(graphics, guiX, guiY);
        drawSecondLayer(graphics, guiX, guiY);
        drawFirstLayer(graphics, guiX, guiY);
        if (!gameEnded) {
            drawToolPreview(graphics, guiX, guiY, mouseX, mouseY);
        }
        drawToolButtons(graphics, guiX, guiY, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component treasureLabel = Component.translatable("gui.cobblesafari.underground.treasures_count", treasureCount);
        int textWidth = font.width(treasureLabel);
        graphics.drawString(font, treasureLabel, (imageWidth - textWidth) / 2, 3, 0xFFFFFF, true);
    }
    
    private void drawStabilityBar(GuiGraphics graphics, int guiX, int guiY) {
        // Frame index 0 = full (Y=0), 63 = empty (Y=63*16)
        int frameIndex;
        if (maxStability <= 0) {
            frameIndex = STATUS_BAR_FRAME_COUNT - 1;
        } else {
            int level = (int) (STATUS_BAR_FRAME_COUNT * (float) currentStability / maxStability);
            level = (int) Math.clamp(level, 0, STATUS_BAR_FRAME_COUNT - 1);
            frameIndex = currentStability >= maxStability ? 0 : (STATUS_BAR_FRAME_COUNT - 1 - level);
        }
        int srcY = frameIndex * STATUS_BAR_HEIGHT;
        graphics.blit(STATUS_BAR_TEXTURE,
                guiX + STATUS_BAR_X, guiY + STATUS_BAR_Y,
                0, srcY, STATUS_BAR_WIDTH, STATUS_BAR_HEIGHT,
                STATUS_BAR_WIDTH, STATUS_BAR_TEXTURE_HEIGHT);
    }
    
    private void drawSecondLayer(GuiGraphics graphics, int guiX, int guiY) {
        // Draw layer 2 (treasures and iron) ALWAYS - transparency is handled by Tier 1 texture itself
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // First pass: draw iron blocks
        for (int gridY = 0; gridY < MiningGrid.HEIGHT; gridY++) {
            for (int gridX = 0; gridX < MiningGrid.WIDTH; gridX++) {
                MiningCell cell = localGrid[gridY][gridX];
                
                if (cell.isIronBlock()) {
                    int screenX = guiX + GRID_OFFSET_X + gridX * CELL_SIZE;
                    int screenY = guiY + GRID_OFFSET_Y + gridY * CELL_SIZE;
                    
                    // Draw iron block with connected texture (letters sorted to match filenames: e, n, s, w → ens, ensw, etc.)
                    String connections = getIronConnections(gridX, gridY);
                    ResourceLocation ironTexture = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                            "textures/gui/underground/iron_" + connections + ".png");
                    
                    graphics.blit(ironTexture, screenX, screenY, 
                            0, 0, CELL_SIZE, CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }
        
        // Second pass: draw treasures from stored origins (matrix top-left)
        long now = System.currentTimeMillis();
        for (PlacedTreasureOrigin origin : placedTreasureOrigins) {
            int screenX = guiX + GRID_OFFSET_X + origin.startX() * CELL_SIZE;
            int screenY = guiY + GRID_OFFSET_Y + origin.startY() * CELL_SIZE;
            ResourceLocation treasureTexture = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                    "textures/gui/underground/item_" + origin.treasureId() + ".png");
            int[] size = getTreasureSize(origin.treasureId());
            int texWidth = size[0] * CELL_SIZE;
            int texHeight = size[1] * CELL_SIZE;
            Long blinkStart = blinkingTreasures.get(origin);
            boolean flashOn = false;
            if (blinkStart != null) {
                long elapsed = now - blinkStart;
                if (elapsed < TREASURE_BLINK_MS) {
                    long period = TREASURE_BLINK_MS / TREASURE_BLINK_COUNT;
                    flashOn = (elapsed % period) < (period / 2);
                }
            }
            if (flashOn) {
                RenderSystem.setShaderColor(1.5f, 1.5f, 1.5f, 1.0f);
            }
            graphics.blit(treasureTexture, screenX, screenY,
                    0, 0, texWidth, texHeight, texWidth, texHeight);
            if (flashOn) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        blinkingTreasures.entrySet().removeIf(e -> (now - e.getValue()) >= TREASURE_BLINK_MS);
    }
    
    /** Iron connection string in NWSE order (North, West, South, East) for texture filenames. */
    private String getIronConnections(int x, int y) {
        StringBuilder sb = new StringBuilder();
        if (y > 0 && localGrid[y - 1][x].isIronBlock()) sb.append("n");
        if (x > 0 && localGrid[y][x - 1].isIronBlock()) sb.append("w");
        if (y < MiningGrid.HEIGHT - 1 && localGrid[y + 1][x].isIronBlock()) sb.append("s");
        if (x < MiningGrid.WIDTH - 1 && localGrid[y][x + 1].isIronBlock()) sb.append("e");
        return sb.isEmpty() ? "none" : sb.toString();
    }
    
    /**
     * Get treasure dimensions from the registry's shape matrix.
     */
    private int[] getTreasureSize(String treasureId) {
        TreasureDefinition def = TreasureRegistry.getById(treasureId);
        if (def != null) {
            return new int[]{def.getWidth(), def.getHeight()};
        }
        return new int[]{1, 1};
    }

    private void drawFirstLayer(GuiGraphics graphics, int guiX, int guiY) {
        // Draw wall tiers - no transparency manipulation, texture handles it
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        for (int gridY = 0; gridY < MiningGrid.HEIGHT; gridY++) {
            for (int gridX = 0; gridX < MiningGrid.WIDTH; gridX++) {
                MiningCell cell = localGrid[gridY][gridX];
                
                int tier = cell.getCurrentTier().getValue();
                
                // Skip if fully revealed (Tier 0)
                if (tier == 0) {
                    continue;
                }
                
                int screenX = guiX + GRID_OFFSET_X + gridX * CELL_SIZE;
                int screenY = guiY + GRID_OFFSET_Y + gridY * CELL_SIZE;
                
                ResourceLocation stoneTexture = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                        "textures/gui/underground/stone_tier_" + tier + ".png");
                
                graphics.blit(stoneTexture, screenX, screenY,
                        0, 0, CELL_SIZE, CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }
    
    private void drawToolPreview(GuiGraphics graphics, int guiX, int guiY, int mouseX, int mouseY) {
        int gridStartX = guiX + GRID_OFFSET_X;
        int gridStartY = guiY + GRID_OFFSET_Y;
        
        // Check if mouse is over the grid
        if (mouseX < gridStartX || mouseX >= gridStartX + MiningGrid.WIDTH * CELL_SIZE ||
            mouseY < gridStartY || mouseY >= gridStartY + MiningGrid.HEIGHT * CELL_SIZE) {
            return;
        }
        
        int hoveredX = (mouseX - gridStartX) / CELL_SIZE;
        int hoveredY = (mouseY - gridStartY) / CELL_SIZE;
        
        if (usingHammer) {
            // 3x3 hammer pattern
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int gx = hoveredX + dx;
                    int gy = hoveredY + dy;
                    
                    if (gx >= 0 && gx < MiningGrid.WIDTH && gy >= 0 && gy < MiningGrid.HEIGHT) {
                        boolean isCorner = (dx != 0 && dy != 0);
                        ResourceLocation preview = isCorner ? PREVIEW_1DMG : PREVIEW_2DMG;
                        
                        int screenX = gridStartX + gx * CELL_SIZE;
                        int screenY = gridStartY + gy * CELL_SIZE;
                        
                        graphics.blit(preview, screenX, screenY,
                                0, 0, CELL_SIZE, CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    }
                }
            }
        } else {
            // + pickaxe pattern
            // Center - 2 damage
            drawPreviewCell(graphics, gridStartX, gridStartY, hoveredX, hoveredY, PREVIEW_2DMG);
            // Cardinals - 1 damage
            drawPreviewCell(graphics, gridStartX, gridStartY, hoveredX, hoveredY - 1, PREVIEW_1DMG);
            drawPreviewCell(graphics, gridStartX, gridStartY, hoveredX, hoveredY + 1, PREVIEW_1DMG);
            drawPreviewCell(graphics, gridStartX, gridStartY, hoveredX - 1, hoveredY, PREVIEW_1DMG);
            drawPreviewCell(graphics, gridStartX, gridStartY, hoveredX + 1, hoveredY, PREVIEW_1DMG);
        }
    }
    
    private void drawPreviewCell(GuiGraphics graphics, int gridStartX, int gridStartY, 
                                  int gx, int gy, ResourceLocation texture) {
        if (gx >= 0 && gx < MiningGrid.WIDTH && gy >= 0 && gy < MiningGrid.HEIGHT) {
            int screenX = gridStartX + gx * CELL_SIZE;
            int screenY = gridStartY + gy * CELL_SIZE;
            
            graphics.blit(texture, screenX, screenY,
                    0, 0, CELL_SIZE, CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }
    }
    
    private void drawToolButtons(GuiGraphics graphics, int guiX, int guiY, int mouseX, int mouseY) {
        // Pickaxe button
        ResourceLocation pickaxeTexture = usingHammer ? BTN_PICKAXE : BTN_PICKAXE_SELECTED;
        graphics.blit(pickaxeTexture, 
                guiX + BTN_PICKAXE_X, guiY + BTN_PICKAXE_Y,
                0, 0, BTN_WIDTH, BTN_HEIGHT, BTN_WIDTH, BTN_HEIGHT);
        
        // Hammer button
        ResourceLocation hammerTexture = usingHammer ? BTN_HAMMER_SELECTED : BTN_HAMMER;
        graphics.blit(hammerTexture,
                guiX + BTN_HAMMER_X, guiY + BTN_HAMMER_Y,
                0, 0, BTN_WIDTH, BTN_HEIGHT, BTN_WIDTH, BTN_HEIGHT);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || gameEnded) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        int guiX = (width - imageWidth) / 2;
        int guiY = (height - imageHeight) / 2;
        
        // Check tool button clicks
        if (isInButton(mouseX, mouseY, guiX + BTN_PICKAXE_X, guiY + BTN_PICKAXE_Y, BTN_WIDTH, BTN_HEIGHT)) {
            if (usingHammer) {
                usingHammer = false;
                Services.PLATFORM.sendPayloadToServer(new UndergroundPayloads.SwitchToolPayload(sessionId, false));
                playClickSound();
            }
            return true;
        }
        
        if (isInButton(mouseX, mouseY, guiX + BTN_HAMMER_X, guiY + BTN_HAMMER_Y, BTN_WIDTH, BTN_HEIGHT)) {
            if (!usingHammer) {
                usingHammer = true;
                Services.PLATFORM.sendPayloadToServer(new UndergroundPayloads.SwitchToolPayload(sessionId, true));
                playClickSound();
            }
            return true;
        }
        
        // Check grid click
        int gridStartX = guiX + GRID_OFFSET_X;
        int gridStartY = guiY + GRID_OFFSET_Y;
        
        if (mouseX >= gridStartX && mouseX < gridStartX + MiningGrid.WIDTH * CELL_SIZE &&
            mouseY >= gridStartY && mouseY < gridStartY + MiningGrid.HEIGHT * CELL_SIZE) {
            
            int cellX = (int) (mouseX - gridStartX) / CELL_SIZE;
            int cellY = (int) (mouseY - gridStartY) / CELL_SIZE;
            
            // Send mine action to server
            Services.PLATFORM.sendPayloadToServer(new UndergroundPayloads.MineActionPayload(sessionId, cellX, cellY));
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean isInButton(double mouseX, double mouseY, int btnX, int btnY, int btnW, int btnH) {
        return mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
    }
    
    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }
    
    // === Network update handlers ===
    
    public void onGridUpdate(UndergroundPayloads.GridUpdatePayload payload) {
        for (UndergroundPayloads.CellUpdateData update : payload.updates()) {
            if (update.x() >= 0 && update.x() < MiningGrid.WIDTH &&
                update.y() >= 0 && update.y() < MiningGrid.HEIGHT) {
                
                MiningCell cell = localGrid[update.y()][update.x()];
                cell.setCurrentTier(MiningCell.WallTier.fromValue(update.newTier()));
                
                if (update.secondLayerContent() >= 0 && 
                    update.secondLayerContent() < MiningCell.SecondLayerContent.values().length) {
                    cell.setSecondLayerContent(MiningCell.SecondLayerContent.values()[update.secondLayerContent()]);
                }
                
                if (update.treasureId() != null) {
                    cell.setTreasureId(update.treasureId());
                }
            }
        }
    }
    
    public void onStabilityUpdate(UndergroundPayloads.StabilityUpdatePayload payload) {
        this.currentStability = payload.current();
        this.maxStability = payload.max();
    }
    
    public void onTreasureRevealed(UndergroundPayloads.TreasureRevealedPayload payload) {
        blinkingTreasures.put(
                new PlacedTreasureOrigin(payload.treasureId(), payload.startX(), payload.startY()),
                System.currentTimeMillis());
    }
    
    public void onGameEnd(UndergroundPayloads.GameEndPayload payload) {
        Component msg = payload.wallCollapsed()
                ? Component.translatable("gui.cobblesafari.underground.end_collapsed", payload.treasuresCollected(), payload.totalTreasures())
                : Component.translatable("gui.cobblesafari.underground.end_dug", payload.totalTreasures());
        if (payload.wallCollapsed()) {
            this.pendingEndMessage = msg;
            this.shaking = true;
            this.shakeStartTime = System.currentTimeMillis();
        } else {
            this.gameEnded = true;
            this.endMessage = msg;
        }
    }
    
    public void playSound(String soundType) {
        if (minecraft == null || minecraft.player == null) return;
        
        switch (soundType) {
            case "stone_hit" -> minecraft.player.playSound(SoundEvents.STONE_HIT, 0.5f, 1.0f);
            case "iron_hit" -> minecraft.player.playSound(SoundEvents.ANVIL_LAND, 0.3f, 1.5f);
            case "treasure_found" -> minecraft.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.5f);
            case "wall_collapse" -> minecraft.player.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 0.8f);
            case "perfect_clear" -> minecraft.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }
    
    // Getters for network handlers
    public UUID getSessionId() {
        return sessionId;
    }
}
