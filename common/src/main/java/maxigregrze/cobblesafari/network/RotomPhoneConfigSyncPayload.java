package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RotomPhoneConfigSyncPayload(
        List<AppData> apps,
        List<SkinData> skins
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RotomPhoneConfigSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "rotom_phone_config_sync"));

    public static final StreamCodec<FriendlyByteBuf, RotomPhoneConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            RotomPhoneConfigSyncPayload::write,
            RotomPhoneConfigSyncPayload::read
    );

    private static void write(FriendlyByteBuf buf, RotomPhoneConfigSyncPayload payload) {
        buf.writeInt(payload.apps.size());
        for (AppData app : payload.apps) {
            buf.writeUtf(app.name);
            buf.writeInt(app.bannedDimensions.size());
            for (String dim : app.bannedDimensions) {
                buf.writeUtf(dim);
            }
            buf.writeBoolean(app.unlockedForPlayer);
        }
        buf.writeInt(payload.skins.size());
        for (SkinData skin : payload.skins) {
            buf.writeUtf(skin.id);
            buf.writeUtf(skin.displayName);
            buf.writeUtf(skin.color);
            buf.writeBoolean(skin.hasCustomScreen);
            buf.writeBoolean(skin.unlockedFromStart);
            buf.writeBoolean(skin.hasShinyVariant);
            buf.writeBoolean(skin.unlockedForPlayer);
        }
    }

    private static RotomPhoneConfigSyncPayload read(FriendlyByteBuf buf) {
        int appCount = buf.readInt();
        List<AppData> apps = new ArrayList<>();
        for (int i = 0; i < appCount; i++) {
            String name = buf.readUtf();
            int dimCount = buf.readInt();
            List<String> bannedDimensions = new ArrayList<>();
            for (int j = 0; j < dimCount; j++) {
                bannedDimensions.add(buf.readUtf());
            }
            boolean unlockedForPlayer = buf.readBoolean();
            apps.add(new AppData(name, bannedDimensions, unlockedForPlayer));
        }
        int skinCount = buf.readInt();
        List<SkinData> skins = new ArrayList<>();
        for (int i = 0; i < skinCount; i++) {
            String id = buf.readUtf();
            String display = buf.readUtf();
            String color = buf.readUtf();
            boolean hasCustomScreen = buf.readBoolean();
            boolean unlockedFromStart = buf.readBoolean();
            boolean hasShiny = buf.readBoolean();
            boolean unlockedForPlayer = buf.readBoolean();
            skins.add(new SkinData(id, display, color, hasCustomScreen, unlockedFromStart, hasShiny, unlockedForPlayer));
        }
        return new RotomPhoneConfigSyncPayload(apps, skins);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record AppData(String name, List<String> bannedDimensions, boolean unlockedForPlayer) {}

    public record SkinData(String id, String displayName, String color,
                           boolean hasCustomScreen, boolean unlockedFromStart,
                           boolean hasShinyVariant, boolean unlockedForPlayer) {}
}
