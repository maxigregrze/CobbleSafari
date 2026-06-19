package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Summon projectile: {@code bossanchor_moving} block model that bursts from above the anchor
 * and rises quickly to the boss spawn height. <b>Server</b>-driven by
 * {@link maxigregrze.cobblesafari.csboss.BossBattleManager} (no vanilla physics);
 * removed on arrival (explosion + portal spawn).
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
        return 40; // leak safety net; ascent lasts SUMMON_RISE_TICKS
    }
}
