package maxigregrze.cobblesafari.underground.screen;

import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Screen handler for the Underground Mining minigame.
 */
public class UndergroundScreenHandler extends AbstractContainerMenu {
    
    private final UUID sessionId;
    private final int treasureCount;
    private final byte[] initialGridData;
    private final int initialStability;
    private final int initialMaxStability;
    
    // Client constructor (from open data)
    public UndergroundScreenHandler(int syncId, Inventory inv, UndergroundOpenData data) {
        super(UndergroundMinigame.MENU_TYPE, syncId);
        this.sessionId = data.sessionId();
        this.treasureCount = data.treasureCount();
        this.initialGridData = data.gridData();
        this.initialStability = data.currentStability();
        this.initialMaxStability = data.maxStability();
    }
    
    // Server constructor
    public UndergroundScreenHandler(int syncId, Inventory inv, UUID sessionId, 
                                    int treasureCount, byte[] gridData, 
                                    int currentStability, int maxStability) {
        super(UndergroundMinigame.MENU_TYPE, syncId);
        this.sessionId = sessionId;
        this.treasureCount = treasureCount;
        this.initialGridData = gridData;
        this.initialStability = currentStability;
        this.initialMaxStability = maxStability;
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        
        if (!player.level().isClientSide) {
            // End the session when the screen is closed
            UndergroundMinigame.onScreenClosed(sessionId, player);
        }
    }
    
    // Getters
    public UUID getSessionId() {
        return sessionId;
    }
    
    public int getTreasureCount() {
        return treasureCount;
    }
    
    public byte[] getInitialGridData() {
        return initialGridData;
    }
    
    public int getInitialStability() {
        return initialStability;
    }
    
    public int getInitialMaxStability() {
        return initialMaxStability;
    }
}
