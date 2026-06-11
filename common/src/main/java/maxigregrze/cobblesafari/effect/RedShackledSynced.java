package maxigregrze.cobblesafari.effect;

/**
 * Duck interface mixed into {@link net.minecraft.world.entity.LivingEntity} to expose a
 * client-replicated "shackled" flag.
 *
 * <p>Vanilla only syncs a player's own active mob effects to that player's client; the active
 * effect list of any other entity (mobs, wild Pokémon, the boss, other players) is never sent to
 * watching clients. The world chain renderer therefore cannot rely on {@code hasEffect(...)} —
 * it would always read {@code false}. This flag is set server-side from the authoritative effect
 * state and rides along with the entity's {@link net.minecraft.network.syncher.SynchedEntityData},
 * so every tracking client (including late joiners) knows which entities are shackled.</p>
 */
public interface RedShackledSynced {

    void cobblesafari$setShackledSynced(boolean shackled);

    boolean cobblesafari$isShackledSynced();
}
