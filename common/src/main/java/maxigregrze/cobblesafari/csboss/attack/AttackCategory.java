package maxigregrze.cobblesafari.csboss.attack;

/**
 * Catégorie d'effet d'une attaque CSBoss (plan 111). Sert au mode {@code allowSimultaneousAttacks} :
 * deux attaques jouées en même temps doivent appartenir à des catégories différentes.
 *
 * <ul>
 *   <li>{@link #TARGETED} (1/A) — vise des joueurs (ombres/minions/électrodes/anneaux qui suivent ou
 *       ciblent chaque joueur).</li>
 *   <li>{@link #AREA} (2/B) — AOE couvrant le sol via les utils de surface (ex. {@code base_electric_2}).</li>
 *   <li>{@link #SPREAD} (3/C) — envoie des projectiles dans des directions variées/aléatoires
 *       (ex. {@code test}, {@code distortion_1}, {@code base_water_2}).</li>
 * </ul>
 */
public enum AttackCategory {
    TARGETED,
    AREA,
    SPREAD
}
