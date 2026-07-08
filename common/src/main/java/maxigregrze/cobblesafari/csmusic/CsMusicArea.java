package maxigregrze.cobblesafari.csmusic;

import java.util.List;

/**
 * A named music area within a dimension: a set of boxes plus the csmusic id to play inside them.
 * {@code priority} is set at creation (default {@code defaultAreaPriority} from config) and drives
 * arbitration against trigger rules.
 */
public record CsMusicArea(String id, String musicId, boolean activated, int priority, List<CsMusicBox> boxes) {

    public CsMusicArea withActivated(boolean v) {
        return new CsMusicArea(id, musicId, v, priority, boxes);
    }

    public CsMusicArea withBoxes(List<CsMusicBox> b) {
        return new CsMusicArea(id, musicId, activated, priority, List.copyOf(b));
    }

    public CsMusicArea withPriority(int p) {
        return new CsMusicArea(id, musicId, activated, p, boxes);
    }

    public boolean contains(int x, int y, int z) {
        for (CsMusicBox box : boxes) {
            if (box.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }
}
