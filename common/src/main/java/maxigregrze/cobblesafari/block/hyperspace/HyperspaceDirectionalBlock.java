package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Orientable (N/E/S/W) Hyperspace block with a custom model, now a thin configuration of
 * the shared {@link HorizontalModelBlock}:
 * <ul>
 *   <li>{@code wallMounted=true} - attaches to the wall behind ({@link HorizontalModelBlock.Support#WALL});
 *       the shape is authored for {@link Direction#SOUTH} (the wall blocks render their base model facing south).</li>
 *   <li>{@code wallMounted=false} - free‑standing ({@link HorizontalModelBlock.Support#NONE}), shape authored NORTH.</li>
 *   <li>{@code hasCollision=false} - selection only (decorative banners / neon).</li>
 *   <li>{@code collisionShape != null} - collide with only part of the outline (e.g. the flag's
 *       arm, so the neon panel itself stays walk-through).</li>
 * </ul>
 * Reused by small neon (wall, arm-only collision), iron railing (wall), shutters (wall).
 */
public class HyperspaceDirectionalBlock extends HorizontalModelBlock {

    private final VoxelShape northShape;
    private final boolean wallMounted;
    private final boolean hasCollision;
    @Nullable
    private final VoxelShape collisionShape;

    public HyperspaceDirectionalBlock(Properties properties, VoxelShape northShape, boolean wallMounted, boolean hasCollision) {
        this(properties, northShape, wallMounted, hasCollision, null);
    }

    public HyperspaceDirectionalBlock(Properties properties, VoxelShape northShape, boolean wallMounted,
                                      boolean hasCollision, @Nullable VoxelShape collisionShape) {
        super(properties, buildSettings(northShape, wallMounted, hasCollision, collisionShape));
        this.northShape = northShape;
        this.wallMounted = wallMounted;
        this.hasCollision = hasCollision;
        this.collisionShape = collisionShape;
    }

    private static Settings buildSettings(VoxelShape northShape, boolean wallMounted,
                                          boolean hasCollision, @Nullable VoxelShape collisionShape) {
        Settings.Builder builder = Settings.builder()
                .shape(northShape)
                .authoredFacing(wallMounted ? Direction.SOUTH : Direction.NORTH)
                .rotateShape()
                .support(wallMounted ? Support.WALL : Support.NONE)
                .collision(hasCollision ? Collision.SHAPE : Collision.NONE);
        if (collisionShape != null) {
            builder.collisionShape(collisionShape);
        }
        return builder.build();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new HyperspaceDirectionalBlock(props, this.northShape, this.wallMounted,
                this.hasCollision, this.collisionShape));
    }
}
