package maxigregrze.cobblesafari.block.basepc;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SecretBasePCConfig;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BasePCMenu extends AbstractContainerMenu {

    public static final TagKey<Item> FLAG_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "flag"));

    public static MenuType<BasePCMenu> MENU_TYPE;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Container flagContainer;

    private static final int FLAG_SLOT_INDEX = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    public BasePCMenu(int syncId, Inventory inv) {
        this(syncId, inv, new SimpleContainerData(4), ContainerLevelAccess.NULL);
    }

    public BasePCMenu(int syncId, Inventory inv, ContainerData data, ContainerLevelAccess access) {
        super(MENU_TYPE, syncId);
        this.data = data;
        this.access = access;

        this.flagContainer = new SimpleContainer(1) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return stack.is(FLAG_TAG);
            }
        };

        addSlot(new FlagSlot(flagContainer, 0, 128, 145));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 56 + col * 18, 170 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 56 + col * 18, 228));
        }

        addDataSlots(data);
    }

    public ContainerData getData() {
        return data;
    }

    public static boolean wouldUpgrade(int currentRank, Item flagItem) {
        if (flagItem == ModItems.FLAG_BRONZE && currentRank <= 0) return true;
        if (flagItem == ModItems.FLAG_SILVER && currentRank <= 1) return true;
        if (flagItem == ModItems.FLAG_GOLD && currentRank <= 2) return true;
        if (flagItem == ModItems.FLAG_PLATINUM && currentRank <= 3) return true;
        if (flagItem == ModItems.FLAG_CREATIVE && currentRank <= 4) return true;
        return false;
    }

    private static int getFlagBatteryValue(Item flagItem) {
        if (flagItem == ModItems.FLAG_REGULAR) return SecretBasePCConfig.getFlagBatteryValue("regular");
        if (flagItem == ModItems.FLAG_BRONZE) return SecretBasePCConfig.getFlagBatteryValue("bronze");
        if (flagItem == ModItems.FLAG_SILVER) return SecretBasePCConfig.getFlagBatteryValue("silver");
        if (flagItem == ModItems.FLAG_GOLD) return SecretBasePCConfig.getFlagBatteryValue("gold");
        if (flagItem == ModItems.FLAG_PLATINUM) return SecretBasePCConfig.getFlagBatteryValue("platinum");
        return 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        switch (buttonId) {
            case 0 -> {
                int effect = data.get(1);
                if (effect > 0) {
                    data.set(1, effect - 1);
                    data.set(3, 0);
                }
                return true;
            }
            case 1 -> {
                int effect = data.get(1);
                if (effect < 4) {
                    data.set(1, effect + 1);
                    data.set(3, 0);
                }
                return true;
            }
            case 2 -> {
                int currentRank = data.get(0);
                int currentEffect = data.get(1);
                if (BasePCBlockEntity.isEffectLocked(currentRank, currentEffect)) {
                    return false;
                }
                int cost = BasePCBlockEntity.getEffectCost(currentRank, currentEffect);
                boolean currentlyActive = data.get(3) != 0;
                if (currentlyActive) {
                    data.set(3, 0);
                    return true;
                }
                if (cost == 0) {
                    data.set(3, 1);
                    return true;
                }
                int battery = data.get(2);
                if (battery < cost) {
                    return false;
                }
                data.set(3, 1);
                access.execute((level, pos) -> {
                    if (level.getBlockEntity(pos) instanceof BasePCBlockEntity be) {
                        be.applyEffectOnce();
                    }
                });
                return true;
            }
            case 3 -> {
                return consumeFlag();
            }
            default -> { return false; }
        }
    }

    private boolean consumeFlag() {
        ItemStack flagStack = flagContainer.getItem(0);
        if (flagStack.isEmpty()) return false;

        int currentRank = data.get(0);
        int currentBat = data.get(2);
        int maxBat = BasePCBlockEntity.getMaxBattery(currentRank);
        if (currentRank >= 5 && currentBat >= maxBat) return false;

        Item flagItem = flagStack.getItem();
        if (flagItem == ModItems.FLAG_CREATIVE) {
            data.set(0, 5);
            data.set(2, BasePCBlockEntity.getMaxBattery(5));
            flagStack.shrink(1);
            return true;
        }

        boolean wouldUpgrade = wouldUpgrade(currentRank, flagItem);
        if (!wouldUpgrade && currentBat >= maxBat) return false;

        int rechargeAmount = getFlagBatteryValue(flagItem);
        if (flagItem == ModItems.FLAG_BRONZE && currentRank <= 0) {
            data.set(0, 1);
            currentRank = 1;
        } else if (flagItem == ModItems.FLAG_SILVER && currentRank <= 1) {
            data.set(0, 2);
            currentRank = 2;
        } else if (flagItem == ModItems.FLAG_GOLD && currentRank <= 2) {
            data.set(0, 3);
            currentRank = 3;
        } else if (flagItem == ModItems.FLAG_PLATINUM && currentRank <= 3) {
            data.set(0, 4);
            currentRank = 4;
        }

        maxBat = BasePCBlockEntity.getMaxBattery(currentRank);
        if (currentBat >= maxBat) return false;

        data.set(2, Math.min(currentBat + rechargeAmount, maxBat));
        flagStack.shrink(1);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index == FLAG_SLOT_INDEX) {
                if (!moveItemStackTo(slotStack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotStack.is(FLAG_TAG)) {
                if (!moveItemStackTo(slotStack, FLAG_SLOT_INDEX, FLAG_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
                if (!moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                if (!moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.flagContainer));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.SECRETBASE_PC);
    }


    private static class FlagSlot extends Slot {
        public FlagSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(FLAG_TAG);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
