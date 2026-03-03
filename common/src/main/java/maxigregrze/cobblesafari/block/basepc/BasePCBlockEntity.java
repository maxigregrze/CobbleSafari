package maxigregrze.cobblesafari.block.basepc;

import maxigregrze.cobblesafari.block.BlockPart;
import maxigregrze.cobblesafari.config.SecretBasePCConfig;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BasePCBlockEntity extends BlockEntity implements MenuProvider {

    private int rank = 0;
    private int currentEffect = 0;
    private int battery = 0;
    private boolean isActive = false;
    private int tickCounter = 0;

    private static final int[] EFFECT_DURATION_TICKS = {220, 260, 300, 340, 380, 380};
    private static final int[] EFFECT_RADIUS = {8, 16, 24, 32, 40, 40};

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> rank;
                case 1 -> currentEffect;
                case 2 -> battery;
                case 3 -> isActive ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> { rank = value; updateBlockState(); }
                case 1 -> currentEffect = value;
                case 2 -> battery = value;
                case 3 -> isActive = value != 0;
            }
            setChanged();
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public BasePCBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SECRETBASE_PC, pos, state);
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public int getRank() { return rank; }
    public int getBattery() { return battery; }
    public boolean isActive() { return isActive; }
    public int getCurrentEffect() { return currentEffect; }

    public void setRank(int rank) {
        this.rank = Math.max(0, Math.min(rank, 5));
        setChanged();
        updateBlockState();
    }

    public void setBattery(int battery) {
        this.battery = Math.max(0, battery);
        setChanged();
    }

    public static int getMaxBattery(int rank) {
        return SecretBasePCConfig.getMaxBattery(rank);
    }

    public static int getEffectCost(int rank, int effect) {
        return SecretBasePCConfig.getEffectCost(rank, effect);
    }

    public static boolean isEffectLocked(int rank, int effect) {
        return SecretBasePCConfig.isEffectLocked(rank, effect);
    }

    public static Holder<MobEffect> getEffectForIndex(int index) {
        return switch (index) {
            case 0 -> ModEffects.REPEL.holder;
            case 1 -> ModEffects.UNCOMMON_BOOST.holder;
            case 2 -> ModEffects.RARE_BOOST.holder;
            case 3 -> ModEffects.ULTRA_RARE_BOOST.holder;
            case 4 -> ModEffects.SHINY_BOOST.holder;
            default -> null;
        };
    }

    public static int getEffectDuration(int rank) {
        int idx = Math.max(0, Math.min(rank, EFFECT_DURATION_TICKS.length - 1));
        return EFFECT_DURATION_TICKS[idx];
    }

    public static int getEffectRadius(int rank) {
        int idx = Math.max(0, Math.min(rank, EFFECT_RADIUS.length - 1));
        return EFFECT_RADIUS[idx];
    }

    private void updateBlockState() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            int stateRank = state.getValue(BasePCBlock.RANK);
            int r = Math.min(rank, 5);
            if (stateRank != r) {
                Direction facing = state.getValue(BasePCBlock.FACING);
                Direction sideDir = BasePCBlock.getSideDirection(facing);
                level.setBlock(worldPosition, state.setValue(BasePCBlock.RANK, r), 3);
                level.setBlock(worldPosition.relative(sideDir), state.setValue(BasePCBlock.PART, BlockPart.SIDE).setValue(BasePCBlock.RANK, r), 3);
                level.setBlock(worldPosition.above(), state.setValue(BasePCBlock.PART, BlockPart.TOP).setValue(BasePCBlock.RANK, r), 3);
            }
        }
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        tickCounter++;
        if (tickCounter < 80) return;
        tickCounter = 0;

        if (!isActive) return;

        int cost = getEffectCost(rank, currentEffect);
        if (cost == -1) {
            isActive = false;
            setChanged();
            return;
        }

        battery -= cost;
        if (battery <= 0) {
            battery = 0;
            isActive = false;
            setChanged();
            return;
        }

        setChanged();
        applyEffect(level, pos);
    }

    public void applyEffectOnce() {
        if (level != null && !level.isClientSide()) {
            applyEffect(level, worldPosition);
        }
    }

    private void applyEffect(Level level, BlockPos pos) {
        int radius = getEffectRadius(rank);
        int duration = getEffectDuration(rank);
        Holder<MobEffect> effect = getEffectForIndex(currentEffect);
        if (effect == null) return;

        AABB area = new AABB(pos).inflate(radius);
        List<ServerPlayer> players = ((ServerLevel) level).getPlayers(
                p -> area.contains(p.getX(), p.getY(), p.getZ()));

        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(effect, duration, 0, true, true));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Rank", rank);
        tag.putInt("Battery", battery);
        tag.putInt("CurrentEffect", currentEffect);
        tag.putBoolean("IsActive", isActive);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rank = tag.getInt("Rank");
        battery = tag.getInt("Battery");
        currentEffect = tag.getInt("CurrentEffect");
        isActive = tag.getBoolean("IsActive");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.cobblesafari.basepc.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new BasePCMenu(syncId, inv, containerData, ContainerLevelAccess.create(level, worldPosition));
    }
}
