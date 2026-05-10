package maxigregrze.cobblesafari.event;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.config.SpawnBoostConfigData;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.UUID;

public final class SelfCombatPowerEffects {

    private SelfCombatPowerEffects() {}

    public static float modifyDamageAmount(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0.0f || !Float.isFinite(amount)) {
            return amount;
        }
        float out = amount;
        out = applySelfAttackIfApplicable(victim, source, out);
        out = applySelfDefenseIfApplicable(victim, source, out);
        if (!Float.isFinite(out) || out < 0.0f) {
            return amount;
        }
        return out;
    }

    private static float applySelfAttackIfApplicable(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof PokemonEntity pe)) {
            return amount;
        }
        Pokemon pokemon = pe.getPokemon();
        if (!pokemon.isWild()) {
            return amount;
        }
        ServerPlayer attacker = resolvePlayerAttacker(source);
        if (attacker == null) {
            return amount;
        }
        Integer lv = findMatchingTypedPowerLevel(attacker, pokemon, true);
        if (lv == null) {
            return amount;
        }
        float m = selfAttackMultiplier(lv, SpawnBoostConfig.data.effectSettings);
        return amount * m;
    }

    private static float applySelfDefenseIfApplicable(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer player)) {
            return amount;
        }
        PokemonEntity attackerPoke = resolvePokemonAttacker(source);
        if (attackerPoke == null) {
            return amount;
        }
        if (!isHostilePokemonToward(attackerPoke, player)) {
            return amount;
        }
        Integer lv = findMatchingTypedPowerLevel(player, attackerPoke.getPokemon(), false);
        if (lv == null) {
            return amount;
        }
        float m = selfDefenseMultiplier(lv, SpawnBoostConfig.data.effectSettings);
        return amount * m;
    }

    private static boolean isHostilePokemonToward(PokemonEntity attacker, ServerPlayer victim) {
        UUID owner = attacker.getOwnerUUID();
        return !Objects.equals(owner, victim.getUUID());
    }

    private static ServerPlayer resolvePlayerAttacker(DamageSource source) {
        Entity e = source.getEntity();
        if (e instanceof ServerPlayer sp) {
            return sp;
        }
        e = source.getDirectEntity();
        if (e instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
    }

    private static PokemonEntity resolvePokemonAttacker(DamageSource source) {
        Entity e = source.getEntity();
        if (e instanceof PokemonEntity pe) {
            return pe;
        }
        e = source.getDirectEntity();
        if (e instanceof PokemonEntity pe) {
            return pe;
        }
        return null;
    }

    private static Integer findMatchingTypedPowerLevel(ServerPlayer player, Pokemon pokemon, boolean selfAttack) {
        for (int lv = 3; lv >= 1; lv--) {
            Integer found = findTypedSelfPowerAtLevel(player, pokemon, selfAttack, lv);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Integer findTypedSelfPowerAtLevel(ServerPlayer player, Pokemon pokemon, boolean selfAttack, int lv) {
        for (int vi = 0; vi < PowerVariantRegistry.ELEMENTAL_COUNT; vi++) {
            if (!PowerVariantRegistry.pokemonHasVariantType(pokemon, vi)) {
                continue;
            }
            if (hasSelfPower(player, selfAttack, vi, lv)) {
                return lv;
            }
        }
        if (hasSelfPower(player, selfAttack, PowerVariantRegistry.INDEX_ALL, lv)) {
            return lv;
        }
        return null;
    }

    private static boolean hasSelfPower(ServerPlayer player, boolean selfAttack, int variantIndex, int level) {
        return selfAttack
                ? player.hasEffect(ModPowerEffects.selfAttack(variantIndex, level))
                : player.hasEffect(ModPowerEffects.selfDefense(variantIndex, level));
    }

    private static float selfAttackMultiplier(int level, SpawnBoostConfigData.EffectSettings es) {
        return switch (level) {
            case 1 -> es.selfAttackPowerLevel1DamageMultiplier;
            case 2 -> es.selfAttackPowerLevel2DamageMultiplier;
            case 3 -> es.selfAttackPowerLevel3DamageMultiplier;
            default -> 1.0f;
        };
    }

    private static float selfDefenseMultiplier(int level, SpawnBoostConfigData.EffectSettings es) {
        return switch (level) {
            case 1 -> es.selfDefensePowerLevel1DamageTakenMultiplier;
            case 2 -> es.selfDefensePowerLevel2DamageTakenMultiplier;
            case 3 -> es.selfDefensePowerLevel3DamageTakenMultiplier;
            default -> 1.0f;
        };
    }
}
