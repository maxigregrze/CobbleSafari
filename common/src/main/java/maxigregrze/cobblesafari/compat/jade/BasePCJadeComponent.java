package maxigregrze.cobblesafari.compat.jade;

import maxigregrze.cobblesafari.block.basepc.BasePCBlock;
import maxigregrze.cobblesafari.block.basepc.BasePCBlockEntity;
import maxigregrze.cobblesafari.block.BlockPart;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum BasePCJadeComponent implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String[] RANK_KEYS = {
            "tooltip.cobblesafari.basepc.rank.regular",
            "tooltip.cobblesafari.basepc.rank.bronze",
            "tooltip.cobblesafari.basepc.rank.silver",
            "tooltip.cobblesafari.basepc.rank.gold",
            "tooltip.cobblesafari.basepc.rank.platinum",
            "tooltip.cobblesafari.basepc.rank.creative"
    };

    private static final String[] EFFECT_KEYS = {
            "gui.cobblesafari.basepc.effect.repel",
            "gui.cobblesafari.basepc.effect.uncommon_boost",
            "gui.cobblesafari.basepc.effect.rare_boost",
            "gui.cobblesafari.basepc.effect.ultra_rare_boost",
            "gui.cobblesafari.basepc.effect.shiny_boost"
    };

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null) return;

        int rankIdx = data.getInt("rank");
        if (rankIdx >= 0 && rankIdx < RANK_KEYS.length) {
            tooltip.add(Component.translatable(RANK_KEYS[rankIdx]));
        }

        int battery = data.getInt("battery");
        int maxBattery = data.getInt("max_battery");
        int percent = maxBattery > 0 ? Math.min(100, (battery * 100) / maxBattery) : 0;
        tooltip.add(Component.translatable("cobblesafari.waila.basepc.battery", percent));

        boolean active = data.getBoolean("active");
        if (active) {
            int effectIdx = data.getInt("effect");
            String effectKey = effectIdx >= 0 && effectIdx < EFFECT_KEYS.length ? EFFECT_KEYS[effectIdx] : EFFECT_KEYS[0];
            tooltip.add(Component.translatable("cobblesafari.waila.basepc.status_active", Component.translatable(effectKey)));
        } else {
            tooltip.add(Component.translatable("cobblesafari.waila.basepc.status_inactive"));
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockState state = accessor.getBlockState();
        if (!state.is(ModBlocks.SECRETBASE_PC)) return;

        BasePCBlockEntity be = resolveBlockEntity(accessor);
        if (be == null) return;

        int rank = be.getRank();
        int r = Math.min(rank, 5);
        tag.putInt("rank", r);
        tag.putInt("battery", be.getBattery());
        tag.putInt("max_battery", BasePCBlockEntity.getMaxBattery(rank));
        tag.putBoolean("active", be.isActive());
        tag.putInt("effect", be.getCurrentEffect());
    }

    private static BasePCBlockEntity resolveBlockEntity(BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof BasePCBlockEntity be) {
            return be;
        }
        BlockState state = accessor.getBlockState();
        BlockPos pos = accessor.getPosition();
        BlockPos center = getCenterPos(pos, state);
        if (accessor.getLevel().getBlockEntity(center) instanceof BasePCBlockEntity be) {
            return be;
        }
        return null;
    }

    private static BlockPos getCenterPos(BlockPos pos, BlockState state) {
        if (state.getValue(BasePCBlock.PART) == BlockPart.CENTER) {
            return pos;
        }
        Direction facing = state.getValue(BasePCBlock.FACING);
        if (state.getValue(BasePCBlock.PART) == BlockPart.TOP) {
            return pos.below();
        }
        return pos.relative(facing.getClockWise());
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "base_pc");
    }
}
