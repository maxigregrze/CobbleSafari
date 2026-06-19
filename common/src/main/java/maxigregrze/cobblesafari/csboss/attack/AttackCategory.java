package maxigregrze.cobblesafari.csboss.attack;

/**
 * Effect category of a CSBoss attack. Used by {@code allowSimultaneousAttacks} mode:
 * two attacks played at the same time must belong to different categories.
 *
 * <ul>
 * <li>{@link #TARGETED} (1/A) — targets players (shadows/minions/electrodes/rings that follow or
 * target each player).</li>
 * <li>{@link #AREA} (2/B) — AOE covering the floor via surface utils (e.g. {@code base_electric_2}).</li>
 * <li>{@link #SPREAD} (3/C) — sends projectiles in varied/random directions
 * (e.g. {@code test}, {@code distortion_1}, {@code base_water_2}).</li>
 * </ul>
 */
public enum AttackCategory {
    TARGETED,
    AREA,
    SPREAD
}
