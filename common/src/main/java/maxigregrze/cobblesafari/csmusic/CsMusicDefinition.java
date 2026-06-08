package maxigregrze.cobblesafari.csmusic;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable CobbleSafari music definition, loaded from
 * {@code data/<ns>/csmusic/<id>.json} (plan 105 § 2). intro/loop/outro fields are
 * <b>sound event ids</b> resolved by the client resource pack {@code sounds.json}.
 */
public record CsMusicDefinition(
        String id,
        ResourceLocation loop,
        @Nullable ResourceLocation intro,
        @Nullable ResourceLocation outro,
        int priority
) {
    public static final int DEFAULT_PRIORITY = 1;

    public boolean hasIntro() {
        return intro != null;
    }

    public boolean hasOutro() {
        return outro != null;
    }
}
