package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class WildLevelModifierInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public WildLevelModifierInfluence(ServerPlayer player) {
        this.player = player;
    }

    private static double getEffectDistanceSq() {
        return Math.pow(SpawnBoostConfig.data.effectSettings.effectDistanceBlocks, 2);
    }

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {
        if (!(entity instanceof PokemonEntity pe)) {
            return;
        }
        // The proximity gate stays here, and deliberately does NOT move into applyLevelDelta
        // (action plan 184, Decision section 11.1): it is what keeps the open-world spawner from
        // resizing Pokemon that appear far from the eater. A generated encounter has no such
        // ambiguity - it exists because that one player asked for it - and the Ultra Space call
        // site would fail this check every time, since the encounter spawns a phase before the
        // player is teleported in.
        if (player.blockPosition().distSqr(pe.blockPosition()) > getEffectDistanceSq()) {
            return;
        }
        applyLevelDelta(player, pe);
    }

    /**
     * Applies the Humongo/Teensy level delta, with no proximity check (action plan 184, section
     * 11.1). Extracted so a hand-placed spawn can honour the donut without duplicating the lookup.
     *
     * @return the signed delta actually applied, 0 when no power is active or the Pokemon is not wild
     */
    public static int applyLevelDelta(ServerPlayer player, PokemonEntity pe) {
        if (!pe.getPokemon().isWild()) {
            return 0;
        }
        int humongoDelta = activeHumongoDelta(player);
        int teensyLoss = activeTeensyLoss(player);
        if (humongoDelta == 0 && teensyLoss == 0) {
            return 0;
        }

        int level = pe.getPokemon().getLevel();
        int next = level + humongoDelta - teensyLoss;
        int max = Cobblemon.INSTANCE.getConfig().getMaxPokemonLevel();
        next = Math.min(max, Math.max(1, next));
        pe.getPokemon().setLevel(next);
        return next - level;
    }

    private static int activeHumongoDelta(ServerPlayer player) {
        for (int lv = 3; lv >= 1; lv--) {
            if (player.hasEffect(ModPowerEffects.humongo(lv))) {
                return switch (lv) {
                    case 1 -> SpawnBoostConfig.data.effectSettings.humongoPowerLevel1LevelDelta;
                    case 2 -> SpawnBoostConfig.data.effectSettings.humongoPowerLevel2LevelDelta;
                    case 3 -> SpawnBoostConfig.data.effectSettings.humongoPowerLevel3LevelDelta;
                    default -> 0;
                };
            }
        }
        return 0;
    }

    private static int activeTeensyLoss(ServerPlayer player) {
        for (int lv = 3; lv >= 1; lv--) {
            if (player.hasEffect(ModPowerEffects.teensy(lv))) {
                return switch (lv) {
                    case 1 -> SpawnBoostConfig.data.effectSettings.teensyPowerLevel1LevelLoss;
                    case 2 -> SpawnBoostConfig.data.effectSettings.teensyPowerLevel2LevelLoss;
                    case 3 -> SpawnBoostConfig.data.effectSettings.teensyPowerLevel3LevelLoss;
                    default -> 0;
                };
            }
        }
        return 0;
    }
}
