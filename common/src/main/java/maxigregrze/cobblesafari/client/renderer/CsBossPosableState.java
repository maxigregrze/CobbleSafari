package maxigregrze.cobblesafari.client.renderer;

import com.cobblemon.mod.common.api.scheduling.ClientTaskTracker;
import com.cobblemon.mod.common.api.scheduling.SchedulingTracker;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Entity-timed {@link PosableState} for CSBoss / minion rendering.
 *
 * <p>Cobblemon's {@code FloatingState} is GUI-only: its {@code updatePartialTicks} <b>accumulates</b>
 * the partial-tick fraction every render frame and it has no tick-driven {@code age}. Used on a world
 * entity that renders several times per game tick, that accumulation makes
 * {@code animationSeconds = (age + partialTicks) / 20} advance faster than real time — so the higher
 * the framerate, the faster the animation plays (this is why the Giratina boss animations ran far
 * faster than authored in Blockbench).
 *
 * <p>This state instead mirrors Cobblemon's in-world {@code GenericBedrockClientDelegate}: {@code age}
 * is driven by the entity's {@code tickCount} (set each frame via {@link #updateAge(int)} from the
 * renderer) and {@code updatePartialTicks} <b>replaces</b> the partial-tick fraction rather than
 * accumulating it. The result is real-time, framerate-independent playback that matches the authoring
 * speed.
 */
public class CsBossPosableState extends PosableState {

    @Nullable
    private Entity entity;

    public void setEntity(@Nullable Entity entity) {
        this.entity = entity;
    }

    @Nullable
    @Override
    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public SchedulingTracker getSchedulingTracker() {
        return ClientTaskTracker.INSTANCE;
    }

    @Override
    public void updatePartialTicks(float partialTicks) {
        // Replace (do NOT accumulate): the per-frame interpolation fraction is absolute, so summing it
        // across the multiple frames rendered within a single game tick would speed the animation up.
        setCurrentPartialTicks(partialTicks);
    }
}
