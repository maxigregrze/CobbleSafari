package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TimerSyncPayload(String dimensionId, int remainingTicks, boolean active, boolean bypassed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TimerSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "timer_sync"));

    public static final StreamCodec<FriendlyByteBuf, TimerSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            TimerSyncPayload::dimensionId,
            ByteBufCodecs.INT,
            TimerSyncPayload::remainingTicks,
            ByteBufCodecs.BOOL,
            TimerSyncPayload::active,
            ByteBufCodecs.BOOL,
            TimerSyncPayload::bypassed,
            TimerSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public String getFormattedTime() {
        if (bypassed) {
            return "XX:XX";
        }
        int totalSeconds = remainingTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
