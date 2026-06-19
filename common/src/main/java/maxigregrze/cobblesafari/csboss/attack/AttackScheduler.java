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
 * Attack scheduler for a session: between two patterns, waits a random delay
 * ∈ [moveCooldownMin, moveCooldownMax] seconds, then launches a pattern from the effective pool.
 */
public class AttackScheduler {

    private final RandomSource random = RandomSource.create();
    private final List<String> pool;
    private final int cooldownMinTicks;
    private final int cooldownMaxTicks;
    private final boolean allowSimultaneous;

    /** Attacks in progress (1, or 2 of different categories if {@code allowSimultaneousAttacks}). */
    private final List<CsBossAttack> active = new ArrayList<>();
    private int cooldown;

    public AttackScheduler(CsBossDefinition def) {
        this.pool = resolvePool(def);
        this.cooldownMinTicks = def.moveCooldownMin() * 20;
        this.cooldownMaxTicks = def.moveCooldownMax() * 20;
        this.allowSimultaneous = def.allowSimultaneousAttacks();
        this.cooldown = rollCooldown();
    }

    /** {@code true} if an attack pattern is in progress (the boss must then stay immobile). */
    public boolean isAttacking() {
        return !active.isEmpty();
    }

    /** {@code true} if an in-progress pattern controls boss orientation (cf. {@code distortion_1}). */
    public boolean attackControlsRotation() {
        for (CsBossAttack a : active) {
            if (a.controlsBossRotation()) {
                return true;
            }
        }
        return false;
    }

    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (!active.isEmpty()) {
            boolean allDone = true;
            for (CsBossAttack a : active) {
                a.tick(level, session, boss);
                if (!a.isDone()) {
                    allDone = false;
                }
            }
            // The pair ends when the longer of the two attacks finishes.
            if (allDone) {
                active.clear();
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
        startAttacks(level, session, boss);
    }

    private void startAttacks(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        CsBossAttack first = CsBossAttackRegistry.create(pool.get(random.nextInt(pool.size())));
        if (first == null) {
            cooldown = rollCooldown();
            return;
        }
        first.begin(level, session, boss);
        active.add(first);

        if (allowSimultaneous) {
            // Second attack of a different category (otherwise only one is played).
            CsBossAttack second = pickDifferentCategory(first.category());
            if (second != null) {
                second.begin(level, session, boss);
                active.add(second);
            }
        }
    }

    private CsBossAttack pickDifferentCategory(AttackCategory exclude) {
        List<CsBossAttack> candidates = new ArrayList<>();
        for (String id : pool) {
            CsBossAttack a = CsBossAttackRegistry.create(id);
            if (a != null && a.category() != exclude) {
                candidates.add(a);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
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
        // Pool by species type(s)
        List<String> typed = poolFromSpecies(def.specie());
        if (!typed.isEmpty()) {
            return typed;
        }
        // Safety net
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
