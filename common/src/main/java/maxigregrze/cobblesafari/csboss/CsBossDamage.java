package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Custom damage sources for the Boss Battle System (datapack damage types).
 * Voir {@code data/cobblesafari/damage_type/csboss_*.json}.
 */
public final class CsBossDamage {
    public static final ResourceKey<DamageType> BULLET = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "csboss_bullet"));
    public static final ResourceKey<DamageType> COWARDICE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "csboss_cowardice"));

    private CsBossDamage() {}

    public static DamageSource bullet(Level level) {
        return source(level, BULLET);
    }

    public static DamageSource cowardice(Level level) {
        return source(level, COWARDICE);
    }

    /**
     * Coup direct d'une attaque CSBoss : applique le damageMultiplier de la phase courante.
     * Seul point d'entrée des dégâts d'attaque (feu, éclairs et effets restent hors multiplicateur).
     */
    public static boolean hurt(ServerPlayer player, @Nullable BossBattleSession session,
                               DamageSource source, float baseDamage) {
        double mult = session == null ? CsBossDefinition.DEFAULT_DAMAGE_MULTIPLIER
                : session.getDefinition().damageMultiplier();
        return player.hurt(source, (float) (baseDamage * mult));
    }

    /**
     * {@code true} while {@code player} is still a live fighter in {@code level} after a hit. A lethal
     * hit in a timed dimension is cancelled by the timer rescue, which revives the player at 1 HP and
     * teleports them out <em>inside</em> {@code hurt}: fire, knockback, launches and grabs that follow
     * a hit must then be skipped, or they land on the rescued player in the origin dimension.
     */
    public static boolean stillInFight(ServerPlayer player, net.minecraft.world.level.Level level) {
        return player.isAlive() && !player.isRemoved() && player.level() == level;
    }

    /** Variante avec la source {@code csboss_bullet}. */
    public static boolean hurt(ServerPlayer player, @Nullable BossBattleSession session, float baseDamage) {
        return hurt(player, session, bullet(player.level()), baseDamage);
    }

    private static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
    }
}
