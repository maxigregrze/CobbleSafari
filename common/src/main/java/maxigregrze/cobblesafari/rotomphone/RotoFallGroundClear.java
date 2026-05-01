package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RotoFallGroundClear {

    private static final int TICKS_ON_GROUND_BEFORE_REMOVE = 5;

    private static final Map<UUID, Integer> CONSECUTIVE_GROUND_TICKS = new ConcurrentHashMap<>();

    private RotoFallGroundClear() {}

    public static void removePlayer(UUID playerId) {
        CONSECUTIVE_GROUND_TICKS.remove(playerId);
    }

    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        UUID id = entity.getUUID();
        if (!entity.onGround()) {
            CONSECUTIVE_GROUND_TICKS.remove(id);
            return;
        }
        if (!entity.hasEffect(ModEffects.ROTO_FALL.holder)) {
            CONSECUTIVE_GROUND_TICKS.remove(id);
            return;
        }
        int next = CONSECUTIVE_GROUND_TICKS.getOrDefault(id, 0) + 1;
        CONSECUTIVE_GROUND_TICKS.put(id, next);
        if (next >= TICKS_ON_GROUND_BEFORE_REMOVE) {
            entity.removeEffect(ModEffects.ROTO_FALL.holder);
            CONSECUTIVE_GROUND_TICKS.remove(id);
        }
    }
}
