package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Reusable directional beam entity rendered with the vanilla <b>beacon beam</b> texture. Its
 * orientation comes from the entity yaw/pitch (so it interpolates client-side), and its length /
 * colour are synced. Purely visual — collision/damage is handled by the driving attack.
 */
public class AttackBeamEntity extends AbstractAttackEntity {

    private static final EntityDataAccessor<Integer> DATA_LENGTH =
            SynchedEntityData.defineId(AttackBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(AttackBeamEntity.class, EntityDataSerializers.INT);

    public AttackBeamEntity(EntityType<? extends AttackBeamEntity> type, Level level) {
        super(type, level);
    }

    public static AttackBeamEntity spawn(ServerLevel level, double x, double y, double z,
                                         int sessionId, int length, int colorArgb) {
        AttackBeamEntity e = new AttackBeamEntity(ModEntities.ATTACK_BEAM, level);
        e.setSessionId(sessionId);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        e.setLength(length);
        e.setColor(colorArgb);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LENGTH, 16);
        builder.define(DATA_COLOR, 0xFFFFFFFF);
    }

    public int getLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public void setLength(int length) {
        this.entityData.set(DATA_LENGTH, Math.max(1, length));
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(int colorArgb) {
        this.entityData.set(DATA_COLOR, colorArgb);
    }

    /** Aims the beam along a direction, encoded into the entity yaw/pitch (Minecraft convention). */
    public void setDirection(Vec3 dir) {
        Vec3 d = dir.normalize();
        float pitch = (float) Math.toDegrees(Math.asin(-d.y));
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    /** Inflated so the long beam is never frustum-culled when its origin is off-screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(getLength());
    }

    @Override
    protected int maxLifespan() {
        return 600; // safety TTL; the driving attack removes it earlier
    }
}
