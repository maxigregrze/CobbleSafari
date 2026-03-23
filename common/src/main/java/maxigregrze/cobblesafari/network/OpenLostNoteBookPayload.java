package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record OpenLostNoteBookPayload(ItemStack book) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenLostNoteBookPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_lost_note_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLostNoteBookPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            OpenLostNoteBookPayload::book,
            OpenLostNoteBookPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
