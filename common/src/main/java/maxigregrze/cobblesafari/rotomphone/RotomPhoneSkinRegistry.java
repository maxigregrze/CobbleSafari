package maxigregrze.cobblesafari.rotomphone;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RotomPhoneSkinRegistry {
    private static final Map<String, RotomPhoneSkinDefinition> SKINS = new LinkedHashMap<>();

    private RotomPhoneSkinRegistry() {}

    public static void clear() {
        SKINS.clear();
    }

    public static void register(RotomPhoneSkinDefinition skin) {
        SKINS.put(skin.getId(), skin);
    }

    public static RotomPhoneSkinDefinition getSkin(String id) {
        return SKINS.get(id);
    }

    public static List<RotomPhoneSkinDefinition> getAllSkins() {
        return Collections.unmodifiableList(new ArrayList<>(SKINS.values()));
    }

    public static boolean isUnlockedByPlayer(ServerPlayer player, RotomPhoneSkinDefinition skin) {
        if (skin == null) {
            return false;
        }
        if (skin.isUnlockedFromStart()) {
            return true;
        }
        String advancementPath = skin.getUnlockingAdvancement();
        if (advancementPath == null || advancementPath.isEmpty()) {
            return false;
        }
        ResourceLocation advId = ResourceLocation.tryParse(advancementPath);
        if (advId == null) {
            return false;
        }
        AdvancementHolder holder = player.server.getAdvancements().get(advId);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    public static List<RotomPhoneSkinDefinition> getUnlockedSkins(ServerPlayer player) {
        List<RotomPhoneSkinDefinition> unlocked = new ArrayList<>();
        for (RotomPhoneSkinDefinition skin : SKINS.values()) {
            if (isUnlockedByPlayer(player, skin)) {
                unlocked.add(skin);
            }
        }
        return unlocked;
    }
}
