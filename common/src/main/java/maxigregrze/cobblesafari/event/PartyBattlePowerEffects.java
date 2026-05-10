package maxigregrze.cobblesafari.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.pokemon.stats.SidemodEvSource;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.config.SpawnBoostConfigData;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

public final class PartyBattlePowerEffects {

    private static final String SIDE_MOD_ID = "cobblesafari_party_battle_power";

    private static final List<Stat> EV_STATS = List.of(
            Stats.HP,
            Stats.ATTACK,
            Stats.DEFENCE,
            Stats.SPECIAL_ATTACK,
            Stats.SPECIAL_DEFENCE,
            Stats.SPEED
    );

    private static final ConcurrentHashMap<UUID, Map<UUID, Map<Stat, Integer>>> SNAPSHOTS = new ConcurrentHashMap<>();

    private PartyBattlePowerEffects() {}

    public static void register() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.LOWEST, (Consumer<BattleStartedEvent.Pre>) PartyBattlePowerEffects::onBattlePre);
        CobblemonEvents.BATTLE_STARTED_POST.subscribe(Priority.HIGH, (Consumer<BattleStartedEvent.Post>) PartyBattlePowerEffects::onBattlePost);
    }

    private static Unit onBattlePre(BattleStartedEvent.Pre event) {
        PokemonBattle battle = event.getBattle();
        Map<UUID, Map<Stat, Integer>> perPokemon = new HashMap<>();
        for (BattleActor actor : battle.getActors()) {
            collectActorSnapshots(actor, perPokemon);
        }
        if (!perPokemon.isEmpty()) {
            SNAPSHOTS.put(battle.getBattleId(), perPokemon);
            battle.getOnEndHandlers().add(b -> {
                restoreBattle(b);
                return Unit.INSTANCE;
            });
        }
        return Unit.INSTANCE;
    }

    private static void collectActorSnapshots(BattleActor actor, Map<UUID, Map<Stat, Integer>> perPokemon) {
        if (actor.getType() != ActorType.PLAYER) {
            return;
        }
        ServerPlayer player = resolvePlayer(actor);
        if (player == null || !hasAnyBattleEffect(player)) {
            return;
        }
        for (BattlePokemon bp : actor.getPokemonList()) {
            Pokemon p = bp.getEffectedPokemon();
            if (p.getOwnerPlayer() == player) {
                perPokemon.put(p.getUuid(), snapshot(p));
                p.getEvs().doWithoutEmitting(() -> {
                    applyPowers(player, p);
                    return Unit.INSTANCE;
                });
            }
        }
    }

    private static Unit onBattlePost(BattleStartedEvent.Post event) {
        restoreBattle(event.getBattle());
        return Unit.INSTANCE;
    }

    private static boolean hasAnyBattleEffect(ServerPlayer player) {
        for (int lv = 1; lv <= 3; lv++) {
            if (player.hasEffect(ModPowerEffects.attack(lv))
                    || player.hasEffect(ModPowerEffects.defense(lv))
                    || player.hasEffect(ModPowerEffects.spAtk(lv))
                    || player.hasEffect(ModPowerEffects.spDef(lv))
                    || player.hasEffect(ModPowerEffects.speed(lv))) {
                return true;
            }
            for (int vi = 0; vi < PowerVariantRegistry.VARIANT_COUNT; vi++) {
                if (player.hasEffect(ModPowerEffects.move(vi, lv))
                        || player.hasEffect(ModPowerEffects.resistance(vi, lv))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static ServerPlayer resolvePlayer(BattleActor actor) {
        for (BattlePokemon bp : actor.getPokemonList()) {
            if (bp.getEffectedPokemon().getOwnerPlayer() instanceof ServerPlayer sp) {
                return sp;
            }
        }
        return null;
    }

    private static Map<Stat, Integer> snapshot(Pokemon p) {
        Map<Stat, Integer> m = new HashMap<>();
        for (Stat s : EV_STATS) {
            m.put(s, p.getEvs().getOrDefault(s));
        }
        return m;
    }

    private static void restoreBattle(PokemonBattle battle) {
        Map<UUID, Map<Stat, Integer>> snap = SNAPSHOTS.remove(battle.getBattleId());
        if (snap == null) {
            return;
        }
        for (BattleActor actor : battle.getActors()) {
            if (actor.getType() != ActorType.PLAYER) {
                continue;
            }
            for (BattlePokemon bp : actor.getPokemonList()) {
                Pokemon p = bp.getEffectedPokemon();
                Map<Stat, Integer> row = snap.get(p.getUuid());
                if (row != null) {
                    restore(p, row);
                }
            }
        }
    }

    private static void restore(Pokemon p, Map<Stat, Integer> row) {
        p.getEvs().doWithoutEmitting(() -> {
            for (Stat s : EV_STATS) {
                Integer v = row.get(s);
                if (v != null) {
                    p.getEvs().set(s, v);
                }
            }
            return Unit.INSTANCE;
        });
    }

    private static void applyPowers(ServerPlayer player, Pokemon p) {
        var es = SpawnBoostConfig.data.effectSettings;
        SidemodEvSource src = new SidemodEvSource(SIDE_MOD_ID, p);

        Integer atkLv = findPowerLevel(player, lv -> player.hasEffect(ModPowerEffects.attack(lv)));
        if (atkLv != null) {
            greedyBoostToMultiplier(p, Stats.ATTACK, monoMultiplier(atkLv, es), src);
        }
        Integer defLv = findPowerLevel(player, lv -> player.hasEffect(ModPowerEffects.defense(lv)));
        if (defLv != null) {
            greedyBoostToMultiplier(p, Stats.DEFENCE, monoMultiplier(defLv, es), src);
        }
        Integer spaLv = findPowerLevel(player, lv -> player.hasEffect(ModPowerEffects.spAtk(lv)));
        if (spaLv != null) {
            greedyBoostToMultiplier(p, Stats.SPECIAL_ATTACK, monoMultiplier(spaLv, es), src);
        }
        Integer spdLv = findPowerLevel(player, lv -> player.hasEffect(ModPowerEffects.spDef(lv)));
        if (spdLv != null) {
            greedyBoostToMultiplier(p, Stats.SPECIAL_DEFENCE, monoMultiplier(spdLv, es), src);
        }
        Integer speLv = findPowerLevel(player, lv -> player.hasEffect(ModPowerEffects.speed(lv)));
        if (speLv != null) {
            greedyBoostToMultiplier(p, Stats.SPEED, monoMultiplier(speLv, es), src);
        }

        Integer moveLv = findTypedPowerLevel(player, p, true);
        if (moveLv != null) {
            float m = dualMultiplier(moveLv, es);
            greedyBoostToMultiplier(p, Stats.ATTACK, m, src);
            greedyBoostToMultiplier(p, Stats.SPECIAL_ATTACK, m, src);
        }

        Integer resLv = findTypedPowerLevel(player, p, false);
        if (resLv != null) {
            float m = dualMultiplier(resLv, es);
            greedyBoostToMultiplier(p, Stats.DEFENCE, m, src);
            greedyBoostToMultiplier(p, Stats.SPECIAL_DEFENCE, m, src);
        }
    }

    private static float monoMultiplier(int level, SpawnBoostConfigData.EffectSettings es) {
        return switch (level) {
            case 1 -> es.partyBattleStatPowerLevel1Multiplier;
            case 2 -> es.partyBattleStatPowerLevel2Multiplier;
            case 3 -> es.partyBattleStatPowerLevel3Multiplier;
            default -> 1.0f;
        };
    }

    private static float dualMultiplier(int level, SpawnBoostConfigData.EffectSettings es) {
        return monoMultiplier(level, es) * es.moveResistanceEvBudgetFactor;
    }

    @Nullable
    private static Integer findPowerLevel(ServerPlayer player, IntPredicate hasLevel) {
        for (int lv = 3; lv >= 1; lv--) {
            if (hasLevel.test(lv)) {
                return lv;
            }
        }
        return null;
    }

    @Nullable
    private static Integer findTypedPowerLevel(ServerPlayer player, Pokemon pokemon, boolean move) {
        for (int lv = 3; lv >= 1; lv--) {
            if (hasTypedEffectAtLevel(player, pokemon, lv, move)) {
                return lv;
            }
        }
        return null;
    }

    private static boolean hasTypedEffectAtLevel(ServerPlayer player, Pokemon pokemon, int lv, boolean move) {
        for (int vi = 0; vi < PowerVariantRegistry.ELEMENTAL_COUNT; vi++) {
            if (PowerVariantRegistry.pokemonHasVariantType(pokemon, vi)
                    && hasTypedEffect(player, vi, lv, move)) {
                return true;
            }
        }
        return PowerVariantRegistry.pokemonHasVariantType(pokemon, PowerVariantRegistry.INDEX_ALL)
                && hasTypedEffect(player, PowerVariantRegistry.INDEX_ALL, lv, move);
    }

    private static boolean hasTypedEffect(ServerPlayer player, int vi, int lv, boolean move) {
        return move
                ? player.hasEffect(ModPowerEffects.move(vi, lv))
                : player.hasEffect(ModPowerEffects.resistance(vi, lv));
    }

    private static void greedyBoostToMultiplier(Pokemon p, Stat stat, float targetMultiplier, SidemodEvSource src) {
        if (targetMultiplier <= 1.0f) {
            return;
        }
        int baseline = p.getStat(stat);
        int target = (int) Math.ceil(baseline * (double) targetMultiplier);
        if (target <= baseline) {
            return;
        }
        for (int guard = 0; guard < 400 && p.getStat(stat) < target; guard++) {
            int before = p.getStat(stat);
            boolean progressed = false;
            for (int step : new int[] {4, 3, 2, 1}) {
                int add = p.getEvs().add(stat, step, src);
                if (add != 0) {
                    progressed = true;
                    break;
                }
            }
            if (!progressed || p.getStat(stat) == before) {
                break;
            }
        }
    }
}
