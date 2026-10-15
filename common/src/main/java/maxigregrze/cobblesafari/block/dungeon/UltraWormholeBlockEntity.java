package maxigregrze.cobblesafari.block.dungeon;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The return portal of a sub-instance. Carries one field - the destination's {@code typeId} - and
 * the two angles its renderer interpolates (action plan 183, section 4.2).
 *
 * <p>The motion is {@code DistortionPortalBlockEntity}'s, with a single deliberate difference:
 * when no player is within the tracking radius this portal <b>holds its last orientation</b>
 * instead of drifting round the Y axis. That is the {@code else} branch of the distortion portal,
 * removed - {@code targetYaw} is simply left alone and the Z spin carries on by itself.</p>
 */
public class UltraWormholeBlockEntity extends BlockEntity {

    private static final String NBT_TYPE_ID = "TypeId";

    private static final double TRACKING_RADIUS = 16.0;
    private static final float TRACKING_SMOOTHING = 0.25f;
    /** Shared with the warp ride screen, whose portals spin at the same rate (action plan 189). */
    public static final float SPIN_Z_DEGREES_PER_TICK = 1.5f;

    /** Which destination this portal belongs to. Drives the tint; never interpreted server-side. */
    private int typeId;

    private float currentYaw;
    private float previousYaw;
    private float targetYaw;
    private float currentSpinZ;
    private float previousSpinZ;

    public UltraWormholeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ULTRA_WORMHOLE, pos, state);
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, UltraWormholeBlockEntity blockEntity) {
        if (!level.isClientSide() || !state.is(ModBlocks.ULTRA_WORMHOLE)) {
            return;
        }
        blockEntity.previousYaw = blockEntity.currentYaw;
        blockEntity.previousSpinZ = blockEntity.currentSpinZ;
        blockEntity.currentSpinZ += SPIN_Z_DEGREES_PER_TICK;

        Player nearest = level.getNearestPlayer(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, TRACKING_RADIUS, false);
        if (nearest != null) {
            double dx = nearest.getX() - (pos.getX() + 0.5);
            double dz = nearest.getZ() - (pos.getZ() + 0.5);
            blockEntity.targetYaw = 90.0f - (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI));
        }
        // No else: out of range the target is left where it was, so the portal keeps facing
        // whoever was last near it. This is the one departure from the distortion portal.

        float delta = Mth.wrapDegrees(blockEntity.targetYaw - blockEntity.currentYaw);
        blockEntity.currentYaw = Mth.wrapDegrees(blockEntity.currentYaw + delta * TRACKING_SMOOTHING);
    }

    public float getCurrentYaw() {
        return currentYaw;
    }

    public float getPreviousYaw() {
        return previousYaw;
    }

    public float getCurrentSpinZ() {
        return currentSpinZ;
    }

    public float getPreviousSpinZ() {
        return previousSpinZ;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        typeId = tag.getInt(NBT_TYPE_ID);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(NBT_TYPE_ID, typeId);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(NBT_TYPE_ID, typeId);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
