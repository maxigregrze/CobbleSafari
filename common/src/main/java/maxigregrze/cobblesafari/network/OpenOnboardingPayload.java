package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.onboarding.OnboardingChoices;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: opens the onboarding screen with the boxes the server pre-filled (action plan 198, section 6). */
public record OpenOnboardingPayload(OnboardingChoices initial) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenOnboardingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_onboarding"));

    public static final StreamCodec<FriendlyByteBuf, OpenOnboardingPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> OnboardingChoices.STREAM_CODEC.encode(buf, payload.initial()),
            buf -> new OpenOnboardingPayload(OnboardingChoices.STREAM_CODEC.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
