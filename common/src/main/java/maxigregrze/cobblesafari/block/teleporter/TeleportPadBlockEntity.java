package maxigregrze.cobblesafari.block.teleporter;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.network.OpenTeleportPadConfigPayload;
import maxigregrze.cobblesafari.teleporter.TeleportPadManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Teleport pad block entity: stores the link to its partner as a <b>facing-relative</b>
 * offset {@code (forward, up, right)} so it survives structure rotation / mirroring
 * (only {@code FACING} is rotated, the offset is re-projected to world space at use time).
 */
public class TeleportPadBlockEntity extends BlockEntity {

    public static final int DEFAULT_TINT_COLOR = 0xf4f980;

    private static final String KEY_FORWARD = "LinkF";
    private static final String KEY_UP = "LinkU";
    private static final String KEY_RIGHT = "LinkR";
    private static final String KEY_LINKED = "Linked";
    private static final String KEY_TINT_COLOR = "TintColor";

    private int linkForward;
    private int linkUp;
    private int linkRight;
    private boolean linked;
    private int tintColor = DEFAULT_TINT_COLOR;

    public TeleportPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEPORT_PAD, pos, state);
    }

    public boolean isLinked() {
        return linked;
    }

    public int getLinkForward() {
        return linkForward;
    }

    public int getLinkUp() {
        return linkUp;
    }

    public int getLinkRight() {
        return linkRight;
    }

    public int getTintColor() {
        return tintColor;
    }

    public void setTintColor(int tintColor) {
        this.tintColor = tintColor & 0xFFFFFF;
        syncToClients();
    }

    public void setLink(int forward, int up, int right) {
        this.linkForward = forward;
        this.linkUp = up;
        this.linkRight = right;
        this.linked = true;
        syncToClients();
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            TeleportPadBlock.setConnected(level, worldPosition, true);
        }
    }

    public void clearLink() {
        this.linkForward = 0;
        this.linkUp = 0;
        this.linkRight = 0;
        this.linked = false;
        syncToClients();
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            TeleportPadBlock.setConnected(level, worldPosition, false);
        }
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            TeleportPadBlock.setConnected(level, worldPosition, linked);
        }
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(TeleportPadBlock.FACING)
                ? state.getValue(TeleportPadBlock.FACING) : Direction.NORTH;
    }

    /** Partner block position in world space, or {@code null} when not linked. */
    @Nullable
    public BlockPos partnerPos() {
        if (!linked) {
            return null;
        }
        return worldPosition.offset(TeleportPadManager.worldOffset(facing(), linkForward, linkUp, linkRight));
    }

    public OpenTeleportPadConfigPayload createOpenPayload() {
        BlockPos off = TeleportPadManager.worldOffset(facing(), linkForward, linkUp, linkRight);
        TeleportPadMode mode = getBlockState().hasProperty(TeleportPadBlock.MODE)
                ? getBlockState().getValue(TeleportPadBlock.MODE) : TeleportPadMode.TOP;
        boolean allowColor = getBlockState().is(ModBlocks.SURVIVAL_TELEPORT_PAD);
        return new OpenTeleportPadConfigPayload(worldPosition, mode.getSerializedName(),
                off.getX(), off.getY(), off.getZ(), linked, tintColor, allowColor);
    }

    public void syncToClients() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(KEY_FORWARD, linkForward);
        tag.putInt(KEY_UP, linkUp);
        tag.putInt(KEY_RIGHT, linkRight);
        tag.putBoolean(KEY_LINKED, linked);
        tag.putInt(KEY_TINT_COLOR, tintColor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.linkForward = tag.getInt(KEY_FORWARD);
        this.linkUp = tag.getInt(KEY_UP);
        this.linkRight = tag.getInt(KEY_RIGHT);
        this.linked = tag.getBoolean(KEY_LINKED);
        this.tintColor = tag.contains(KEY_TINT_COLOR) ? tag.getInt(KEY_TINT_COLOR) : DEFAULT_TINT_COLOR;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
