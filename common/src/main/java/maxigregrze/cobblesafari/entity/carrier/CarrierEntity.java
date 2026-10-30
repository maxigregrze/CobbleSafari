package maxigregrze.cobblesafari.entity.carrier;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Porteur invisible piloté par le serveur (plan 199 § 6) : le passager est monté dessus et suit un
 * {@link CarrierMotion}. Le client interpole un véhicule nativement, d'où un déplacement fluide là où
 * une vitesse joueur renvoyée chaque tick dériverait (voir {@code RampLaunchManager}).
 *
 * <p>Générique : retour des joueurs en fin de combat CSBoss, prise de la tornade {@code base_flying_4},
 * et tout usage futur. Le démontage par Maj, le message « mount.onboard » et la pose assise sont
 * neutralisés par trois mixins ({@code PlayerCarrierDismountMixin},
 * {@code ClientPacketListenerCarrierMountMixin}, {@code LivingEntityRendererCarrierPoseMixin}).</p>
 */
public class CarrierEntity extends Entity {

    private static final String KEY_ELAPSED = "Elapsed";
    private static final String KEY_MOTION = "Motion";
    private static final String KEY_ALLOW_DISMOUNT = "AllowDismount";
    private static final String KEY_TRAIL = "Trail";
    private static final String KEY_RELEASE = "Release";
    /** Porteur jamais occupé (copie orpheline relue d'un chunk après un crash) : retiré après ce délai. */
    private static final int ORPHAN_TICKS = 20;

    @Nullable
    private CarrierMotion motion;
    private int elapsed;
    private boolean allowDismount;
    @Nullable
    private SimpleParticleType trail;
    private Vec3 releaseVelocity = Vec3.ZERO;
    @Nullable
    private Runnable onRelease;
    private boolean hadPassenger;
    private int orphanTicks;

    public CarrierEntity(EntityType<? extends CarrierEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    /** Crée un porteur sur {@code passenger} et l'y monte immédiatement. */
    public static CarrierEntity grab(ServerLevel level, Entity passenger, CarrierMotion motion,
                                     CarrierOptions options) {
        CarrierEntity carrier = new CarrierEntity(ModEntities.CARRIER, level);
        carrier.motion = motion;
        carrier.allowDismount = options.allowDismount();
        carrier.trail = options.trail();
        carrier.releaseVelocity = options.releaseVelocity();
        carrier.onRelease = options.onRelease();
        carrier.setPos(passenger.position());
        level.addFreshEntity(carrier);
        if (options.clearFireOnGrab()) {
            passenger.clearFire();
        }
        passenger.startRiding(carrier, true);
        return carrier;
    }

    public boolean allowsDismount() {
        return allowDismount;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (getPassengers().isEmpty()) {
            // Passager parti (perle, téléport, mort) : le trajet n'a plus d'objet.
            if (hadPassenger || ++orphanTicks >= ORPHAN_TICKS) {
                discard();
            }
            return;
        }
        hadPassenger = true;
        if (motion == null) {
            release();
            return;
        }
        elapsed++;
        Vec3 p = motion.position(elapsed);
        setPos(p.x, p.y, p.z);
        if (trail != null) {
            serverLevel.sendParticles(trail, p.x, p.y + 0.6, p.z, 2, 0.2, 0.3, 0.2, 0.01);
        }
        if (elapsed >= motion.duration()) {
            release();
        }
    }

    /** Dépose le passager à la position courante, applique l'impulsion de sortie puis retire le porteur. */
    public void release() {
        List<Entity> riders = new ArrayList<>(getPassengers());
        ejectPassengers();
        for (Entity rider : riders) {
            rider.resetFallDistance();
            if (releaseVelocity.lengthSqr() > 1.0e-6) {
                rider.setDeltaMovement(releaseVelocity);
                rider.hurtMarked = true; // une seule impulsion, simulée par le client
            }
        }
        if (onRelease != null) {
            onRelease.run();
            onRelease = null;
        }
        discard();
    }

    /** Pieds du passager exactement sur la position du porteur. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return entity.getVehicleAttachmentPoint(this);
    }

    /** Dépose exacte (pas de recherche de place libre vanilla) : la cible du trajet est voulue. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return position();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Aucune donnée synchronisée : position seule.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.elapsed = tag.getInt(KEY_ELAPSED);
        this.motion = tag.contains(KEY_MOTION) ? CarrierMotions.load(tag.getCompound(KEY_MOTION)) : null;
        this.allowDismount = tag.getBoolean(KEY_ALLOW_DISMOUNT);
        this.trail = null;
        if (tag.contains(KEY_TRAIL)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(KEY_TRAIL));
            ParticleType<?> type = id == null ? null : BuiltInRegistries.PARTICLE_TYPE.get(id);
            if (type instanceof SimpleParticleType simple) {
                this.trail = simple;
            }
        }
        this.releaseVelocity = tag.contains(KEY_RELEASE)
                ? CarrierMotion.getVec(tag, KEY_RELEASE) : Vec3.ZERO;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(KEY_ELAPSED, elapsed);
        if (motion != null) {
            tag.put(KEY_MOTION, CarrierMotions.save(motion));
        }
        tag.putBoolean(KEY_ALLOW_DISMOUNT, allowDismount);
        if (trail != null) {
            ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(trail);
            if (id != null) {
                tag.putString(KEY_TRAIL, id.toString());
            }
        }
        CarrierMotion.putVec(tag, KEY_RELEASE, releaseVelocity);
    }
}
