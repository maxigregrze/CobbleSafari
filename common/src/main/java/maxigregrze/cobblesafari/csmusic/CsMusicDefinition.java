package maxigregrze.cobblesafari.csmusic;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Immutable CobbleSafari music definition, loaded from
 * {@code data/<ns>/csmusic/<id>.json}. intro/loop/outro fields are
 * <b>sound event ids</b> resolved by the client resource pack {@code sounds.json}.
 *
 * <p>{@code parent} is a static musical relationship: when this track wins over its parent, the
 * client performs a <b>synced crossfade</b> (start at the parent's playhead). {@code tags} let
 * trigger rules pick a random track from a pool. {@code priority} is retained for backward
 * compatibility but is no longer used for arbitration (priority now lives on trigger rules / areas).</p>
 */
public record CsMusicDefinition(
        String id,
        ResourceLocation loop,
        @Nullable ResourceLocation intro,
        @Nullable ResourceLocation outro,
        int priority,
        @Nullable String parent,
        Set<String> tags
) {
    public static final int DEFAULT_PRIORITY = 1;

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
