package maxigregrze.cobblesafari.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RedShackledEffectHandler {

    private static final Map<UUID, FrozenAnchor> ANCHORS = new HashMap<>();
    private static final double DRIFT_EPSILON_SQR = 0.0001;

    private RedShackledEffectHandler() {}

    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (!RedShackledEffects.isShackled(entity)) {
            clearAnchor(entity);
            return;
        }

        FrozenAnchor anchor = ANCHORS.computeIfAbsent(entity.getUUID(), id -> createAnchor(entity));

        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);

        if (entity instanceof Mob mob) {
            if (!anchor.noAiApplied) {
                anchor.hadNoAi = mob.isNoAi();
                mob.setNoAi(true);
                anchor.noAiApplied = true;
            }
        }

        entity.setYRot(anchor.yRot);
        entity.setXRot(anchor.xRot);
        entity.setYHeadRot(anchor.yRot);

        double dx = entity.getX() - anchor.x;
        double dy = entity.getY() - anchor.y;
        double dz = entity.getZ() - anchor.z;
        if (dx * dx + dy * dy + dz * dz > DRIFT_EPSILON_SQR) {
            entity.teleportTo(anchor.x, anchor.y, anchor.z);
        }
    }

    private static FrozenAnchor createAnchor(LivingEntity entity) {
        FrozenAnchor anchor = new FrozenAnchor();
        anchor.x = entity.getX();
        anchor.y = entity.getY();
        anchor.z = entity.getZ();
        anchor.yRot = entity.getYRot();
        anchor.xRot = entity.getXRot();
        anchor.hadNoGravity = entity.isNoGravity();
        entity.setNoGravity(true);
        if (entity instanceof RedShackledSynced synced) {
            synced.cobblesafari$setShackledSynced(true);
        }
        return anchor;
    }

    private static void clearAnchor(LivingEntity entity) {
        FrozenAnchor anchor = ANCHORS.remove(entity.getUUID());
        if (anchor == null || entity.isRemoved()) {
            return;
        }

        if (entity instanceof RedShackledSynced synced) {
            synced.cobblesafari$setShackledSynced(false);
        }
        entity.setNoGravity(anchor.hadNoGravity);
        if (entity instanceof Mob mob && anchor.noAiApplied) {
            mob.setNoAi(anchor.hadNoAi);
        }
    }

    private static final class FrozenAnchor {
        double x;
        double y;
        double z;
        float yRot;
        float xRot;
        boolean hadNoGravity;
        boolean hadNoAi;
        boolean noAiApplied;
    }
}
