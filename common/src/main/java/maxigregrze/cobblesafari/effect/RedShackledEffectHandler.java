package maxigregrze.cobblesafari.effect;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.network.ModNetworking;
import maxigregrze.cobblesafari.safari.SafariStateManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RedShackledEffectHandler {

    private static final Map<UUID, FrozenAnchor> ANCHORS = new HashMap<>();
    private static final double DRIFT_EPSILON_SQR = 0.0001;

    // The shackle flips NoGravity / NoAI on the entity itself, and both are saved with it. Which
    // flag the shackle turned on is therefore saved with it too, as a vanilla entity tag: after a
    // restart the in-memory anchor is gone, and the tag is what says the flag must go back to false.
    // A flag that was already true (set by an admin) never gets a tag and is never touched.
    private static final String TAG_NO_GRAVITY = "cobblesafari.shackle_nogravity";
    private static final String TAG_NO_AI = "cobblesafari.shackle_noai";

    private RedShackledEffectHandler() {}

    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (!RedShackledEffects.isShackled(entity)) {
            clearAnchor(entity);
            return;
        }

        FrozenAnchor anchor = ANCHORS.get(entity.getUUID());
        if (anchor == null) {
            anchor = createAnchor(entity);
            ANCHORS.put(entity.getUUID(), anchor);
            onShackleStarted(entity);
        }

        entity.setDeltaMovement(Vec3.ZERO);
        applyNoGravity(entity);
        if (entity instanceof Mob mob && !mob.isNoAi()) {
            mob.addTag(TAG_NO_AI);
            mob.setNoAi(true);
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
        applyNoGravity(entity);
        return anchor;
    }

    private static void applyNoGravity(LivingEntity entity) {
        if (!entity.isNoGravity()) {
            entity.addTag(TAG_NO_GRAVITY);
            entity.setNoGravity(true);
        }
    }

    // Also reached with no anchor: an entity saved while shackled whose effect ran out before it
    // was loaded again still carries the tags, and is restored on its first tick.
    private static void clearAnchor(LivingEntity entity) {
        FrozenAnchor anchor = ANCHORS.remove(entity.getUUID());
        boolean tagged = entity.getTags().contains(TAG_NO_GRAVITY) || entity.getTags().contains(TAG_NO_AI);
        if ((anchor == null && !tagged) || entity.isRemoved()) {
            return;
        }

        if (entity.removeTag(TAG_NO_GRAVITY)) {
            entity.setNoGravity(false);
        }
        if (entity instanceof Mob mob && mob.removeTag(TAG_NO_AI)) {
            mob.setNoAi(false);
        }
        onShackleEnded(entity);
    }

    // Single server-side detection point for both shackle transitions, so every application path
    // (thrown red chain, boss attack, /effect) feeds the Safari flee suspension and the client sync.
    private static void onShackleStarted(LivingEntity entity) {
        ModNetworking.sendShackleSync(entity, true);
        if (entity instanceof PokemonEntity pokemon) {
            SafariStateManager.suspendFleeForShackle(pokemon);
        }
    }

    // Not reached for a removed entity (clearAnchor short-circuits): the client destroys the entity
    // on its own, and the tracker entry it leaves behind is never queried again.
    private static void onShackleEnded(LivingEntity entity) {
        ModNetworking.sendShackleSync(entity, false);
        if (entity instanceof PokemonEntity pokemon) {
            SafariStateManager.resumeFleeAfterShackle(pokemon);
        }
    }

    private static final class FrozenAnchor {
        double x;
        double y;
        double z;
        float yRot;
        float xRot;
    }
}
