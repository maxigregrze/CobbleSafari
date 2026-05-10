package maxigregrze.cobblesafari.item.donut;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record DonutFlavorComponent(
        DonutMainFlavor flavor,
        int tier,
        List<ResourceLocation> inputBerries,
        int calories,
        List<DonutBonus> bonuses
) {

    public static final int MAX_TIER = 5;

    public DonutFlavorComponent {
        if (tier < 0 || tier > MAX_TIER) {
            throw new IllegalArgumentException("tier out of range: " + tier);
        }
        if (inputBerries.size() > 3) {
            throw new IllegalArgumentException("at most 3 input berries");
        }
        if (bonuses.size() > 3) {
            throw new IllegalArgumentException("at most 3 bonuses");
        }
        inputBerries = List.copyOf(inputBerries);
        bonuses = List.copyOf(bonuses);
    }

    public DonutFlavorComponent(DonutMainFlavor flavor, int tier) {
        this(flavor, tier, List.of(), 0, List.of());
    }

    public static final Codec<DonutFlavorComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    DonutMainFlavor.CODEC.fieldOf("flavor").forGetter(DonutFlavorComponent::flavor),
                    Codec.intRange(0, MAX_TIER).fieldOf("tier").forGetter(DonutFlavorComponent::tier),
                    ResourceLocation.CODEC.listOf()
                            .optionalFieldOf("input_berries", List.of())
                            .forGetter(DonutFlavorComponent::inputBerries),
                    Codec.INT.optionalFieldOf("calories", 0).forGetter(DonutFlavorComponent::calories),
                    DonutBonus.CODEC.listOf()
                            .optionalFieldOf("bonuses", List.of())
                            .forGetter(DonutFlavorComponent::bonuses)
            ).apply(instance, DonutFlavorComponent::new)
    );

    private static final StreamCodec<ByteBuf, ResourceLocation> RL_STREAM = ResourceLocation.STREAM_CODEC;
    private static final StreamCodec<ByteBuf, List<ResourceLocation>> BERRIES_STREAM =
            RL_STREAM.apply(ByteBufCodecs.list(3));
    private static final StreamCodec<ByteBuf, List<DonutBonus>> BONUSES_STREAM =
            DonutBonus.STREAM_CODEC.apply(ByteBufCodecs.list(3));

    public static final StreamCodec<ByteBuf, DonutFlavorComponent> STREAM_CODEC = StreamCodec.composite(
            DonutMainFlavor.STREAM_CODEC,
            DonutFlavorComponent::flavor,
            ByteBufCodecs.VAR_INT,
            DonutFlavorComponent::tier,
            BERRIES_STREAM,
            DonutFlavorComponent::inputBerries,
            ByteBufCodecs.VAR_INT,
            DonutFlavorComponent::calories,
            BONUSES_STREAM,
            DonutFlavorComponent::bonuses,
            DonutFlavorComponent::new
    );
}
