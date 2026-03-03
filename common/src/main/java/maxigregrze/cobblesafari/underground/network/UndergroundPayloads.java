package maxigregrze.cobblesafari.underground.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Network payloads for the Underground Mining minigame.
 */
public class UndergroundPayloads {
    
    // UUID StreamCodec helper
    private static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );
    
    // ==================== CLIENT TO SERVER ====================
    
    /**
     * Request to mine at a position.
     */
    public record MineActionPayload(UUID sessionId, int cellX, int cellY) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MineActionPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_mine_action"));
        
        public static final StreamCodec<FriendlyByteBuf, MineActionPayload> STREAM_CODEC = StreamCodec.composite(
                UUID_CODEC, MineActionPayload::sessionId,
                ByteBufCodecs.INT, MineActionPayload::cellX,
                ByteBufCodecs.INT, MineActionPayload::cellY,
                MineActionPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Request to switch tools.
     */
    public record SwitchToolPayload(UUID sessionId, boolean toHammer) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SwitchToolPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_switch_tool"));
        
        public static final StreamCodec<FriendlyByteBuf, SwitchToolPayload> STREAM_CODEC = StreamCodec.composite(
                UUID_CODEC, SwitchToolPayload::sessionId,
                ByteBufCodecs.BOOL, SwitchToolPayload::toHammer,
                SwitchToolPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // ==================== SERVER TO CLIENT ====================
    
    /**
     * Update grid cells after mining.
     */
    public record GridUpdatePayload(UUID sessionId, List<CellUpdateData> updates) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GridUpdatePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_grid_update"));
        
        public static final StreamCodec<FriendlyByteBuf, GridUpdatePayload> STREAM_CODEC =
                StreamCodec.of(GridUpdatePayload::write, GridUpdatePayload::read);
        
        private static void write(FriendlyByteBuf buf, GridUpdatePayload payload) {
            buf.writeLong(payload.sessionId().getMostSignificantBits());
            buf.writeLong(payload.sessionId().getLeastSignificantBits());
            buf.writeInt(payload.updates().size());
            for (CellUpdateData update : payload.updates()) {
                buf.writeInt(update.x());
                buf.writeInt(update.y());
                buf.writeInt(update.newTier());
                buf.writeBoolean(update.revealed());
                buf.writeInt(update.secondLayerContent());
                buf.writeUtf(update.treasureId() != null ? update.treasureId() : "");
            }
        }
        
        private static GridUpdatePayload read(FriendlyByteBuf buf) {
            UUID sessionId = new UUID(buf.readLong(), buf.readLong());
            int count = buf.readInt();
            List<CellUpdateData> updates = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int x = buf.readInt();
                int y = buf.readInt();
                int newTier = buf.readInt();
                boolean revealed = buf.readBoolean();
                int secondLayerContent = buf.readInt();
                String treasureId = buf.readUtf();
                updates.add(new CellUpdateData(x, y, newTier, revealed, secondLayerContent, 
                        treasureId.isEmpty() ? null : treasureId));
            }
            return new GridUpdatePayload(sessionId, updates);
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    public record CellUpdateData(int x, int y, int newTier, boolean revealed, 
                                  int secondLayerContent, String treasureId) {}
    
    /**
     * Update stability after mining.
     */
    public record StabilityUpdatePayload(UUID sessionId, int current, int max) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StabilityUpdatePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_stability_update"));
        
        public static final StreamCodec<FriendlyByteBuf, StabilityUpdatePayload> STREAM_CODEC = StreamCodec.composite(
                UUID_CODEC, StabilityUpdatePayload::sessionId,
                ByteBufCodecs.INT, StabilityUpdatePayload::current,
                ByteBufCodecs.INT, StabilityUpdatePayload::max,
                StabilityUpdatePayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Notify that a treasure was fully revealed.
     */
    public record TreasureRevealedPayload(UUID sessionId, String treasureId, int startX, int startY) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TreasureRevealedPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_treasure_revealed"));
        
        public static final StreamCodec<FriendlyByteBuf, TreasureRevealedPayload> STREAM_CODEC = StreamCodec.composite(
                UUID_CODEC, TreasureRevealedPayload::sessionId,
                ByteBufCodecs.STRING_UTF8, TreasureRevealedPayload::treasureId,
                ByteBufCodecs.INT, TreasureRevealedPayload::startX,
                ByteBufCodecs.INT, TreasureRevealedPayload::startY,
                TreasureRevealedPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Notify that the game has ended.
     */
    public record GameEndPayload(UUID sessionId, boolean wallCollapsed, int treasuresCollected, int totalTreasures) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GameEndPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_game_end"));
        
        public static final StreamCodec<FriendlyByteBuf, GameEndPayload> STREAM_CODEC = StreamCodec.composite(
                UUID_CODEC, GameEndPayload::sessionId,
                ByteBufCodecs.BOOL, GameEndPayload::wallCollapsed,
                ByteBufCodecs.INT, GameEndPayload::treasuresCollected,
                ByteBufCodecs.INT, GameEndPayload::totalTreasures,
                GameEndPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Play a sound on the client.
     */
    public record PlaySoundPayload(UUID sessionId, String soundType) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlaySoundPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_play_sound"));
        
        public static final StreamCodec<FriendlyByteBuf, PlaySoundPayload> STREAM_CODEC = StreamCodec.composite(
                UUID_CODEC, PlaySoundPayload::sessionId,
                ByteBufCodecs.STRING_UTF8, PlaySoundPayload::soundType,
                PlaySoundPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Sync the full treasure registry to the client (shapes + definitions).
     */
    public record TreasureRegistrySyncPayload(List<TreasureEntryData> entries) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TreasureRegistrySyncPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_treasure_sync"));

        public static final StreamCodec<FriendlyByteBuf, TreasureRegistrySyncPayload> STREAM_CODEC =
                StreamCodec.of(TreasureRegistrySyncPayload::write, TreasureRegistrySyncPayload::read);

        private static void write(FriendlyByteBuf buf, TreasureRegistrySyncPayload payload) {
            buf.writeInt(payload.entries().size());
            for (TreasureEntryData entry : payload.entries()) {
                buf.writeUtf(entry.id());
                buf.writeUtf(entry.textureId());
                buf.writeInt(entry.weight());
                buf.writeInt(entry.minQty());
                buf.writeInt(entry.maxQty());
                buf.writeInt(entry.shapeMatrix().length);
                if (entry.shapeMatrix().length > 0) {
                    buf.writeInt(entry.shapeMatrix()[0].length);
                    for (boolean[] row : entry.shapeMatrix()) {
                        for (boolean cell : row) {
                            buf.writeBoolean(cell);
                        }
                    }
                }
            }
        }

        private static TreasureRegistrySyncPayload read(FriendlyByteBuf buf) {
            int count = buf.readInt();
            List<TreasureEntryData> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String id = buf.readUtf();
                String textureId = buf.readUtf();
                int weight = buf.readInt();
                int minQty = buf.readInt();
                int maxQty = buf.readInt();
                int rows = buf.readInt();
                boolean[][] matrix;
                if (rows > 0) {
                    int cols = buf.readInt();
                    matrix = new boolean[rows][cols];
                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            matrix[r][c] = buf.readBoolean();
                        }
                    }
                } else {
                    matrix = new boolean[0][0];
                }
                entries.add(new TreasureEntryData(id, textureId, weight, minQty, maxQty, matrix));
            }
            return new TreasureRegistrySyncPayload(entries);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TreasureEntryData(String id, String textureId, int weight,
                                     int minQty, int maxQty, boolean[][] shapeMatrix) {}
}
