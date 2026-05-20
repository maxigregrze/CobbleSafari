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

public record GtsAppResultPayload(
        int subscreen,
        int offerCount,
        int ownActiveOfferId,
        int successCount,
        int oldestSuccessId,
        String validateResult,
        String operationResult,
        int searchPage,
        int searchTotalPages,
        List<SearchEntry> searchEntries,
        String startTradeKind,
        List<CompoundTag> candidateNbts,
        CompoundTag offeredNbt,
        CompoundTag receivedNbt,
        String errorKey
) implements CustomPacketPayload {

    public static final int SUB_BEGIN = 0;
    public static final int SUB_VALIDATE_RESULT = 1;
    public static final int SUB_DEPOSIT = 2;
    public static final int SUB_RETRIEVAL = 3;
    public static final int SUB_RECEIVE = 4;
    public static final int SUB_SEARCH_RESULT = 5;
    public static final int SUB_START_TRADE_RESULT = 6;
    public static final int SUB_TRADE = 7;
    public static final int SUB_ERROR = 8;

    public record SearchEntry(
            int offerId,
            CompoundTag offeredNbt,
            String wishSpecies,
            int wishLevelBucket,
            String wishGender,
            String wishShiny
    ) {}

    public static final CustomPacketPayload.Type<GtsAppResultPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "gts_app_result"));

    public static final StreamCodec<FriendlyByteBuf, GtsAppResultPayload> STREAM_CODEC = StreamCodec.of(
            GtsAppResultPayload::write,
            GtsAppResultPayload::read);

    private static void write(FriendlyByteBuf buf, GtsAppResultPayload p) {
        buf.writeVarInt(p.subscreen());
        buf.writeVarInt(p.offerCount());
        buf.writeVarInt(p.ownActiveOfferId());
        buf.writeVarInt(p.successCount());
        buf.writeVarInt(p.oldestSuccessId());
        buf.writeUtf(p.validateResult() == null ? "" : p.validateResult());
        buf.writeUtf(p.operationResult() == null ? "" : p.operationResult());
        buf.writeVarInt(p.searchPage());
        buf.writeVarInt(p.searchTotalPages());
        List<SearchEntry> entries = p.searchEntries() == null ? List.of() : p.searchEntries();
        buf.writeVarInt(entries.size());
        for (SearchEntry e : entries) {
            buf.writeVarInt(e.offerId());
            buf.writeNbt(e.offeredNbt() == null ? new CompoundTag() : e.offeredNbt());
            buf.writeUtf(e.wishSpecies() == null ? "" : e.wishSpecies());
            buf.writeVarInt(e.wishLevelBucket());
            buf.writeUtf(e.wishGender() == null ? "" : e.wishGender());
            buf.writeUtf(e.wishShiny() == null ? "" : e.wishShiny());
        }
        buf.writeUtf(p.startTradeKind() == null ? "" : p.startTradeKind());
        List<CompoundTag> cands = p.candidateNbts() == null ? List.of() : p.candidateNbts();
        buf.writeVarInt(cands.size());
        for (CompoundTag t : cands) {
            buf.writeNbt(t == null ? new CompoundTag() : t);
        }
        buf.writeNbt(p.offeredNbt() == null ? new CompoundTag() : p.offeredNbt());
        buf.writeNbt(p.receivedNbt() == null ? new CompoundTag() : p.receivedNbt());
        buf.writeUtf(p.errorKey() == null ? "" : p.errorKey());
    }

    private static GtsAppResultPayload read(FriendlyByteBuf buf) {
        int sub = buf.readVarInt();
        int offerCount = buf.readVarInt();
        int ownId = buf.readVarInt();
        int successCount = buf.readVarInt();
        int oldestSuccess = buf.readVarInt();
        String validate = buf.readUtf();
        String op = buf.readUtf();
        int searchPage = buf.readVarInt();
        int searchTotal = buf.readVarInt();
        int entryCount = buf.readVarInt();
        List<SearchEntry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            int oid = buf.readVarInt();
            CompoundTag nbt = buf.readNbt();
            String wish = buf.readUtf();
            int bucket = buf.readVarInt();
            String gender = buf.readUtf();
            String shiny = buf.readUtf();
            entries.add(new SearchEntry(
                    oid,
                    nbt == null ? new CompoundTag() : nbt,
                    wish,
                    bucket,
                    gender,
                    shiny));
        }
        String startKind = buf.readUtf();
        int candCount = buf.readVarInt();
        List<CompoundTag> cands = new ArrayList<>(candCount);
        for (int i = 0; i < candCount; i++) {
            CompoundTag t = buf.readNbt();
            cands.add(t == null ? new CompoundTag() : t);
        }
        CompoundTag offered = buf.readNbt();
        CompoundTag received = buf.readNbt();
        String err = buf.readUtf();
        return new GtsAppResultPayload(
                sub,
                offerCount,
                ownId,
                successCount,
                oldestSuccess,
                validate,
                op,
                searchPage,
                searchTotal,
                Collections.unmodifiableList(entries),
                startKind,
                Collections.unmodifiableList(cands),
                offered == null ? new CompoundTag() : offered,
                received == null ? new CompoundTag() : received,
                err);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
