package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.onboarding.OnboardingChoices;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: the onboarding screen's Save (action plan 198, section 6). The server re-checks permission. */
public record SaveOnboardingPayload(OnboardingChoices choices) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SaveOnboardingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "save_onboarding"));

    public static final StreamCodec<FriendlyByteBuf, SaveOnboardingPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> OnboardingChoices.STREAM_CODEC.encode(buf, payload.choices()),
            buf -> new SaveOnboardingPayload(OnboardingChoices.STREAM_CODEC.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
