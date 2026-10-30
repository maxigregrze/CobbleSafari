package maxigregrze.cobblesafari.network;

import io.netty.handler.codec.DecoderException;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SecretBasePCConfig;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: the server's Secret Base PC tiers (action plan 198, F5). The PC screen shows battery caps,
 * costs and locks; without this copy it would read them from the client's own config file.
 */
public record SecretBasePCConfigSyncPayload(List<SecretBasePCConfig.TierSnapshot> tiers) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SecretBasePCConfigSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "secretbase_pc_config_sync"));

    /** Far above the 6 shipped tiers; only bounds what a hostile server could make us allocate. */
    private static final int MAX_TIERS = 64;
    private static final int MAX_EFFECTS = 64;

    public static final StreamCodec<FriendlyByteBuf, SecretBasePCConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            SecretBasePCConfigSyncPayload::write,
            SecretBasePCConfigSyncPayload::read
    );

    private static void write(FriendlyByteBuf buf, SecretBasePCConfigSyncPayload payload) {
        buf.writeVarInt(payload.tiers().size());
        for (SecretBasePCConfig.TierSnapshot tier : payload.tiers()) {
            buf.writeVarInt(tier.maxBattery());
            buf.writeVarInt(tier.effectCosts().length);
            for (int cost : tier.effectCosts()) {
                buf.writeInt(cost);
            }
        }
    }

    private static SecretBasePCConfigSyncPayload read(FriendlyByteBuf buf) {
        int count = bounded(buf.readVarInt(), MAX_TIERS);
        List<SecretBasePCConfig.TierSnapshot> tiers = new ArrayList<>(count);
        for (int t = 0; t < count; t++) {
            int maxBattery = buf.readVarInt();
            int[] costs = new int[bounded(buf.readVarInt(), MAX_EFFECTS)];
            for (int e = 0; e < costs.length; e++) {
                costs[e] = buf.readInt();
            }
            tiers.add(new SecretBasePCConfig.TierSnapshot(maxBattery, costs));
        }
        return new SecretBasePCConfigSyncPayload(tiers);
    }

    private static int bounded(int size, int max) {
        if (size < 0 || size > max) {
            throw new DecoderException("Secret Base PC sync: size " + size + " out of [0, " + max + "]");
        }
        return size;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void syncToPlayer(ServerPlayer player) {
        Services.PLATFORM.sendPayloadToPlayer(player, new SecretBasePCConfigSyncPayload(SecretBasePCConfig.snapshotTiers()));
    }

    /** Client side. */
    public void apply() {
        SecretBasePCConfig.applyClientSync(tiers);
    }
}
