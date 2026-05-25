package maxigregrze.cobblesafari.block.misc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns a vanilla wind-charge burst at a position (same knockback as {@link WindCharge} explosion).
 */
public class WhirlwindWindBurst extends WindCharge {

    public WhirlwindWindBurst(ServerLevel level, Vec3 center) {
        super(level, center.x, center.y, center.z, Vec3.ZERO);
    }

    public void burst(Vec3 pos) {
        this.explode(pos);
    }
}
