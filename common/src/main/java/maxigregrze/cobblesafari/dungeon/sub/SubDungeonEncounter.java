package maxigregrze.cobblesafari.dungeon.sub;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.moves.Learnset;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.HiddenAbilityProperty;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.warpride.logic.WarpRideDestination;
import maxigregrze.cobblesafari.warpride.logic.WarpRideDestinationRegistry;
import maxigregrze.cobblesafari.warpride.logic.WarpRideDifficultyLoader;
import maxigregrze.cobblesafari.warpride.logic.WarpRideRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The wild encounter of a sub-instance: what the ride paid for (action plans 183 B8, and 184).
 *
 * <p>Plan 183 shipped this as a bare species draw, with {@code rank} choosing a list and
 * {@code distance} consumed nowhere. Plan 184 makes the ride's outcome decide the Pokemon's level,
 * IVs, shininess, hidden ability and bonus moves, and re-opens the mod's own spawn modifiers - which
 * a hand-placed spawn bypasses entirely - onto it.</p>
 *
 * <p>The order of section 10 is load-bearing and three of its steps cannot move:</p>
 * <ul>
 *   <li>the {@code ServerPlayer} handed to {@code create} is what makes {@code SHINY_CHANCE_CALCULATION}
 *       fire with a player, and therefore what makes every incense and Sparkling Power donut count;</li>
 *   <li>the bonus-move draw cannot happen before {@code create}, because the {@code Learnset} lives on
 *       the <b>form</b>, which Cobblemon only resolves inside {@code apply};</li>
 *   <li>the mod's modifiers run before {@code addFreshEntity}, so a shiny guarantee does not recolour
 *       a Pokemon in front of the player who just found it.</li>
 * </ul>
 */
final class SubDungeonEncounter {

    private SubDungeonEncounter() {}

    private static final int PARTY_SIZE = 6;
    private static final int LEVEL_CEILING = 100;

    /**
     * Draws one anchor uniformly at random, erases <b>every</b> anchor, and spawns on the drawn one.
     *
     * <p>The anchors are cleared <b>before</b> the species list is consulted, not after the spawn:
     * an empty list must not leave marker blocks standing in the structure.</p>
     */
    static void resolve(ServerLevel level, ServerPlayer player, List<BlockPos> anchors,
                        SubDungeonRequest request) {
        // Only anchors the player can actually reach are eligible. A structure that straddles the
        // void guard carries some of its anchors in the buried half, below the line - a Pokemon
        // placed there would be walled into the terrain, and a player going after it would be
        // evacuated on the way down. The draw is therefore over the reachable anchors only, while
        // the erasure below still covers every one of them.
        DungeonConfig config = DungeonDimensions.getDungeonByDimension(level.dimension());
        int guardY = config == null ? Integer.MIN_VALUE : config.getVoidGuardY();
        List<BlockPos> reachable = anchors.stream().filter(a -> a.getY() >= guardY).toList();

        BlockPos spot = reachable.isEmpty()
                ? null
                : reachable.get(level.getRandom().nextInt(reachable.size()));

        for (BlockPos anchor : anchors) {
            level.setBlock(anchor, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }

        if (spot == null) {
            if (anchors.isEmpty()) {
                CobbleSafari.LOGGER.warn("[SubDungeon] no ultra_spawn_anchor found in {}; no encounter spawned",
                        request.structurePool());
            } else {
                CobbleSafari.LOGGER.warn("[SubDungeon] all {} ultra_spawn_anchor(s) in {} sit below the void "
                        + "guard at Y {}; no encounter spawned. The pool needs at least one anchor in the "
                        + "half of the structure players can occupy",
                        anchors.size(), request.structurePool(), guardY);
            }
            return;
        }

        WarpRideDestination destination = WarpRideDestinationRegistry.byTypeId(request.typeId());
        List<String> species = destination == null ? List.of() : destination.encountersForRank(request.tier());
        if (species.isEmpty()) {
            CobbleSafari.LOGGER.debug("[SubDungeon] destination {} rank {} has no species list; nothing spawned",
                    request.typeId(), request.tier());
            return;
        }

        String picked = species.get(level.getRandom().nextInt(species.size()));
        spawnReward(level, player, spot, picked, destination, request);
    }

    /** Pins one movement attribute to zero, if the entity carries it at all. */
    private static void zeroSpeed(PokemonEntity entity, Holder<Attribute> attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(0.0);
        }
    }

