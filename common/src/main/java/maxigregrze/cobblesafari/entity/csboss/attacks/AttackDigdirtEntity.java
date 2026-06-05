package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Tas de terre fouisseur (plan 113, base_ground_1) : équivalent de l'ombre pour l'attaque sol, rendu
 * avec le modèle {@code attack_digdirt} (texture météorite). Piloté par l'attaque (suit le joueur).
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
