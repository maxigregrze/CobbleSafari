package maxigregrze.cobblesafari.csmusic;

import java.util.List;

public record CsMusicArea(String id, String musicId, boolean activated, List<CsMusicBox> boxes) {

    public CsMusicArea withActivated(boolean v) {
        return new CsMusicArea(id, musicId, v, boxes);
    }

    public CsMusicArea withBoxes(List<CsMusicBox> b) {
        return new CsMusicArea(id, musicId, activated, List.copyOf(b));
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
