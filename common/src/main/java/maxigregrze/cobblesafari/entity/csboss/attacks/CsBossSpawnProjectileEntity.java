package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Projectile d'invocation (plan 122 § 3.1) : modèle de bloc {@code bossanchor_moving} qui jaillit
 * du dessus de l'ancre et monte rapidement jusqu'à la hauteur d'apparition du boss. Piloté
 * <b>serveur</b> par {@link maxigregrze.cobblesafari.csboss.BossBattleManager} (pas de physique
 * vanilla) ; supprimé à l'arrivée (explosion + spawn du portail).
 */
public class CsBossSpawnProjectileEntity extends AbstractAttackEntity {

    public CsBossSpawnProjectileEntity(EntityType<? extends CsBossSpawnProjectileEntity> type, Level level) {
        super(type, level);
    }

    public static CsBossSpawnProjectileEntity spawn(ServerLevel level, double x, double y, double z, int sessionId) {
        CsBossSpawnProjectileEntity e = new CsBossSpawnProjectileEntity(ModEntities.CSBOSS_SPAWN_PROJECTILE, level);
        e.setSessionId(sessionId);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected int maxLifespan() {
        return 40; // filet anti-fuite ; la montée dure SUMMON_RISE_TICKS
    }
}
