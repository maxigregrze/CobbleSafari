package maxigregrze.cobblesafari.onboarding;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * The eight boxes of the onboarding screen, as opened and as saved (action plan 198, section 6.1).
 * On the wire, one bit per box in component order.
 */
public record OnboardingChoices(
        boolean legendariesAndMythicals,
        boolean ultraBeasts,
        boolean paradox,
        boolean missingRegular,
        boolean portalSpawns,
        boolean portalNotification,
        boolean rewardBalloons,
        boolean megaShowdown
) {

    public static final StreamCodec<FriendlyByteBuf, OnboardingChoices> STREAM_CODEC = StreamCodec.of(
            (buf, choices) -> buf.writeByte(choices.toBits()),
            buf -> fromBits(buf.readUnsignedByte())
    );

    private int toBits() {
        boolean[] boxes = {legendariesAndMythicals, ultraBeasts, paradox, missingRegular,
                portalSpawns, portalNotification, rewardBalloons, megaShowdown};
        int bits = 0;
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i]) {
                bits |= 1 << i;
            }
        }
        return bits;
    }

    private static OnboardingChoices fromBits(int bits) {
        return new OnboardingChoices(bit(bits, 0), bit(bits, 1), bit(bits, 2), bit(bits, 3),
                bit(bits, 4), bit(bits, 5), bit(bits, 6), bit(bits, 7));
    }

    private static boolean bit(int bits, int index) {
        return (bits & (1 << index)) != 0;
    }
}
