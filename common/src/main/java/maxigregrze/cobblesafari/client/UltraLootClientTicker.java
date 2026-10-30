package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.item.ultraloot.ReconArmorLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Client tick entry point of the Ultra Space loot (action plan 192), called by both loaders. */
public final class UltraLootClientTicker {

    // Public: the warp ride screen draws the same trail on its rider (action plan 200, section 4.2).
    /** Backpack nozzle, relative to a gliding player: behind along the flight, up on the back. */
    public static final double NOZZLE_BACK = 0.35;
    public static final double NOZZLE_UP = 0.4;
    /**
     * Bottom edge of each spread wing: further back than the nozzle, out to the side. Tuned in game:
     * side distance doubled (0.6 → 1.2), raised by a third of the former side distance (0.3 → 0.5),
     * then moved 0.3 forward, towards the head (0.7 → 0.4).
     */
    public static final double WING_TIP_BACK = 0.4;
    public static final double WING_TIP_SIDE = 1.2;
    public static final double WING_TIP_UP = 0.5;
    private static final double TRAIL_SPEED = 0.08;
    public static final float[] WING_TRAIL_RGB = {0.35f, 0.65f, 1.0f};

    private UltraLootClientTicker() {}

    public static void onClientTick(Minecraft minecraft) {
        StakatakaShieldAnimator.onClientTick(minecraft);
        if (minecraft.level != null && !minecraft.isPaused()) {
            for (Player player : minecraft.level.players()) {
                if (player.isFallFlying() && ReconArmorLogic.canGlide(player)) {
                    emitTrails(minecraft, player);
                }
            }
        }
    }

    /**
     * Section 10.4: every tick of glide, a firework spark and a puff of smoke from the backpack, and
     * the same pair, tinted blue, from the bottom edge of each wing.
     */
    private static void emitTrails(Minecraft minecraft, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 exhaust = look.scale(-TRAIL_SPEED);
        Vec3 nozzle = player.position().add(0.0, NOZZLE_UP, 0.0).subtract(look.scale(NOZZLE_BACK));
        emitPair(minecraft, nozzle, exhaust, null);

        Vec3 side = new Vec3(-look.z, 0.0, look.x);
        side = side.lengthSqr() < 1.0e-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 wingBase = player.position().add(0.0, WING_TIP_UP, 0.0).subtract(look.scale(WING_TIP_BACK));
        emitPair(minecraft, wingBase.add(side.scale(WING_TIP_SIDE)), exhaust, WING_TRAIL_RGB);
        emitPair(minecraft, wingBase.subtract(side.scale(WING_TIP_SIDE)), exhaust, WING_TRAIL_RGB);
    }

    private static void emitPair(Minecraft minecraft, Vec3 at, Vec3 velocity, float[] tint) {
        spawn(minecraft, ParticleTypes.FIREWORK, at, velocity, tint);
        spawn(minecraft, ParticleTypes.SMOKE, at, new Vec3(0.0, 0.01, 0.0), tint);
    }

    private static void spawn(Minecraft minecraft, SimpleParticleType type, Vec3 at, Vec3 velocity, float[] tint) {
        Particle particle = minecraft.particleEngine.createParticle(type, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
        if (particle != null && tint != null) {
            particle.setColor(tint[0], tint[1], tint[2]);
        }
    }
}
