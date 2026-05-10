package maxigregrze.cobblesafari.item.donut;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum DonutMainFlavor implements StringRepresentable {
    SPICY("spicy", 0, Stats.ATTACK),
    DRY("dry", 1, Stats.SPECIAL_ATTACK),
    SWEET("sweet", 2, Stats.DEFENCE),
    SOUR("sour", 3, Stats.SPECIAL_DEFENCE),
    BITTER("bitter", 4, Stats.SPEED),
    MIX("mix", 5, Stats.HP);

    private final String name;
    private final int index;
    private final Stat stat;

    DonutMainFlavor(String name, int index, Stat stat) {
        this.name = name;
        this.index = index;
        this.stat = stat;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public Stat getStat() {
        return stat;
    }

    public static DonutMainFlavor byIndex(int idx) {
        for (DonutMainFlavor f : values()) {
            if (f.index == idx) {
                return f;
            }
        }
        return SPICY;
    }

    public static final IntFunction<DonutMainFlavor> BY_INDEX = DonutMainFlavor::byIndex;

    public static final StringRepresentable.EnumCodec<DonutMainFlavor> CODEC =
            StringRepresentable.fromEnum(DonutMainFlavor::values);

    public static final StreamCodec<ByteBuf, DonutMainFlavor> STREAM_CODEC =
            ByteBufCodecs.idMapper(DonutMainFlavor::byIndex, DonutMainFlavor::getIndex);
}
