package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Burrowing dirt pile (plan 113, base_ground_1): ground-attack equivalent of the shadow, rendered
 * with the {@code attack_digdirt} model (meteorite texture). Driven by the attack (follows the player).
 */
public class AttackDigdirtEntity extends AbstractAttackEntity {

    public AttackDigdirtEntity(EntityType<? extends AttackDigdirtEntity> type, Level level) {
        super(type, level);
    }

    public static AttackDigdirtEntity spawn(ServerLevel level, double x, double y, double z, int sessionId) {
        AttackDigdirtEntity e = new AttackDigdirtEntity(ModEntities.ATTACK_DIGDIRT, level);
        e.setSessionId(sessionId);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected int maxLifespan() {
        return 400;
    }
}
