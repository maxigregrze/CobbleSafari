package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LostNotesBlockEntity extends BlockEntity {

    private static final String NBT_BOOK = "StoredBook";
    private ItemStack storedBook = ItemStack.EMPTY;

    public LostNotesBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOST_NOTES, pos, state);
    }

    public boolean hasBook() {
        return !this.storedBook.isEmpty();
    }

    public ItemStack getStoredBookCopy() {
        return this.storedBook.copy();
    }

    public void setStoredBook(ItemStack stack) {
        this.storedBook = stack.copyWithCount(1);
        this.setChanged();
    }

    public void clearStoredBook() {
        this.storedBook = ItemStack.EMPTY;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.storedBook.isEmpty()) {
            tag.put(NBT_BOOK, this.storedBook.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_BOOK)) {
            this.storedBook = ItemStack.parseOptional(registries, tag.getCompound(NBT_BOOK));
        } else {
            this.storedBook = ItemStack.EMPTY;
        }
    }
}
