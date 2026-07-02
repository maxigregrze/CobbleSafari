package maxigregrze.cobblesafari.rotomphone;

import java.util.List;

public class RotomPhoneSkinDefinition {
    private final String id;
    private final String displayName;
    private final String color;
    private final boolean hasCustomScreen;
    private final boolean unlockedFromStart;
    private final String unlockingAdvancement;
    private final boolean hasShinyVariant;
    private final List<String> tags;
    private final boolean addUnlockItem;

    public RotomPhoneSkinDefinition(String id, String displayName, String color,
                                     boolean hasCustomScreen, boolean unlockedFromStart,
                                     String unlockingAdvancement, boolean hasShinyVariant,
                                     List<String> tags, boolean addUnlockItem) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.hasCustomScreen = hasCustomScreen;
        this.unlockedFromStart = unlockedFromStart;
        this.unlockingAdvancement = unlockingAdvancement;
        this.hasShinyVariant = hasShinyVariant;
        this.tags = tags != null ? List.copyOf(tags) : List.of();
        this.addUnlockItem = addUnlockItem;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public boolean hasCustomScreen() { return hasCustomScreen; }
    public boolean isUnlockedFromStart() { return unlockedFromStart; }
    public String getUnlockingAdvancement() { return unlockingAdvancement; }
    public boolean hasShinyVariant() { return hasShinyVariant; }
    public List<String> getTags() { return tags; }
    public boolean addUnlockItem() { return addUnlockItem; }
}
