package maxigregrze.cobblesafari.csmusic;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory set of compiled {@link CsMusicRule}s loaded from {@code csmusic/definition/*.json}.
 * Order is not significant — the resolver sorts by priority.
 */
public final class CsMusicTriggerRegistry {

    private static final List<CsMusicRule> RULES = new ArrayList<>();

    private CsMusicTriggerRegistry() {}

    /**
     * True if the player currently matches at least one rule that can only play during a battle
     * ({@link CsMusicCondition#requiresBattle()}) — i.e. "a csmusic track matches the fight".
     * Drives suppression of Cobblemon's native battle music.
     */
    public static boolean hasMatchingBattleRule(ServerPlayer player) {
        for (CsMusicRule rule : RULES) {
            if (rule.condition().requiresBattle() && rule.condition().matches(player)) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        RULES.clear();
    }

    public static void addAll(List<CsMusicRule> rules) {
        RULES.addAll(rules);
    }

    public static List<CsMusicRule> all() {
        return Collections.unmodifiableList(RULES);
    }

    public static int size() {
        return RULES.size();
    }
}