    /**
     * The whole composition of section 10, in the one order that works.
     *
     * <p>{@code speciesSpec} is the flat {@code "species"} / {@code "species form=x"} convention the
     * mod already uses in {@code egg_incubator/wildegg_*.json}. Every rank-driven setting is guarded
     * by "only if the destination author did not already pin it": an entry that says
     * {@code "nihilego level=70 shiny"} keeps both, and still receives the IVs, hidden ability and
     * moves it did not fix. The explicit beats the scale, and the scale beats the default.</p>
     */
    private static void spawnReward(ServerLevel level, ServerPlayer player, BlockPos pos,
                                    String speciesSpec, WarpRideDestination destination,
                                    SubDungeonRequest request) {
        try {
            RandomSource random = level.getRandom();
            WarpRideRewards rewards = WarpRideDifficultyLoader.get().rewards();
            int rank = Mth.clamp(request.tier(), 0, 3);
            double p = rewards.performanceFactor(request.distance());

            PokemonProperties props = PokemonProperties.Companion.parse(speciesSpec);

            double median = partyMedianOfFilledSlots(player);
            int targetLevel = clampLevel((int) Math.round(median) + rewards.levelOffset(rank));
            if (props.getLevel() == null) {
                props.setLevel(targetLevel);
            }
            if (props.getMinPerfectIVs() == null) {
                props.setMinPerfectIVs(rewards.ivPerfectCount(rank));
            }

            double haPercent = rewards.hiddenAbilityPercent(rank, p);
            boolean haRolled = !hasHiddenAbilityProperty(props) && rollPercent(random, haPercent);
            if (haRolled) {
                props.getCustomProperties().add(new HiddenAbilityProperty(true));
            }

            double alphaPercent = rewards.alphaPercent(rank, p);
            boolean alphaRolled = props.isAlpha() == null && rollPercent(random, alphaPercent);
            if (alphaRolled) {
                props.setAlpha(true);
            }

            double shinyPercent = rewards.shinyPercent(rank, p);
            boolean shinyByRank = props.getShiny() == null && rollPercent(random, shinyPercent);
            if (shinyByRank) {
                props.setShiny(true);
            }

            // The player argument is the point: create -> roll -> SHINY_CHANCE_CALCULATION, which
            // ShinyBoostEvent listens to and which returns the rate untouched when the player is
            // null. Cobblemon only rolls shiny when properties.shiny is still null, so leaving it
            // unset above is exactly what lets the boosted roll have its independent turn.
            Pokemon pokemon = props.create(player);

            List<MoveTemplate> drawn = drawBonusMoves(pokemon, rewards, rank, p, random);
            if (!drawn.isEmpty()) {
                PokemonProperties moveProps = new PokemonProperties();
                moveProps.setMoves(drawn.stream().map(MoveTemplate::getName).toList());
                moveProps.apply(pokemon);
            }
            // What Cobblemon actually inserted, not what we asked for: its insertion path takes
            // min(replaceable slots, insertable moves), so a move the Pokemon already knew is
            // silently dropped. The log must report the Pokemon, never the intent.
            List<MoveTemplate> bonusMoves = drawn.stream().filter(move ->
                    pokemon.getMoveSet().getMoves().stream()
                            .anyMatch(known -> known.getTemplate().equals(move))).toList();

            PokemonEntity entity = new PokemonEntity(level, pokemon, CobblemonEntities.POKEMON);
            props.applyCustomProperties(entity);
            UltraEncounterModifiers.Applied applied = UltraEncounterModifiers.apply(player, entity);

            entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    random.nextFloat() * 360.0f, 0.0f);

            // The reward stays on its anchor. A structure this shape is full of ledges and open
            // shafts, and a Pokemon left to roam does not merely wander off: a flying or levitating
            // species climbs out of the room entirely and parks somewhere the player cannot follow -
            // in the buried half below the void guard, or clear of the structure altogether. The
            // encounter was paid for with a ride, so it may not be allowed to walk away from the
            // person who won it.
            //
            // Zeroing the speed attributes rather than setNoAi: the brain keeps running, so the
            // Pokemon still turns, tracks the player and plays its idle animations - it simply has
            // no speed to translate with. setNoAi would freeze it into an ornament.
            //
            // Both attributes are zeroed because walking and flight read different ones, and a
            // levitating species is precisely the case that gets out of reach.
            zeroSpeed(entity, Attributes.MOVEMENT_SPEED);
            zeroSpeed(entity, Attributes.FLYING_SPEED);
            entity.setPersistenceRequired();

            level.addFreshEntity(entity);

            logOutcome(player, request, destination, pokemon, bonusMoves);
            logProvenance(p, shinyPercent, shinyByRank, haPercent, haRolled,
                    alphaPercent, alphaRolled, median, targetLevel, bonusMoves, rewards, rank,
                    applied);
        } catch (Exception e) {
            // A malformed species line is a data error: log it and leave the room empty rather
            // than failing a generation the player has already paid a ride for.
            CobbleSafari.LOGGER.error("[SubDungeon] could not spawn '{}' at {}", speciesSpec, pos, e);
        }
    }

    // ------------------------------------------------------------------ level

    /**
     * The median level over the player's <b>filled</b> party slots (action plan 184, Decision § 4).
     *
     * <p>Not {@code DifficultyScaling.partyMedian}: that one reads six fixed slots and counts an
     * empty one as level 0, so a party of three level-100 Pokemon yields 50. For CSBoss that is
     * intended - a small party earns a longer fight - but here it would flatly punish travelling
     * light, on a reward that is neither repeatable nor cheap. CSBoss is left untouched rather than
     * given a flag, so no boss-fight balance can regress from this change.</p>
     *
     * @return 0 when the party is empty, which the caller floors to level 1
     */
    private static double partyMedianOfFilledSlots(ServerPlayer player) {
        List<Double> levels = new ArrayList<>(PARTY_SIZE);
        try {
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (int slot = 0; slot < PARTY_SIZE; slot++) {
                Pokemon p = party.get(slot);
                if (p != null) {
                    levels.add((double) Mth.clamp(p.getLevel(), 1, LEVEL_CEILING));
                }
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("[SubDungeon] could not read {}'s party; treating it as empty",
                    player.getName().getString(), e);
            return 0.0;
        }
        if (levels.isEmpty()) {
            return 0.0;
        }
        double[] sorted = levels.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(sorted);
        int n = sorted.length;
        return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
    }

    /** Slots 3 and 4 of Cobblemon's own {@code cobblemon:alpha} moveset builder, which it fills with TMs. */
    private static final int ALPHA_TM_MOVES = 2;

    private static int clampLevel(int level) {
        int max = Math.min(LEVEL_CEILING, Cobblemon.INSTANCE.getConfig().getMaxPokemonLevel());
        return Mth.clamp(level, 1, max);
    }

    // ------------------------------------------------------------------ moves

    /**
     * Draws the new moves: 0, 1 or 2 rank bonuses from the resolved form's egg and TM pools merged,
     * plus the two TM moves an alpha is owed.
     *
     * <p>Merged on purpose: most Ultra Beasts have no egg moves at all, and an egg-only pool would
     * have made rank 3's promise fail silently on exactly the species this dungeon exists to hand
     * out. The known price is that a species with forty TMs and four egg moves will usually draw a
     * TM - which is why the log names which kind it actually was.</p>
     *
     * <p>The alpha half needs no draw at all. {@code PokemonProperties.create} runs
     * {@code apply} - which sets {@code isAlpha} - and then {@code initialize()}, which finds an
     * empty moveset and calls {@code initializeMovesetFromDefault()}; for an alpha that resolves
     * the {@code cobblemon:alpha} builder, whose slots 3 and 4 are {@code tm}. The two TM moves are
     * therefore already on the Pokemon by the time we get here. What they need is <b>protection</b>:
     * the bonus insertion picks its target slots at random, so it could overwrite them and the
     * guarantee would vanish silently. Listing them among the requested moves is what prevents it -
     * see the note on {@link #drawInto} callers below. A fully rolled alpha thus ends up with two TM
     * moves and two bonus moves, a whole moveset of new moves, which is the system's own ceiling
     * rather than one imposed here.</p>
     */
    private static List<MoveTemplate> drawBonusMoves(Pokemon pokemon, WarpRideRewards rewards,
                                                     int rank, double p, RandomSource random) {
        int wanted = 0;
        if (rollPercent(random, rewards.bonusMovePercent(rank, p))) {
            wanted = 1;
            if (rollPercent(random, rewards.secondMovePercent(rank, p))) {
                wanted = 2;
            }
        }

        Learnset learnset = pokemon.getForm().getMoves();
        List<MoveTemplate> drawn = new ArrayList<>(wanted + ALPHA_TM_MOVES);

        if (wanted > 0) {
            Set<MoveTemplate> pool = new LinkedHashSet<>(learnset.getEggMoves());
            pool.addAll(learnset.getTmMoves());
            drawInto(drawn, pool, wanted, random);
        }

        // Cobblemon already granted the alpha its TM moves (see the javadoc). Naming them in the
        // requested set does not insert them a second time - PokemonProperties.apply filters out
        // moves the Pokemon already knows - but it does mark their slots unreplaceable, which is
        // the only way to stop the bonus draw from landing on top of them.
        if (pokemon.isAlpha()) {
            List<MoveTemplate> tmMoves = learnset.getTmMoves();
            pokemon.getMoveSet().getMoves().stream()
                    .map(Move::getTemplate)
                    .filter(tmMoves::contains)
                    .limit(ALPHA_TM_MOVES)
                    .filter(move -> !drawn.contains(move))
                    .forEach(drawn::add);
        }
        return drawn;
    }

    /** Draws up to {@code wanted} entries from {@code pool}, skipping what {@code into} already holds. */
    private static void drawInto(List<MoveTemplate> into, Set<MoveTemplate> pool, int wanted,
                                 RandomSource random) {
        List<MoveTemplate> candidates = new ArrayList<>(pool);
        candidates.removeAll(into);
        for (int i = 0; i < wanted && !candidates.isEmpty(); i++) {
            into.add(candidates.remove(random.nextInt(candidates.size())));
        }
    }

    private static boolean isEggMove(Pokemon pokemon, MoveTemplate move) {
        return pokemon.getForm().getMoves().getEggMoves().contains(move);
    }

    // ------------------------------------------------------------------ logging

    /**
     * The one operator-facing line. Every fragment is checked against the <b>finished</b> Pokemon,
     * never inferred from the roll that was meant to produce it: a won hidden-ability roll on a
     * species that has none must not claim one, and a shiny that came from an incense rather than
     * from the rank must still read as shiny.
     */
    private static void logOutcome(ServerPlayer player, SubDungeonRequest request,
                                   WarpRideDestination destination, Pokemon pokemon,
                                   List<MoveTemplate> bonusMoves) {
        List<String> modifiers = new ArrayList<>();
        if (pokemon.getShiny()) {
            modifiers.add("is Shiny");
        }
        if (pokemon.isAlpha()) {
            modifiers.add("is Alpha");
        }
        if (new HiddenAbilityProperty(true).matches(pokemon)) {
            modifiers.add("has a hidden ability");
        }
        String moveFragment = describeBonusMoves(pokemon, bonusMoves);
        if (moveFragment != null) {
            modifiers.add(moveFragment);
        }
        modifiers.add("has " + perfectIvCount(pokemon) + " perfect IVs");

        CobbleSafari.LOGGER.info(
                "[WarpRide] {} has completed a warp dive ( distance = {} ; type = {} ; Rank = {} ) "
                        + "and has rolled a Lvl {} {} that {}",
                player.getName().getString(),
                request.distance(),
                destination == null ? "unknown" : destination.destinationId(),
                request.tier(),
                pokemon.getLevel(),
                pokemon.getSpecies().getName(),
                joinModifiers(modifiers));
    }

    /** Joins with {@code ", "}, and {@code " & "} before the last one. */
    private static String joinModifiers(List<String> modifiers) {
        if (modifiers.size() == 1) {
            return modifiers.get(0);
        }
        String head = String.join(", ", modifiers.subList(0, modifiers.size() - 1));
        return head + " & " + modifiers.get(modifiers.size() - 1);
    }

    /**
     * @return null when no new move landed, otherwise a fragment naming how many of each kind
     *         the <b>finished</b> Pokemon ended up with. Counts up to four, since an alpha with
     *         both rank bonuses fills a whole moveset.
     */
    private static String describeBonusMoves(Pokemon pokemon, List<MoveTemplate> bonusMoves) {
        if (bonusMoves.isEmpty()) {
            return null;
        }
        long eggs = bonusMoves.stream().filter(m -> isEggMove(pokemon, m)).count();
        long tms = bonusMoves.size() - eggs;
        if (eggs > 0 && tms > 0) {
            return "has " + countMoves(eggs, "an", "egg move")
                    + " and " + countMoves(tms, "a", "TM move");
        }
        return eggs > 0
                ? "has " + countMoves(eggs, "an", "egg move")
                : "has " + countMoves(tms, "a", "TM move");
    }

    private static String countMoves(long count, String article, String noun) {
        if (count == 1) {
            return article + " " + noun;
        }
        String spelled = switch ((int) count) {
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            default -> Long.toString(count);
        };
        return spelled + " " + noun + "s";
    }

    private static int perfectIvCount(Pokemon pokemon) {
        int count = 0;
        for (var entry : pokemon.getIvs()) {
            if (entry.getValue() != null && entry.getValue() == IVs.MAX_VALUE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Where each thing came from. Debug-level, and the reason the manual tests are playable at all:
     * without it, telling "the rank gave the shiny" from "the incense did" would take thirty runs.
     */
    @SuppressWarnings("java:S107")   // a debug line has as many inputs as it has facts to report
    private static void logProvenance(double p, double shinyPercent, boolean shinyByRank,
                                      double haPercent, boolean haRolled,
                                      double alphaPercent, boolean alphaRolled,
                                      double median, int targetLevel, List<MoveTemplate> bonusMoves,
                                      WarpRideRewards rewards, int rank,
                                      UltraEncounterModifiers.Applied applied) {
        if (!CobbleSafari.LOGGER.isDebugEnabled()) {
            return;
        }
        CobbleSafari.LOGGER.debug(
                "[WarpRide]   detail: p={} ; shinyRoll={}% {} ; haRoll={}% {} ; alphaRoll={}% {} ; "
                        + "modHaBoost={} ; median={} base={} humongo/teensy={} ; moves={} ; "
                        + "ivTarget={} ; guaranteedShiny={}",
                String.format("%.2f", p),
                String.format("%.1f", shinyPercent), shinyByRank ? "won" : "lost",
                String.format("%.1f", haPercent), haRolled ? "won" : "lost",
                String.format("%.1f", alphaPercent), alphaRolled ? "won" : "lost",
                applied.hiddenAbilityAttempted() ? "granted" : "no",
                String.format("%.1f", median), targetLevel,
                applied.levelDelta() >= 0 ? "+" + applied.levelDelta() : applied.levelDelta(),
                bonusMoves.stream().map(MoveTemplate::getName).toList(),
                rewards.ivPerfectCount(rank),
                applied.guaranteedShinyUsed() ? "consumed" : "not consumed");
    }

    // ------------------------------------------------------------------ helpers

    private static boolean rollPercent(RandomSource random, double percent) {
        return percent > 0.0 && random.nextDouble() * 100.0 < percent;
    }

    private static boolean hasHiddenAbilityProperty(PokemonProperties props) {
        return props.getCustomProperties().stream().anyMatch(HiddenAbilityProperty.class::isInstance);
    }
}
