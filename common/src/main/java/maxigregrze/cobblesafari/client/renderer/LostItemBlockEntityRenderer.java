package maxigregrze.cobblesafari.client.renderer;

import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.misc.LostItemBlock;
import maxigregrze.cobblesafari.block.misc.LostItemBlockEntity;
import maxigregrze.cobblesafari.block.misc.LostItemSkin;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Draws a Lost Item in up to three parts (action plan 193, sections 6 and 7): the skin's static
 * {@code _empty} model (shadow, rubbles), then - while the local player has not claimed it - the chosen
 * ball through Cobblemon's own 3D item model, and for the floating skin its bobbing aura. The shine skin
 * has no ball: each plane of its model turns on itself instead (action plan 196, section 7.9).
 */
public class LostItemBlockEntityRenderer implements BlockEntityRenderer<LostItemBlockEntity> {

    private static final float PX = 1.0f / 16.0f;
    /** Cobblemon's ball models are 8 px tall; the lost item ball has always been 4 px. */
    private static final float BALL_SCALE = 0.5f;
    /** Half the height of the scaled ball, to spin it around its own centre. */
    private static final float BALL_HALF_HEIGHT = 2.0f * PX;
    private static final float BURIED_TILT_DEGREES = 22.5f;
    /** Same tilt as the small Auspicious Pokeball display model (decision 7.4 a). */
    private static final float FLOATING_TILT_DEGREES = 22.5f;
    /** Presentation constant, tune freely (decision 7.4 b): the Auspicious ball uses 0.5 for a ball twice as big. */
    private static final float LOST_ITEM_BOB_AMPLITUDE = 0.125f;
    /** The floating lost item bobs twice as fast as the Auspicious ball; its spin keeps the shared pace. */
    private static final float LOST_ITEM_BOB_SPEED = 2.0f;
    /** On a wall the model's up axis points at the player: this turns the ball's button towards it too. */
    private static final float WALL_BALL_TURN_DEGREES = 90.0f;
    private static final ResourceLocation FALLBACK_BALL = ResourceLocation.fromNamespaceAndPath("cobblemon", "poke_ball");
    private static final RandomSource SHINE_RANDOM = RandomSource.create(42L);
    /** The shine floats 1.6 px off its support, so its flat plane does not lie on the surface (plan 196, § 7.9). */
    private static final float SHINE_LIFT = 1.6f * PX;

    private final BlockRenderDispatcher blockRenderDispatcher;
    /** One display stack per ball id; Cobblemon swaps in the 3D model for any stack of a ball item. */
    private final Map<String, ItemStack> ballStacks = new HashMap<>();

    public LostItemBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(LostItemBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(LostItemBlock.FACE) || !state.hasProperty(LostItemBlock.FACING)) {
            return;
        }

        UUID localPlayerId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        boolean claimed = localPlayerId != null && blockEntity.hasClaimed(localPlayerId);
        LostItemSkin skin = state.getValue(LostItemBlock.SKIN);
        AttachFace face = state.getValue(LostItemBlock.FACE);
        Direction facing = state.getValue(LostItemBlock.FACING);

