package maxigregrze.cobblesafari.compat.wthit;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class BasePCWTHITComponent implements IBlockComponentProvider {
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
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getData().raw();
        if (data == null) return;

        int rankIdx = data.getInt("rank");
        if (rankIdx >= 0 && rankIdx < RANK_KEYS.length) {
            tooltip.addLine(Component.translatable(RANK_KEYS[rankIdx]));
        }

        int battery = data.getInt("battery");
        int maxBattery = data.getInt("max_battery");
        int percent = maxBattery > 0 ? Math.min(100, (battery * 100) / maxBattery) : 0;
        tooltip.addLine(Component.translatable("cobblesafari.waila.basepc.battery", percent));

        boolean active = data.getBoolean("active");
        if (active) {
            int effectIdx = data.getInt("effect");
            String effectKey = effectIdx >= 0 && effectIdx < EFFECT_KEYS.length ? EFFECT_KEYS[effectIdx] : EFFECT_KEYS[0];
            tooltip.addLine(Component.translatable("cobblesafari.waila.basepc.status_active", Component.translatable(effectKey)));
        } else {
            tooltip.addLine(Component.translatable("cobblesafari.waila.basepc.status_inactive"));
        }
    }
}
