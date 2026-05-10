package maxigregrze.cobblesafari.item.donut;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DonutBonus(String powerId, int level, int type) {

    public static final Codec<DonutBonus> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("id").forGetter(DonutBonus::powerId),
            Codec.INT.fieldOf("level").forGetter(DonutBonus::level),
            Codec.INT.fieldOf("type").forGetter(DonutBonus::type)
    ).apply(inst, DonutBonus::new));

    public static final StreamCodec<ByteBuf, DonutBonus> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            DonutBonus::powerId,
            ByteBufCodecs.VAR_INT,
            DonutBonus::level,
            ByteBufCodecs.VAR_INT,
            DonutBonus::type,
            DonutBonus::new
    );
}
