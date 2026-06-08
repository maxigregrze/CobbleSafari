package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.misc.UnionRoomSpotlightBlock;
import maxigregrze.cobblesafari.block.misc.UnionRoomSpotlightBlockEntity;
import maxigregrze.cobblesafari.block.misc.UnionRoomSpotlightDisplayLightBlock;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class UnionRoomSpotlightBlockEntityRenderer implements BlockEntityRenderer<UnionRoomSpotlightBlockEntity> {

    /** Ticks for one full 360° yaw rotation (constant angular speed, one direction). */
    private static final float YAW_PERIOD_TICKS = 90f;
    private static final float PITCH_PERIOD_TICKS = 90f;

    private static final float PITCH_MIN_DEG = 20f;
    private static final float PITCH_MAX_DEG = 30f;

    private final BlockRenderDispatcher blockRenderDispatcher;

    public UnionRoomSpotlightBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(UnionRoomSpotlightBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(UnionRoomSpotlightBlock.FACE) || !state.hasProperty(UnionRoomSpotlightBlock.FACING)) {
            return;
        }

        Block displayBlock = displayLightForSpotlight(state.getBlock());
        if (displayBlock == null) {
            return;
        }

        float t = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + partialTick : partialTick;

        float yawDeg = (t % YAW_PERIOD_TICKS) / YAW_PERIOD_TICKS * 360f;

        float pitchMid = (PITCH_MIN_DEG + PITCH_MAX_DEG) * 0.5f;
        float pitchAmplitude = (PITCH_MAX_DEG - PITCH_MIN_DEG) * 0.5f;
        float pitchDeg = pitchMid + pitchAmplitude * (float) Math.sin(2.0 * Math.PI * t / PITCH_PERIOD_TICKS);

        AttachFace face = state.getValue(UnionRoomSpotlightBlock.FACE);
        Direction facing = state.getValue(UnionRoomSpotlightBlock.FACING);

        BlockState lightState = displayBlock.defaultBlockState()
                .setValue(UnionRoomSpotlightDisplayLightBlock.FACE, face)
                .setValue(UnionRoomSpotlightDisplayLightBlock.FACING, facing);

        poseStack.pushPose();
        applyAnimation(poseStack, face, facing, yawDeg, pitchDeg);
        this.blockRenderDispatcher.renderSingleBlock(lightState, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Nullable
    private static Block displayLightForSpotlight(Block spotlightBlock) {
        if (spotlightBlock == ModBlocks.UNION_ROOM_SPOTLIGHT_GREEN) {
            return ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_GREEN;
        }
        if (spotlightBlock == ModBlocks.UNION_ROOM_SPOTLIGHT_YELLOW) {
            return ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_YELLOW;
        }
        if (spotlightBlock == ModBlocks.UNION_ROOM_SPOTLIGHT_BLUE) {
            return ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_BLUE;
        }
        if (spotlightBlock == ModBlocks.UNION_ROOM_SPOTLIGHT_RED) {
            return ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_RED;
        }
        return null;
    }

    /**
     * Rotates the beam around the lamp base. The pivot is the center of the face the lamp is
     * attached to; the yaw axis is the outward normal of that face (so the beam sweeps a cone
     * pointing away from the wall/floor/ceiling); the pitch axis is perpendicular to the yaw
     * axis in the plane of the attachment face.
     */
    private static void applyAnimation(PoseStack poseStack, AttachFace face, Direction facing,
                                       float yawDeg, float pitchDeg) {
        float pivotX;
        float pivotY;
        float pivotZ;
        Vector3f yawAxis;
        Vector3f pitchAxis;
        float pitchSign;

        switch (face) {
            case FLOOR -> {
                pivotX = 0.5f;
                pivotY = 0f;
                pivotZ = 0.5f;
                yawAxis = new Vector3f(0f, 1f, 0f);
                pitchAxis = new Vector3f(1f, 0f, 0f);
                pitchSign = 1f;
            }
            case CEILING -> {
                pivotX = 0.5f;
                pivotY = 1f;
                pivotZ = 0.5f;
                yawAxis = new Vector3f(0f, 1f, 0f);
                pitchAxis = new Vector3f(1f, 0f, 0f);
                pitchSign = -1f;
            }
            case WALL -> {
                pivotX = 0.5f - 0.5f * facing.getStepX();
                pivotY = 0.5f;
                pivotZ = 0.5f - 0.5f * facing.getStepZ();
                yawAxis = new Vector3f(facing.getStepX(), 0f, facing.getStepZ());
                pitchAxis = new Vector3f(-facing.getStepZ(), 0f, facing.getStepX());
                pitchSign = 1f;
            }
            default -> {
                return;
            }
        }

        poseStack.translate(pivotX, pivotY, pivotZ);
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(yawAxis, yawDeg));
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(pitchAxis, pitchSign * pitchDeg));
        poseStack.translate(-pivotX, -pivotY, -pivotZ);
    }
}
