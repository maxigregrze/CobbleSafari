package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Orbe de Giratina (plan 113, distortion_4) : rendue avec le modèle/texture du bloc
 * {@code giratina_core_moving}, émissive avec une lueur rouge. Pilotée par l'attaque (suit un joueur
 * à vitesse de marche, flotte 1 bloc au‑dessus du sol). Empoisonne + blesse au contact (géré par l'attaque).
 */
public class AttackGiratinaOrbEntity extends AbstractAttackEntity {

    private static final DustParticleOptions RED_GLOW =
            new DustParticleOptions(new Vector3f(0.85f, 0.05f, 0.05f), 1.3f);

    public AttackGiratinaOrbEntity(EntityType<? extends AttackGiratinaOrbEntity> type, Level level) {
        super(type, level);
    }

    public static AttackGiratinaOrbEntity spawn(ServerLevel level, double x, double y, double z, int sessionId) {
        AttackGiratinaOrbEntity e = new AttackGiratinaOrbEntity(ModEntities.ATTACK_GIRATINA_ORB, level);
        e.setSessionId(sessionId);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void clientTick() {
        // Lueur rouge autour de l'orbe.
        if (this.random.nextInt(2) == 0) {
            this.level().addParticle(RED_GLOW,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                    this.getY() + 0.5 + (this.random.nextDouble() - 0.5) * 0.8,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.8,
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    protected int maxLifespan() {
        return 400;
    }
}
