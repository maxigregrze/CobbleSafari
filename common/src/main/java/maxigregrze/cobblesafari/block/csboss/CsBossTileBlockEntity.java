package maxigregrze.cobblesafari.block.csboss;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Tuile de sol CSBoss (retours plan 199) : retient la décoration sans collision qu'elle a remplacée
 * (tas de feuilles, herbe, tapis de fleurs…) ou le bloc mimic caché pendant le combat (état + données
 * de son block entity), et la remet en place quand la tuile s'éteint. Stocké avec le tick programmé du
 * bloc, donc un déchargement de chunk ou un redémarrage ne la perd pas. Les bascules d'arène
 * ({@code ArenaBlockScanner.setBattleState}) restaurent d'abord une tuile posée sur un bloc réactif.
 */
public class CsBossTileBlockEntity extends BlockEntity {

    private static final String KEY_REPLACED = "Replaced";
    private static final String KEY_REPLACED_DATA = "ReplacedData";

    @Nullable
    private BlockState replaced;

    /** Données du block entity remplacé (bloc mimic), {@code null} pour une simple décoration. */
    @Nullable
    private CompoundTag replacedData;

    public CsBossTileBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CSBOSS_TILE, pos, state);
    }

    protected CsBossTileBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setReplaced(@Nullable BlockState state, @Nullable CompoundTag data) {
        this.replaced = state == null || state.isAir() ? null : state;
        this.replacedData = this.replaced == null ? null : data;
        setChanged();
    }

    /** Retire la tuile en {@code pos} et rend ce qu'elle avait remplacé (de l'air sinon). */
    public static void restore(ServerLevel level, BlockPos pos) {
        CsBossTileBlockEntity tile = level.getBlockEntity(pos) instanceof CsBossTileBlockEntity be ? be : null;
        BlockState under = tile != null ? tile.replaced : null;
        CompoundTag data = tile != null ? tile.replacedData : null;
        if (under != null && (data != null || under.canSurvive(level, pos))) {
            level.setBlock(pos, under, Block.UPDATE_ALL);
            BlockEntity restored = data != null ? level.getBlockEntity(pos) : null;
            if (restored != null) {
                restored.loadWithComponents(data, level.registryAccess());
                restored.setChanged();
            }
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (replaced != null) {
            tag.put(KEY_REPLACED, NbtUtils.writeBlockState(replaced));
        }
        if (replacedData != null) {
            tag.put(KEY_REPLACED_DATA, replacedData.copy());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.replaced = tag.contains(KEY_REPLACED, Tag.TAG_COMPOUND)
                ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound(KEY_REPLACED))
                : null;
        this.replacedData = tag.contains(KEY_REPLACED_DATA, Tag.TAG_COMPOUND) ? tag.getCompound(KEY_REPLACED_DATA) : null;
    }
}
