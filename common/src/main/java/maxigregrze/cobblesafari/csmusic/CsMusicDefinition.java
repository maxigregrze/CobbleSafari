package maxigregrze.cobblesafari.csmusic;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Immutable CobbleSafari music definition, loaded from an initialisation file
 * {@code data/<ns>/csmusic/<id>.json}. intro/loop/outro fields are <b>sound event ids</b> resolved
 * by the client resource pack {@code sounds.json}.
 *
 * <p>{@code parent} is a static musical relationship: two related tracks share a loop length, so a
 * transition between them is always a <b>synced crossfade</b> (the incoming track starts at the
 * outgoing track's playhead), whatever the priorities involved. {@code tags} let trigger rules draw
 * a random track from a pool.</p>
 *
 * <p>There is no {@code priority} here: priority lives on the trigger rule and on the area.</p>
 */
public record CsMusicDefinition(
        String id,
        ResourceLocation loop,
        @Nullable ResourceLocation intro,
        @Nullable ResourceLocation outro,
        @Nullable String parent,
        Set<String> tags
) {
    public boolean hasIntro() {
        return intro != null;
    }

    public boolean hasOutro() {
        return outro != null;
    }

    public boolean hasParent() {
        return parent != null && !parent.isBlank();
    }

    public boolean hasTag(String tag) {
        return tag != null && tags.contains(tag);
    }
}
