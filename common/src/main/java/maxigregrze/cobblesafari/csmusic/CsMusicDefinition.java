package maxigregrze.cobblesafari.csmusic;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Définition immuable d'une musique CobbleSafari, chargée depuis
 * {@code data/<ns>/csmusic/<id>.json} (plan 105 § 2). Les champs intro/loop/outro sont des
 * <b>ids d'événement sonore</b> résolus par le {@code sounds.json} du resourcepack client.
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