        poseStack.pushPose();
        if (state.getValue(LostItemBlock.LOWERED)) {
            // On a road-like floor, one pixel short of a full block: everything sits 1 px lower.
            poseStack.translate(0.0f, -LostItemBlock.LOWERED_OFFSET, 0.0f);
        }
        this.renderParts(blockEntity, partialTick, skin, face, facing, claimed, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderParts(LostItemBlockEntity blockEntity, float partialTick, LostItemSkin skin, AttachFace face,
                             Direction facing, boolean claimed, PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {
        // A skin without a ball is its own item (the shine): drawn only until the local player claims it.
        if (!skin.rendersBall()) {
            if (!claimed) {
                this.renderShine(blockEntity, partialTick, face, facing, poseStack, bufferSource, packedLight, packedOverlay);
            }
            return;
        }
        // The _empty model of each skin is exactly its static part: the ball is drawn separately below.
        BlockState renderState = ModBlocks.LOST_ITEM_VISUAL.defaultBlockState()
                .setValue(LostItemBlock.FACE, face)
                .setValue(LostItemBlock.FACING, facing)
                .setValue(LostItemBlock.HAS_ITEM, false)
                .setValue(LostItemBlock.SKIN, skin);
        poseStack.pushPose();
        blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        if (claimed) {
            return;
        }

        poseStack.pushPose();
        applyBlockstateRotation(poseStack, face, facing);
        if (skin == LostItemSkin.FLOATING) {
            double t = FloatingBallAnimation.time(blockEntity, partialTick);
            float bob = FloatingBallAnimation.bobWave(t * LOST_ITEM_BOB_SPEED) * LOST_ITEM_BOB_AMPLITUDE;

            poseStack.pushPose();
            poseStack.translate(0.0f, bob, 0.0f);
            this.renderModel(poseStack, bufferSource, Sheets.translucentCullBlockSheet(),
                    LostItemModels.get(LostItemModels.FLOATING_AURA), packedLight, packedOverlay);
            poseStack.popPose();

            poseStack.translate(8 * PX, 12 * PX + bob, 8 * PX);
            poseStack.mulPose(Axis.YP.rotationDegrees(FloatingBallAnimation.yawDegrees(t)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(FLOATING_TILT_DEGREES));
            poseStack.translate(0.0f, -BALL_HALF_HEIGHT, 0.0f);
        } else {
            applyStaticBallAnchor(poseStack, skin);
        }
        if (face == AttachFace.WALL) {
            // Turned around its own centre, innermost, so a floating ball still spins with its button facing out.
            poseStack.translate(0.0f, BALL_HALF_HEIGHT, 0.0f);
            poseStack.mulPose(Axis.XP.rotationDegrees(WALL_BALL_TURN_DEGREES));
            poseStack.translate(0.0f, -BALL_HALF_HEIGHT, 0.0f);
        }
        poseStack.scale(BALL_SCALE, BALL_SCALE, BALL_SCALE);
        this.renderBall(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /**
     * Each plane of the shine model turns clockwise around its own centre, as seen from its positive side
     * (above, south, east), at the floating balls' pace. The floor model is split by the axis its quads
     * face, so the planes follow any edit of the model, then placed like the blockstate would place it.
     */
    private void renderShine(LostItemBlockEntity blockEntity, float partialTick, AttachFace face, Direction facing,
                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState modelState = ModBlocks.LOST_ITEM_VISUAL.defaultBlockState()
                .setValue(LostItemBlock.FACE, AttachFace.FLOOR)
                .setValue(LostItemBlock.FACING, Direction.NORTH)
                .setValue(LostItemBlock.HAS_ITEM, false)
                .setValue(LostItemBlock.SKIN, LostItemSkin.SHINE);
        BakedModel model = this.blockRenderDispatcher.getBlockModel(modelState);
        Map<Direction.Axis, List<BakedQuad>> planes = new EnumMap<>(Direction.Axis.class);
        for (BakedQuad quad : model.getQuads(modelState, null, SHINE_RANDOM)) {
            planes.computeIfAbsent(quad.getDirection().getAxis(), axis -> new ArrayList<>()).add(quad);
        }
        VertexConsumer consumer = bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(modelState, false));
        float angle = FloatingBallAnimation.yawDegrees(FloatingBallAnimation.time(blockEntity, partialTick));

        poseStack.pushPose();
        applyBlockstateRotation(poseStack, face, facing);
        poseStack.translate(0.0f, SHINE_LIFT, 0.0f);
        planes.forEach((axis, quads) -> {
            Vector3f centre = centre(quads);
            Axis clockwise = switch (axis) {
                case X -> Axis.XN;
                case Y -> Axis.YN;
                case Z -> Axis.ZN;
            };
            poseStack.pushPose();
            poseStack.translate(centre.x, centre.y, centre.z);
            poseStack.mulPose(clockwise.rotationDegrees(angle));
            poseStack.translate(-centre.x, -centre.y, -centre.z);
            for (BakedQuad quad : quads) {
                consumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
            }
            poseStack.popPose();
        });
        poseStack.popPose();
    }

    /** Centre of the box around the quads' vertices, in block units. */
    private static Vector3f centre(List<BakedQuad> quads) {
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            for (int i = 0; i < 4; i++) {
                Vector3f p = new Vector3f(Float.intBitsToFloat(vertices[i * stride]),
                        Float.intBitsToFloat(vertices[i * stride + 1]),
                        Float.intBitsToFloat(vertices[i * stride + 2]));
                min.min(p);
                max.max(p);
            }
        }
        return min.add(max).mul(0.5f);
    }

    /** Ball positions copied from the authored {@code _full} models, in model pixels (section 6.3). */
    private static void applyStaticBallAnchor(PoseStack poseStack, LostItemSkin skin) {
        switch (skin) {
            case RUBBLES -> poseStack.translate(9 * PX, 0.0f, 8 * PX);
            case CLASSIC -> poseStack.translate(8 * PX, 0.0f, 8 * PX);
            case BURIED -> {
                float pivotY = 0.3125f * PX;
                float pivotZ = 6.9225f * PX;
                poseStack.translate(8 * PX, pivotY, pivotZ);
                poseStack.mulPose(Axis.XP.rotationDegrees(BURIED_TILT_DEGREES));
                poseStack.translate(-8 * PX, -pivotY, -pivotZ);
                poseStack.translate(8 * PX, -1.7f * PX, 8 * PX);
            }
            case FLOATING -> poseStack.translate(8 * PX, 10 * PX, 8 * PX);
            case SHINE -> {
                // No ball: the renderer returns before reaching the anchor.
            }
        }
    }

    /** Mirrors the x / y rotations of {@code lost_item_visual.json}, as BlockModelRotation applies them. */
    private static void applyBlockstateRotation(PoseStack poseStack, AttachFace face, Direction facing) {
        int x = switch (face) {
            case FLOOR -> 0;
            case CEILING -> 180;
            case WALL -> 90;
        };
        int y = switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        if (face == AttachFace.CEILING) {
            // The x = 180 flip mirrors north and south: turned back so it faces the way it does on the floor.
            y = (y + 180) % 360;
        }
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-y));
        poseStack.mulPose(Axis.XP.rotationDegrees(-x));
        poseStack.translate(-0.5f, -0.5f, -0.5f);
    }

    private void renderBall(LostItemBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = this.ballStack(blockEntity.getPokeballId());
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, blockEntity.getLevel(), null, 0);
        this.renderModel(poseStack, bufferSource, Sheets.cutoutBlockSheet(), model, packedLight, packedOverlay);
    }

    private ItemStack ballStack(String id) {
        return this.ballStacks.computeIfAbsent(id, key -> {
            ResourceLocation loc = ResourceLocation.tryParse(key);
            PokeBall ball = loc != null ? PokeBalls.getPokeBall(loc) : null;
            if (ball == null) {
                ball = PokeBalls.getPokeBall(FALLBACK_BALL);
            }
            return ball != null ? new ItemStack(ball.item()) : ItemStack.EMPTY;
        });
    }

    private void renderModel(PoseStack poseStack, MultiBufferSource bufferSource, RenderType renderType, BakedModel model, int packedLight, int packedOverlay) {
        this.blockRenderDispatcher.getModelRenderer().renderModel(poseStack.last(), bufferSource.getBuffer(renderType),
                null, model, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
    }
}
