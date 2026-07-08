package maxigregrze.cobblesafari.csmusic;

import java.util.ArrayList;
import java.util.List;

public class CsMusicAreaFileData {
    public List<AreaEntry> areas = new ArrayList<>();

    public static class AreaEntry {
        public String id;
        public String music;
        public boolean activated;
        /** 0 or missing = use config {@code defaultAreaPriority} (backward compatible). */
        public int priority;
        public List<BoxEntry> boxes = new ArrayList<>();
    }

    public static class BoxEntry {
        public int[] min;
        public int[] max;
    }
}
