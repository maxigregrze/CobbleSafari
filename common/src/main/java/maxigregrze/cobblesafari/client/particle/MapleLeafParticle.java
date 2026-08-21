package maxigregrze.cobblesafari.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A maple leaf drifting to the ground. Modelled on vanilla's cherry-blossom particle - slow fall,
 * a widening spiral, a lazy spin - but driven by a single sprite per colour rather than a
 * twelve-frame sheet, and with a much shorter life since these are shed continuously by a whole
 * canopy rather than by a handful of blossoms.
 */
public class MapleLeafParticle extends TextureSheetParticle {

    private static final int LIFETIME = 110;
    private static final float DRIFT_SCALE = 0.0022F;

    private final float phase;
    private final float spinAcceleration;
    private float rotSpeed;

    protected MapleLeafParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.phase = this.random.nextFloat();
        this.rotSpeed = (float) Math.toRadians(this.random.nextBoolean() ? -28.0 : 28.0);
        this.spinAcceleration = (float) Math.toRadians(this.random.nextBoolean() ? -5.0 : 5.0);
        this.lifetime = LIFETIME + this.random.nextInt(40);
        this.gravity = 8.0E-4F;
        this.friction = 1.0F;
        float size = this.random.nextBoolean() ? 0.06F : 0.085F;
        this.quadSize = size;
        this.setSize(size, size);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            this.remove();
            return;
        }
        float age = (float) (LIFETIME - this.lifetime);
        float curve = Math.min(age / LIFETIME, 1.0F);
        double angle = Math.toRadians(this.phase * 360.0F);
        double swing = Math.pow(curve, 1.25);
        this.xd += Math.cos(angle) * swing * DRIFT_SCALE;
        this.zd += Math.sin(angle) * swing * DRIFT_SCALE;
        this.yd -= this.gravity;
        this.rotSpeed += this.spinAcceleration / 20.0F;
        this.oRoll = this.roll;
        this.roll += this.rotSpeed / 20.0F;
        this.move(this.xd, this.yd, this.zd);
        if (this.onGround) {
            this.remove();
        }
    }

    /** Shared by the three colours; each gets its own registered type and sprite set. */
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new MapleLeafParticle(level, x, y, z, this.sprites);
        }
    }
}
