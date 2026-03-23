package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DistortionStoneBricksRuneBlockEntity extends BlockEntity {

    private static final String NBT_RUNE_TEXT = "RuneText";
    private String runeText = "";

    public DistortionStoneBricksRuneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISTORTION_STONEBRICKS_RUNE, pos, state);
    }

    public String getRuneText() {
        return runeText;
    }

    public void setRuneText(String runeText) {
        this.runeText = runeText == null ? "" : runeText;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(NBT_RUNE_TEXT, this.runeText);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.runeText = tag.getString(NBT_RUNE_TEXT);
    }
}
