package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.pokestatue.AbstractPokeStatueBlock;
import maxigregrze.cobblesafari.block.pokestatue.AbstractPokeStatueBlockEntity;
import maxigregrze.cobblesafari.block.pokestatue.PokeStatueBlock;
import maxigregrze.cobblesafari.block.pokestatue.PokeStatueBlockEntity;
import maxigregrze.cobblesafari.block.pokestatue.PokeStatueMaterial;
import maxigregrze.cobblesafari.block.pokestatue.PokeStatueRenderStyle;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders every statue block.
 *
 * <ul>
 *   <li><b>Regular, lower half</b> - only the hologram pedestal, which needs a translucent pass.
 *       The plain and tinted pedestals are chunk-baked models.</li>
 *   <li><b>Regular, upper half</b> and <b>Creative Statue</b> - the species model, drawn by
 *       {@link PokeStatueModelRenderer} at the block's own origin height.</li>
 * </ul>
 */
public class PokeStatueBlockEntityRenderer<T extends AbstractPokeStatueBlockEntity>
        implements BlockEntityRenderer<T> {

    private static final Direction[] QUAD_DIRECTIONS = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    /** Bound on the frozen-pose cache; statues out of view are evicted wholesale rather than tracked. */
    private static final int MAX_CACHED_POSES = 256;

    private final BlockRenderDispatcher blockRenderDispatcher;
    private final Map<String, PokeStatueModelRenderer.StatueSpecies> speciesCache = new HashMap<>();
    private final Map<BlockPos, CachedPose> poseCache = new HashMap<>();

    /**
     * A prepared model state plus the configuration it was built from. A playing statue is built at
     * progress 0 and keyed with {@code fixed = false}, so its progress value - kept, but not shown -
     * never forces a rebuild.
     */
    private record CachedPose(String species, String animation, boolean fixed, float progress,
                              PokeStatueModelRenderer.StatueSpecies info,
                              PokeStatueModelRenderer.FrozenPose pose) {
        boolean matches(String s, String a, boolean f, float p) {
            return species.equals(s) && animation.equals(a) && fixed == f && progress == p;
        }
    }

    public PokeStatueBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof AbstractPokeStatueBlock block)) {
            return;
        }
        AbstractPokeStatueBlockEntity data =
                AbstractPokeStatueBlockEntity.dataAt(blockEntity.getLevel(), blockEntity.getBlockPos(), state);
        if (data == null) {
            return;
        }
        // The regular statue's lower half is the pedestal, never the Pokémon.
        if (state.getBlock() instanceof PokeStatueBlock
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            if (data instanceof PokeStatueBlockEntity pedestal) {
                renderPedestal(pedestal, state, poseStack, bufferSource, packedLight);
            }
            return;
        }
        renderPokemon(data, block, state, blockEntity.getBlockPos(), partialTick, poseStack, bufferSource, packedLight);
    }

    // -- pedestal (regular statue, lower half) ------------------------------

    private void renderPedestal(PokeStatueBlockEntity data, BlockState state, PoseStack poseStack,
                                MultiBufferSource bufferSource, int packedLight) {
        // Plain and tinted pedestals are chunk-baked models (see PokeStatueBlock.TINTED); only the
        // hologram needs a renderer pass, because pokestatue_hologram.png has partial alpha and so
        // must be drawn translucent. It replaces the base texture rather than overlaying it, so
        // there is no coplanar twin to z-fight with and no flicker.
        PokeStatueMaterial material = data.getMaterial();
        if (!material.isHologram()) {
            return;
        }
        BlockState displayState = ModBlocks.POKESTATUE_HOLOGRAM_DISPLAY.defaultBlockState();
        int rgb = data.hasColor() ? data.getColor() : PokeStatueMaterial.HOLOGRAM_DEFAULT_COLOR;

        BakedModel model = this.blockRenderDispatcher.getBlockModel(displayState);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        RandomSource random = RandomSource.create();
        random.setSeed(42L);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(modelYaw(state.getValue(PokeStatueBlock.FACING))));
        poseStack.translate(-0.5, 0.0, -0.5);
        var pose = poseStack.last();
        for (Direction dir : QUAD_DIRECTIONS) {
            for (BakedQuad quad : model.getQuads(displayState, dir, random)) {
                consumer.putBulkData(pose, quad, r, g, b, 1f, packedLight, OverlayTexture.NO_OVERLAY);
            }
        }
        poseStack.popPose();
    }

    /**
     * Rotation applied to the pedestal model for a given FACING. Kept in lockstep with the
     * {@code pokemon_statue} blockstate: north = 0, east = 90, south = 180, west = 270.
     */
    private static float modelYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
    }

    // -- the Pokémon --------------------------------------------------------

    private void renderPokemon(AbstractPokeStatueBlockEntity data, AbstractPokeStatueBlock block,
                               BlockState state, BlockPos pos, float partialTick, PoseStack poseStack,
                               MultiBufferSource bufferSource, int packedLight) {
        CachedPose cached = cachedPose(data, pos);
        // A species Cobblemon knows but cannot draw would come out as the substitute: draw nothing
        // instead unless asked to. An empty field resolves to the substitute itself, not to this
        // case, so it keeps its dummy (action plan 190, B7).
        if (cached.info().modelless() && !data.showsDummyWithoutModel()) {
            return;
        }
        float scale = cached.info().fitScale() * data.getScale();
        if (scale <= 0.0001f) {
            return;
        }
        PokeStatueRenderStyle style = data.renderStyle();

        PokeStatueModelRenderer.Playhead playhead = cached.pose().held();
        if (!data.isAnimationFixed() && data.getLevel() != null) {
            // World time, so a paused game pauses the statue and every client sees the same frame.
            double elapsed = (data.getLevel().getGameTime() + partialTick) / 20.0 * data.getAnimationSpeed();
            playhead = PokeStatueModelRenderer.seek(cached.pose(), elapsed);
        }

        poseStack.pushPose();
        poseStack.translate(0.5, block.modelOriginY(), 0.5);
        PokeStatueModelRenderer.render(poseStack, bufferSource, packedLight,
                cached.info(), cached.pose(), playhead, scale, style,
                state.getValue(AbstractPokeStatueBlock.FACING).toYRot());
        poseStack.popPose();
    }

    /**
     * Frozen poses are prepared once and kept: a statue never moves, so rebuilding the model state
     * (and re-resolving the animation through MoLang) every frame would be pure waste.
     */
    private CachedPose cachedPose(AbstractPokeStatueBlockEntity data, BlockPos pos) {
        String species = data.getSpecies();
        String animation = data.getAnimation();
        boolean fixed = data.isAnimationFixed();
        float progress = fixed ? data.getProgress() : 0.0f;
        CachedPose cached = poseCache.get(pos);
        if (cached != null && cached.matches(species, animation, fixed, progress)) {
            return cached;
        }
        if (poseCache.size() >= MAX_CACHED_POSES) {
            poseCache.clear();
        }
        PokeStatueModelRenderer.StatueSpecies info =
                PokeStatueModelRenderer.resolve(species, this.speciesCache);
        CachedPose fresh = new CachedPose(species, animation, fixed, progress, info,
                PokeStatueModelRenderer.freeze(info, animation, progress));
        poseCache.put(pos.immutable(), fresh);
        return fresh;
    }
}
