package maxigregrze.cobblesafari.csboss.attack;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDefinition;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scheduler d'attaques d'une session (plan 100 § 12.1) : entre deux patterns, attend un délai
 * aléatoire ∈ [moveCooldownMin, moveCooldownMax] secondes, puis lance un pattern du pool effectif.
 */
public class AttackScheduler {

    private final RandomSource random = RandomSource.create();
    private final List<String> pool;
    private final int cooldownMinTicks;
    private final int cooldownMaxTicks;

    private CsBossAttack current;
    private int cooldown;

    public AttackScheduler(CsBossDefinition def) {
        this.pool = resolvePool(def);
        this.cooldownMinTicks = def.moveCooldownMin() * 20;
        this.cooldownMaxTicks = def.moveCooldownMax() * 20;
        this.cooldown = rollCooldown();
    }

    /** {@code true} si un pattern d'attaque est en cours (le boss doit alors rester immobile). */
    public boolean isAttacking() {
        return current != null;
    }

    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (current != null) {
            current.tick(level, session, boss);
            if (current.isDone()) {
                current = null;
                cooldown = rollCooldown();
            }
            return;
        }
        if (pool.isEmpty()) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        String id = pool.get(random.nextInt(pool.size()));
        CsBossAttack attack = CsBossAttackRegistry.create(id);
        if (attack == null) {
            cooldown = rollCooldown();
            return;
        }
        current = attack;
        current.begin(level, session, boss);
    }

    private int rollCooldown() {
        if (cooldownMaxTicks <= cooldownMinTicks) {
            return cooldownMinTicks;
        }
        return cooldownMinTicks + random.nextInt(cooldownMaxTicks - cooldownMinTicks + 1);
    }

    private static List<String> resolvePool(CsBossDefinition def) {
        if (def.grantsAllMoves()) {
            return CsBossAttackRegistry.allIds();
        }
        if (def.hasCustomMoveSet()) {
            List<String> out = new ArrayList<>();
            for (String id : def.moveSet()) {
                if (CsBossAttackRegistry.has(id)) {
                    out.add(id);
                } else {
                    CobbleSafari.LOGGER.warn("[CSBoss] boss '{}' references unknown attack '{}' — ignored", def.bossId(), id);
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
        }
        // Pool par type(s) de l'espèce
        List<String> typed = poolFromSpecies(def.specie());
        if (!typed.isEmpty()) {
            return typed;
        }
        // Filet de sécurité
        return List.of(CsBossAttackRegistry.TEST);
    }

    private static List<String> poolFromSpecies(String specie) {
        Set<String> ids = new LinkedHashSet<>();
        try {
            Pokemon mon = PokemonProperties.Companion.parse(specie).create(null);
            addType(ids, mon.getPrimaryType().getName());
            if (mon.getSecondaryType() != null) {
                addType(ids, mon.getSecondaryType().getName());
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("[CSBoss] could not resolve types for '{}'", specie, e);
        }
        return new ArrayList<>(ids);
    }

    private static void addType(Set<String> ids, String typeName) {
        ids.addAll(CsBossAttackRegistry.poolForType(typeName.toLowerCase(Locale.ROOT)));
    }
}
