package maxigregrze.cobblesafari.dungeon.sub;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.influence.HiddenAbilityBoostInfluence;
import maxigregrze.cobblesafari.influence.WildLevelModifierInfluence;
import maxigregrze.cobblesafari.objectives.ObjectivesManager;
import maxigregrze.cobblesafari.power.GuaranteedShinyManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * The mod's own post-spawn modifiers, applied to a hand-placed Ultra Space encounter
 * (action plan 184, C9).
 *
 * <p>The encounter is built by {@link SubDungeonEncounter}, not by the Cobblemon spawner, so none of
 * the {@code SpawningInfluence}s registered in {@code CobbleSafari.initLogic()} ever see it: an
 * incense, a Sparkling Power donut and a guaranteed-shiny charge all counted for nothing in the one
 * place a player would most want them to. This class closes that, by calling the same code the
 * influences call.</p>
 *
 * <p>It does <b>not</b> fabricate a {@code SpawnAction} to reuse {@code affectSpawn}: a dummy object
 * satisfying a {@code @NotNull} signature is a lie that holds exactly until some influence reads its
 * argument. The two influences that had their logic buried in {@code affectSpawn} were refactored to
 * expose it as a static method instead; the other two entry points were already public and static.</p>
 *
 * <p>Only <b>post-spawn</b> modifiers are in scope. The pool-weighting influences
 * ({@code affectWeight}, {@code affectBucketWeights}, {@code affectSpawnable}) operate on a set of
 * {@code SpawnDetail}s, which does not exist here - the species comes from a flat destination table,
 * drawn uniformly.</p>
 */
public final class UltraEncounterModifiers {

    private UltraEncounterModifiers() {}

    /**
     * What the mod contributed, for the provenance line of section 12.2. Purely informational: the
     * log's modifier list is built by interrogating the finished Pokemon, never from this.
     *
     * @param hiddenAbilityAttempted a Hidden Power donut fired its roll and won
     * @param levelDelta             signed Humongo/Teensy shift actually applied
     * @param guaranteedShinyUsed    a stored shiny guarantee was spent on this encounter
     */
    public record Applied(boolean hiddenAbilityAttempted, int levelDelta, boolean guaranteedShinyUsed) {
    }

    /**
     * Runs the four post-spawn modifiers, in order, on an entity that is <b>not yet in the world</b>.
     *
     * <p>Called before {@code addFreshEntity} on purpose: applying a shiny guarantee afterwards would
     * pop the Pokemon into existence and then recolour it in front of the player. Neither guarantee
     * manager needs the entity to be added - both work on {@code entity.getPokemon()}.</p>
     */
    public static Applied apply(ServerPlayer player, PokemonEntity entity) {
        boolean hiddenAbility = HiddenAbilityBoostInfluence.tryBoost(player, entity);
        int levelDelta = WildLevelModifierInfluence.applyLevelDelta(player, entity);
        boolean guaranteedShiny = GuaranteedShinyManager.tryConsume(player, entity);
        // A no-op by construction today: plan 183 section 11.5 (S17) forbids a
        // dimensional_objectives file for dungeon_ultraspace, so no assignment can be live here.
        // Wired anyway - it costs a line, it is correct in advance, and its absence beside the
        // guarantee above would be an unexplainable asymmetry. It returns void, so unlike the
        // other three it contributes nothing to the provenance line.
        ObjectivesManager.tryConsumeSpawnGuarantee(player, entity);
        return new Applied(hiddenAbility, levelDelta, guaranteedShiny);
    }
}
