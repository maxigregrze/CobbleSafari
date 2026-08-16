package maxigregrze.cobblesafari.client.effect;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client-side view of which entities are currently shackled, fed by
 * {@link maxigregrze.cobblesafari.network.ShackleSyncPayload}.
 *
 * <p>Only the chain renderer needs this: the client never receives another entity's active effect
 * list, so {@code hasEffect} would always read {@code false} for anyone but the local player. The
 * local player is answered from its own effect list instead, which vanilla does sync (see
 * {@link #isShackled(LivingEntity)}).</p>
 *
 * <p>Entries are keyed by raw entity id - the payload can arrive before the entity spawns
 * client-side - and stored in an {@link IntOpenHashSet} rather than a {@code Set<Integer>} because
 * {@link #isShackled(LivingEntity)} runs for every rendered living entity, every frame.</p>
 */
public final class ClientShackleTracker {

    private static final IntSet SHACKLED = new IntOpenHashSet();
    private static ClientLevel boundLevel;

    private ClientShackleTracker() {}

    /** Applies a replicated state change. Called on the client thread by the payload receiver. */
    public static void accept(int entityId, boolean shackled) {
        bindCurrentLevel();
        if (shackled) {
            SHACKLED.add(entityId);
        } else {
            SHACKLED.remove(entityId);
        }
    }

    public static boolean isShackled(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity == Minecraft.getInstance().player) {
            return entity.hasEffect(ModEffects.RED_SHACKLED.holder);
        }
        if (SHACKLED.isEmpty()) {
            return false;
        }
        bindCurrentLevel();
        return SHACKLED.contains(entity.getId());
    }

    /**
     * Drops every entry as soon as the current {@link ClientLevel} changes. Reconnecting or changing
     * dimension is the only case where entity ids can be reused by different entities, so this is
     * the whole cleanup: ids left behind by entities that merely went out of view are never queried,
     * since lookups only ever happen for an entity being rendered.
     */
    private static void bindCurrentLevel() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != boundLevel) {
            SHACKLED.clear();
            boundLevel = level;
        }
    }
}
