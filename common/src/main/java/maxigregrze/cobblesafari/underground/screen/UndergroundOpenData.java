package maxigregrze.cobblesafari.underground.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Data sent when opening the Underground Mining screen.
 */
public record UndergroundOpenData(
    UUID sessionId,
    int treasureCount,
    byte[] gridData,
    int currentStability,
    int maxStability
) {
    
    public static final StreamCodec<FriendlyByteBuf, UndergroundOpenData> STREAM_CODEC =
            StreamCodec.of(UndergroundOpenData::write, UndergroundOpenData::read);
    
    private static void write(FriendlyByteBuf buf, UndergroundOpenData data) {
        buf.writeLong(data.sessionId().getMostSignificantBits());
        buf.writeLong(data.sessionId().getLeastSignificantBits());
        buf.writeInt(data.treasureCount());
        buf.writeByteArray(data.gridData());
        buf.writeInt(data.currentStability());
        buf.writeInt(data.maxStability());
    }
    
    private static UndergroundOpenData read(FriendlyByteBuf buf) {
        UUID sessionId = new UUID(buf.readLong(), buf.readLong());
        int treasureCount = buf.readInt();
        byte[] gridData = buf.readByteArray();
        int currentStability = buf.readInt();
        int maxStability = buf.readInt();
        return new UndergroundOpenData(sessionId, treasureCount, gridData, currentStability, maxStability);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UndergroundOpenData that)) return false;
        return treasureCount == that.treasureCount
                && currentStability == that.currentStability
                && maxStability == that.maxStability
                && Objects.equals(sessionId, that.sessionId)
                && Arrays.equals(gridData, that.gridData);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(sessionId, treasureCount, currentStability, maxStability) + Arrays.hashCode(gridData);
    }

    @Override
    public String toString() {
        return "UndergroundOpenData[sessionId=" + sessionId
                + ", treasureCount=" + treasureCount
                + ", gridData=" + Arrays.toString(gridData)
                + ", currentStability=" + currentStability
                + ", maxStability=" + maxStability + "]";
    }
}
