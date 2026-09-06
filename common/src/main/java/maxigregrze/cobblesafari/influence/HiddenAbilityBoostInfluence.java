package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.compat.CobblemonFishingInterop;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class HiddenAbilityBoostInfluence implements SpawningInfluence {

    private static final float BASE_HIDDEN_ABILITY_ROLL_CHANCE = 0.05f;

    private final ServerPlayer player;

    public HiddenAbilityBoostInfluence(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {
        if (entity instanceof PokemonEntity pe) {
            tryBoost(player, pe);
        }
    }

    /**
     * The Hidden Power roll, callable outside the Cobblemon spawner (action plan 184, section 11.1).
     *
     * <p>Extracted so that a hand-placed spawn - the Ultra Space encounter - can honour the donut
     * without either duplicating this logic or fabricating a {@link SpawnAction} to satisfy a
     * signature. Behaviour through {@link #affectSpawn} is unchanged.</p>
     *
     * @return true when a hidden ability was actually attempted; the attempt itself is silently a
     *         no-op on a form that has none
     */
    public static boolean tryBoost(ServerPlayer player, PokemonEntity pe) {
        HiddenSel sel = findHidden(player, pe.getPokemon());
        if (sel == null) {
            return false;
        }
        float mult = switch (sel.level()) {
            case 1 -> SpawnBoostConfig.data.effectSettings.hiddenPowerLevel1BoostMultiplier;
            case 2 -> SpawnBoostConfig.data.effectSettings.hiddenPowerLevel2BoostMultiplier;
            case 3 -> SpawnBoostConfig.data.effectSettings.hiddenPowerLevel3BoostMultiplier;
            default -> 1.0f;
        };
        float chance = Math.min(1.0f, BASE_HIDDEN_ABILITY_ROLL_CHANCE * mult);
        if (player.getRandom().nextFloat() >= chance) {
            return false;
        }
        CobblemonFishingInterop.alterHiddenAbilityAttempt(pe);
        return true;
    }

    private static HiddenSel findHidden(ServerPlayer player, com.cobblemon.mod.common.pokemon.Pokemon pokemon) {
        for (int level = 3; level >= 1; level--) {
            for (int vi = 0; vi < PowerVariantRegistry.VARIANT_COUNT; vi++) {
                if (!player.hasEffect(ModPowerEffects.hidden(vi, level))) {
                    continue;
                }
                if (!PowerVariantRegistry.pokemonHasVariantType(pokemon, vi)) {
                    continue;
                }
                return new HiddenSel(level);
            }
        }
        return null;
    }

    private record HiddenSel(int level) {}
}
