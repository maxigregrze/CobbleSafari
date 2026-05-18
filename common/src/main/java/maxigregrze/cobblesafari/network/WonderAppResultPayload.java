package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record WonderAppResultPayload(
        int subscreen,
        int ticketsRemaining,
        long nextResetEpochSeconds,
        boolean hasEvent,
        String eventName,
        String customBannerName,
        int eventDaysLeft,
        List<EventPoolEntry> eventPool,
        CompoundTag offeredNbt,
        CompoundTag receivedNbt,
        String errorKey
) implements CustomPacketPayload {

    public static final int SUB_BEGIN = 0;
    public static final int SUB_TRADE = 1;
    public static final int SUB_ERROR = 2;

    public record EventPoolEntry(String groupId, int weight) {}

    public static final CustomPacketPayload.Type<WonderAppResultPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "wonder_app_result"));

    public static final StreamCodec<FriendlyByteBuf, WonderAppResultPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.subscreen());
                buf.writeVarInt(p.ticketsRemaining());
                buf.writeLong(p.nextResetEpochSeconds());
                buf.writeBoolean(p.hasEvent());
                buf.writeUtf(p.eventName() == null ? "" : p.eventName());
                buf.writeUtf(p.customBannerName() == null ? "" : p.customBannerName());
                buf.writeVarInt(p.eventDaysLeft());
                List<EventPoolEntry> pool = p.eventPool() == null ? List.of() : p.eventPool();
                buf.writeVarInt(pool.size());
                for (EventPoolEntry e : pool) {
                    buf.writeUtf(e.groupId());
                    buf.writeVarInt(e.weight());
                }
                buf.writeNbt(p.offeredNbt() == null ? new CompoundTag() : p.offeredNbt());
                buf.writeNbt(p.receivedNbt() == null ? new CompoundTag() : p.receivedNbt());
                buf.writeUtf(p.errorKey() == null ? "" : p.errorKey());
            },
            buf -> {
                int sub = buf.readVarInt();
                int tickets = buf.readVarInt();
                long resetEpoch = buf.readLong();
                boolean hasEvent = buf.readBoolean();
                String eventName = buf.readUtf();
                String customBanner = buf.readUtf();
                int daysLeft = buf.readVarInt();
                int poolSize = buf.readVarInt();
                List<EventPoolEntry> pool = new ArrayList<>(poolSize);
                for (int i = 0; i < poolSize; i++) {
                    pool.add(new EventPoolEntry(buf.readUtf(), buf.readVarInt()));
                }
                CompoundTag offered = buf.readNbt();
                CompoundTag received = buf.readNbt();
                String err = buf.readUtf();
                return new WonderAppResultPayload(
                        sub,
                        tickets,
                        resetEpoch,
                        hasEvent,
                        eventName,
                        customBanner,
                        daysLeft,
                        Collections.unmodifiableList(pool),
                        offered == null ? new CompoundTag() : offered,
                        received == null ? new CompoundTag() : received,
                        err
                );
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
