package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.network.OpenAuspiciousPokeballGoldConfigPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AuspiciousPokeballGoldBlockEntity extends AuspiciousPokeballBlockEntity {

    private static final String NBT_EARNABLE = "Earnable";
    private static final String NBT_EARNERS = "Earners";

    public static final int MAX_EARNERS = 64;
    public static final int MAX_EARNER_NAME_LENGTH = 32;

    private boolean earnable;
    private final LinkedHashSet<String> earners = new LinkedHashSet<>();

    public AuspiciousPokeballGoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUSPICIOUS_POKEBALL_GOLD, pos, state);
    }

    @Override
    protected boolean usesGoldMiscConfigDefaults() {
        return true;
    }

    @Override
    public Block displayRenderBlock() {
        return ModBlocks.AUSPICIOUS_POKEBALL_GOLD_DISPLAY;
    }

    public boolean isEarnable() {
        return this.earnable;
    }

    public void setEarnable(boolean earnable) {
        this.earnable = earnable;
    }

    public List<String> getEarnersCopy() {
        return List.copyOf(this.earners);
    }

    /**
     * Reserved for a future API / GUI; bounds size and stores casing as provided.
     */
    public void setEarnersFromServer(List<String> names) {
        this.earners.clear();
        if (names == null) {
            return;
        }
        for (String raw : names) {
            if (this.earners.size() >= MAX_EARNERS) {
                break;
            }
            if (raw == null) {
                continue;
            }
            String t = raw.trim();
            if (!t.isEmpty() && t.length() <= MAX_EARNER_NAME_LENGTH) {
                this.earners.add(t);
            }
        }
    }

    public boolean isEarner(Player player) {
        String name = player.getGameProfile().getName();
        for (String e : this.earners) {
            if (e.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEarnRestrictedFor(Player player) {
        return this.earnable && (this.earners.isEmpty() || !this.isEarner(player));
    }

    @Override
    protected boolean shouldHideShapeForEarnRestrictions(Player player) {
        return this.isEarnRestrictedFor(player);
    }

    @Override
    protected boolean shouldHideWorldModelForEarnRestrictions(Player player) {
        return this.isEarnRestrictedFor(player);
    }

    @Override
    public boolean canPlayerAttemptClaim(Player player) {
        if (!this.earnable) {
            return true;
        }
        if (this.earners.isEmpty()) {
            return false;
        }
        return this.isEarner(player);
    }

    public OpenAuspiciousPokeballGoldConfigPayload createGoldOpenPayload() {
        return new OpenAuspiciousPokeballGoldConfigPayload(
                this.getBlockPos(),
                this.getPoolBerryId(),
                this.getPoolCandyId(),
                this.getPoolBallsId(),
                this.getPoolTreasuresId(),
                this.getMinRoll(),
                this.getMaxRoll(),
                this.earnable,
                this.getEarnersCopy()
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(NBT_EARNABLE, this.earnable);
        ListTag list = new ListTag();
        for (String name : this.earners) {
            list.add(StringTag.valueOf(name));
        }
        tag.put(NBT_EARNERS, list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.earnable = tag.getBoolean(NBT_EARNABLE);
        this.earners.clear();
        ListTag list = tag.getList(NBT_EARNERS, Tag.TAG_STRING);
        for (int i = 0; i < list.size() && this.earners.size() < MAX_EARNERS; i++) {
            String s = list.getString(i).trim();
            if (!s.isEmpty() && s.length() <= MAX_EARNER_NAME_LENGTH) {
                this.earners.add(s);
            }
        }
    }

    public Set<String> getEarnersUnmodifiable() {
        return Collections.unmodifiableSet(this.earners);
    }

    /**
     * Appends a single earner name, switches the block to earnable mode and resyncs it
     * (used by the dimensional-objectives auspicious redeem.4). No-op if the name is
     * blank, too long, or the list is full.
     */
    public void addEarner(String name) {
        if (name == null) {
            return;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_EARNER_NAME_LENGTH || this.earners.size() >= MAX_EARNERS) {
            return;
        }
        this.earners.add(trimmed);
        this.earnable = true;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
}
