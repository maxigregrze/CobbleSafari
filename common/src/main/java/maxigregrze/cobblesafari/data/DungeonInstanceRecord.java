package maxigregrze.cobblesafari.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistent dungeon structure instance tracked for async region clearing.
 * When {@link #pendingDeletion()} is true, the structure is queued for (or undergoing) removal.
 */
public record DungeonInstanceRecord(
        UUID instanceId,
        UUID portalId,
        String dimensionId,
        int slot,
        BlockPos structurePos,
        int chunkMinX,
        int chunkMinZ,
        int chunkMaxX,
        int chunkMaxZ,
        int structureY,
        int clearSectionsBelow,
        int clearSectionsAbove,
        int zoneSize,
        boolean pendingDeletion,
        /**
         * The entrance portal is gone, but this region is not: it lives on while it is occupied
         * (action plan 182, A1). Absent from a pre-182 record, which therefore reads {@code false}
         * - "entrance still open" - and behaves exactly as before until its portal disappears.
         */
        boolean entranceClosed,
        /**
         * The parent instance this one is nested inside, or {@code null} for an ordinary dungeon
         * (action plan 183, B3). Every field below is additive and optional on read: a pre-183
         * record reads {@code null} / {@code 0} / empty and behaves exactly as before.
         */
        UUID parentInstanceId,
        /** The single player this sub-instance belongs to, or {@code null}. */
        UUID ownerPlayerId,
        /** Credit granted on entry - the minuend of the debit (action plan 183, section 6.2). */
        int grantedTicks,
        /** The ride's {@code type}: <b>interpreted</b>, it selects the structure. */
        int rollTypeId,
        /** The ride's {@code tier}: recorded, never interpreted (spec v2 section 4.7). */
        int rollTier,
        /** The ride's {@code distance}: recorded, never interpreted (spec v2 section 4.7). */
        int rollDistance,
        /**
         * Wormhole entries already spent on <b>this</b> parent instance, one per (player, device)
         * pair (action plan 187, C4). Per run by construction: it resets with the instance.
         *
         * <p>Plan 183 B7 keyed this on the player alone - one ride per run, whatever the number of
         * devices. Keying it on the device instead makes the number of rides follow the structure's
         * own contents: three wormhole blocks in a megalopolis grant three rides, and adding a
         * fourth is an authoring change rather than a config key. Positions are absolute, which is
         * safe because a record belongs to one instance and its structure is placed once.</p>
         */
        List<SubEntry> subEntries
) {
    /**
     * One spent entry: which player, at which device. {@code devicePos} is always the block the
     * player interacted with as {@code SubDungeonManager} resolved it - the lower half for a double
     * block - so the two halves of one wormhole can never count as two entries.
     */
    public record SubEntry(UUID playerId, BlockPos devicePos) {}

    private static final String NBT_INSTANCE_ID = "InstanceId";
    private static final String NBT_PORTAL_ID = "PortalId";
    private static final String NBT_DIMENSION_ID = "DimensionId";
    private static final String NBT_SLOT = "Slot";
    private static final String NBT_STRUCTURE_X = "StructureX";
    private static final String NBT_STRUCTURE_Y = "StructureY";
    private static final String NBT_STRUCTURE_Z = "StructureZ";
    private static final String NBT_CHUNK_MIN_X = "ChunkMinX";
    private static final String NBT_CHUNK_MIN_Z = "ChunkMinZ";
    private static final String NBT_CHUNK_MAX_X = "ChunkMaxX";
    private static final String NBT_CHUNK_MAX_Z = "ChunkMaxZ";
    private static final String NBT_CLEAR_SECTIONS_BELOW = "ClearSectionsBelow";
    private static final String NBT_CLEAR_SECTIONS_ABOVE = "ClearSectionsAbove";
    private static final String NBT_ZONE_SIZE = "ZoneSize";
    private static final String NBT_PENDING_DELETION = "PendingDeletion";
    private static final String NBT_ENTRANCE_CLOSED = "EntranceClosed";
    private static final String NBT_PARENT_INSTANCE_ID = "ParentInstanceId";
    private static final String NBT_OWNER_PLAYER_ID = "OwnerPlayerId";
    private static final String NBT_GRANTED_TICKS = "GrantedTicks";
    private static final String NBT_ROLL_TYPE = "RollType";
    private static final String NBT_ROLL_TIER = "RollTier";
    private static final String NBT_ROLL_DISTANCE = "RollDistance";
    /**
     * Per-device entries (action plan 187, C4). A <b>new</b> key: the pre-187 {@code SubEntriesUsed}
     * list of bare UUIDs is neither read nor written any more. Ultra Space content is unreleased, so
     * no save in circulation depends on it, and writing under a new key guarantees that no record
     * created from here on carries the legacy shape - a record written before this plan simply
     * reads as "no entry spent" rather than as something ambiguous.
     */
    private static final String NBT_SUB_ENTRIES_BY_PORTAL = "SubEntriesByPortal";
    private static final String NBT_SUB_ENTRY_PLAYER = "Player";
    private static final String NBT_SUB_ENTRY_POS = "Pos";

    public static DungeonInstanceRecord create(
            UUID portalId,
            String dimensionId,
            int slot,
            BlockPos structurePos,
            int chunkMinX,
            int chunkMinZ,
            int chunkMaxX,
            int chunkMaxZ,
            int structureY,
            int clearSectionsBelow,
            int clearSectionsAbove,
            int zoneSize
    ) {
        return new DungeonInstanceRecord(
                UUID.randomUUID(),
                portalId,
                dimensionId,
                slot,
                structurePos,
                chunkMinX,
                chunkMinZ,
                chunkMaxX,
                chunkMaxZ,
                structureY,
                clearSectionsBelow,
                clearSectionsAbove,
                zoneSize,
                false,
                false,
                null,
                null,
                0,
                0,
                0,
                0,
                List.of()
        );
    }

    /**
     * A sub-instance: no portal, one owner, one parent, and the ride's three values
     * (action plan 183, section 5.4).
     */
    public static DungeonInstanceRecord createSub(
            String dimensionId,
            int slot,
            BlockPos structurePos,
            int chunkMinX,
            int chunkMinZ,
            int chunkMaxX,
            int chunkMaxZ,
            /**
             * Clear-window sections. Since action plan 187 (C2) the sub-dungeon path passes values
             * <b>measured</b> from the assembled structure here, not {@code DungeonConfig}'s: the
             * config values are a bet on which way a structure grows, and Ultra Space's pools grow
             * downward from an anchor at their top.
             */
            int clearSectionsBelow,
            int clearSectionsAbove,
            int zoneSize,
            UUID parentInstanceId,
            UUID ownerPlayerId,
            int grantedTicks,
            int rollTypeId,
            int rollTier,
            int rollDistance
    ) {
        return new DungeonInstanceRecord(
                UUID.randomUUID(),
                null,
                dimensionId,
                slot,
                structurePos,
                chunkMinX,
                chunkMinZ,
                chunkMaxX,
                chunkMaxZ,
                structurePos.getY(),
                clearSectionsBelow,
                clearSectionsAbove,
                zoneSize,
                false,
                false,
                parentInstanceId,
                ownerPlayerId,
                grantedTicks,
                rollTypeId,
                rollTier,
                rollDistance,
                List.of()
        );
    }

    public DungeonInstanceRecord withPendingDeletion(boolean pendingDeletion) {
        return new DungeonInstanceRecord(
                instanceId, portalId, dimensionId, slot, structurePos,
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                structureY, clearSectionsBelow, clearSectionsAbove, zoneSize,
                pendingDeletion, entranceClosed,
                parentInstanceId, ownerPlayerId, grantedTicks,
                rollTypeId, rollTier, rollDistance, subEntries
        );
    }

    /** Detach: the portal is gone, the region is not (action plan 182, A1). */
    public DungeonInstanceRecord withEntranceClosed(boolean entranceClosed) {
        return new DungeonInstanceRecord(
                instanceId, portalId, dimensionId, slot, structurePos,
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                structureY, clearSectionsBelow, clearSectionsAbove, zoneSize,
                pendingDeletion, entranceClosed,
                parentInstanceId, ownerPlayerId, grantedTicks,
                rollTypeId, rollTier, rollDistance, subEntries
        );
    }

    /**
     * Spends {@code playerId}'s wormhole entry <b>on {@code devicePos}</b> (action plan 187, C4).
     * Idempotent: the same pair is never added twice, so a held right-click cannot bank entries.
     */
    public DungeonInstanceRecord withSubEntryConsumed(UUID playerId, BlockPos devicePos) {
        if (playerId == null || devicePos == null) {
            return this;
        }
        SubEntry entry = new SubEntry(playerId, devicePos.immutable());
        if (subEntries.contains(entry)) {
            return this;
        }
        List<SubEntry> spent = new ArrayList<>(subEntries);
        spent.add(entry);
        return withSubEntries(List.copyOf(spent));
    }

    /**
     * Gives back the entry spent at {@code devicePos} - the two server-initiated failures of
     * section 8.4. Only that device is refunded: the player's other devices are untouched.
     */
    public DungeonInstanceRecord withSubEntryRefunded(UUID playerId, BlockPos devicePos) {
        if (playerId == null || devicePos == null) {
            return this;
        }
        SubEntry entry = new SubEntry(playerId, devicePos.immutable());
        if (!subEntries.contains(entry)) {
            return this;
        }
        List<SubEntry> spent = new ArrayList<>(subEntries);
        spent.remove(entry);
        return withSubEntries(List.copyOf(spent));
    }

    /**
     * Drops <b>every</b> entry {@code playerId} has spent on this instance, whatever the device.
     *
     * <p>Distinct from {@link #withSubEntryRefunded} on purpose: this is not a refund of one failed
     * ride but the reset of a whole run, used when a player pays to re-enter the instance
     * ({@code DungeonTpAcceptHandler.resetWormholeEntryForNewRun}). A new run buys every wormhole
     * back, not just the last one used.</p>
     */
    public DungeonInstanceRecord withSubEntriesClearedFor(UUID playerId) {
        if (playerId == null) {
            return this;
        }
        List<SubEntry> kept = new ArrayList<>();
        for (SubEntry entry : subEntries) {
            if (!playerId.equals(entry.playerId())) {
                kept.add(entry);
            }
        }
        return kept.size() == subEntries.size() ? this : withSubEntries(List.copyOf(kept));
    }

    private DungeonInstanceRecord withSubEntries(List<SubEntry> entries) {
        return new DungeonInstanceRecord(
                instanceId, portalId, dimensionId, slot, structurePos,
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                structureY, clearSectionsBelow, clearSectionsAbove, zoneSize,
                pendingDeletion, entranceClosed,
                parentInstanceId, ownerPlayerId, grantedTicks,
                rollTypeId, rollTier, rollDistance, entries
        );
    }

    public boolean hasSpentSubEntry(UUID playerId, BlockPos devicePos) {
        return playerId != null && devicePos != null
                && subEntries.contains(new SubEntry(playerId, devicePos.immutable()));
    }

    /** @return true when this record describes a nested visit rather than an ordinary dungeon. */
    public boolean isSubInstance() {
        return parentInstanceId != null || ownerPlayerId != null;
    }

    /**
     * Whether the persisted bounds are real: a live-pipeline record carries a ±1-chunk margin, so a
     * genuine box spans three chunks or more, and an all-zero box means "bounds unknown".
     */
    public boolean hasUsableBounds() {
        return chunkMaxX - chunkMinX >= 2 && chunkMaxZ - chunkMinZ >= 2;
    }

    /**
     * Whether {@code pos} falls inside this instance's footprint, ignoring Y: a player above the
     * cleanup window still belongs here. Y only breaks ties between overlapping instances.
     */
    public boolean containsColumn(BlockPos pos) {
        int minX = chunkMinX << 4;
        int minZ = chunkMinZ << 4;
        int maxX = (chunkMaxX << 4) + 15;
        int maxZ = (chunkMaxZ << 4) + 15;
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(NBT_INSTANCE_ID, instanceId);
        if (portalId != null) {
            tag.putUUID(NBT_PORTAL_ID, portalId);
        }
        tag.putString(NBT_DIMENSION_ID, dimensionId);
        tag.putInt(NBT_SLOT, slot);
        tag.putInt(NBT_STRUCTURE_X, structurePos.getX());
        tag.putInt(NBT_STRUCTURE_Y, structurePos.getY());
        tag.putInt(NBT_STRUCTURE_Z, structurePos.getZ());
        tag.putInt(NBT_CHUNK_MIN_X, chunkMinX);
        tag.putInt(NBT_CHUNK_MIN_Z, chunkMinZ);
        tag.putInt(NBT_CHUNK_MAX_X, chunkMaxX);
        tag.putInt(NBT_CHUNK_MAX_Z, chunkMaxZ);
        tag.putInt(NBT_CLEAR_SECTIONS_BELOW, clearSectionsBelow);
        tag.putInt(NBT_CLEAR_SECTIONS_ABOVE, clearSectionsAbove);
        tag.putInt(NBT_ZONE_SIZE, zoneSize);
        tag.putBoolean(NBT_PENDING_DELETION, pendingDeletion);
        tag.putBoolean(NBT_ENTRANCE_CLOSED, entranceClosed);
        // Sub-instance fields: written only when set, so an ordinary record's NBT is unchanged.
        if (parentInstanceId != null) {
            tag.putUUID(NBT_PARENT_INSTANCE_ID, parentInstanceId);
        }
        if (ownerPlayerId != null) {
            tag.putUUID(NBT_OWNER_PLAYER_ID, ownerPlayerId);
        }
        if (grantedTicks != 0) {
            tag.putInt(NBT_GRANTED_TICKS, grantedTicks);
        }
        if (rollTypeId != 0) {
            tag.putInt(NBT_ROLL_TYPE, rollTypeId);
        }
        if (rollTier != 0) {
            tag.putInt(NBT_ROLL_TIER, rollTier);
        }
        if (rollDistance != 0) {
            tag.putInt(NBT_ROLL_DISTANCE, rollDistance);
        }
        if (!subEntries.isEmpty()) {
            ListTag spent = new ListTag();
            for (SubEntry entry : subEntries) {
                CompoundTag one = new CompoundTag();
                one.putUUID(NBT_SUB_ENTRY_PLAYER, entry.playerId());
                one.putLong(NBT_SUB_ENTRY_POS, entry.devicePos().asLong());
                spent.add(one);
            }
            tag.put(NBT_SUB_ENTRIES_BY_PORTAL, spent);
        }
        return tag;
    }

    public static DungeonInstanceRecord load(CompoundTag tag) {
        UUID instanceId = tag.contains(NBT_INSTANCE_ID) ? tag.getUUID(NBT_INSTANCE_ID) : UUID.randomUUID();
        UUID portalId = tag.contains(NBT_PORTAL_ID) ? tag.getUUID(NBT_PORTAL_ID) : null;
        BlockPos structurePos = new BlockPos(
                tag.getInt(NBT_STRUCTURE_X),
                tag.getInt(NBT_STRUCTURE_Y),
                tag.getInt(NBT_STRUCTURE_Z)
        );
        // The pre-187 "SubEntriesUsed" key is deliberately not read: see NBT_SUB_ENTRIES_BY_PORTAL.
        List<SubEntry> spent = List.of();
        if (tag.contains(NBT_SUB_ENTRIES_BY_PORTAL)) {
            List<SubEntry> parsed = new ArrayList<>();
            for (Tag entry : tag.getList(NBT_SUB_ENTRIES_BY_PORTAL, Tag.TAG_COMPOUND)) {
                CompoundTag one = (CompoundTag) entry;
                if (one.hasUUID(NBT_SUB_ENTRY_PLAYER) && one.contains(NBT_SUB_ENTRY_POS)) {
                    parsed.add(new SubEntry(
                            one.getUUID(NBT_SUB_ENTRY_PLAYER),
                            BlockPos.of(one.getLong(NBT_SUB_ENTRY_POS))));
                }
            }
            spent = List.copyOf(parsed);
        }
        return new DungeonInstanceRecord(
                instanceId,
                portalId,
                tag.getString(NBT_DIMENSION_ID),
                tag.getInt(NBT_SLOT),
                structurePos,
                tag.getInt(NBT_CHUNK_MIN_X),
                tag.getInt(NBT_CHUNK_MIN_Z),
                tag.getInt(NBT_CHUNK_MAX_X),
                tag.getInt(NBT_CHUNK_MAX_Z),
                structurePos.getY(),
                tag.getInt(NBT_CLEAR_SECTIONS_BELOW),
                tag.getInt(NBT_CLEAR_SECTIONS_ABOVE),
                tag.getInt(NBT_ZONE_SIZE),
                tag.getBoolean(NBT_PENDING_DELETION),
                // Absent on a pre-182 record: false, i.e. the entrance is still open.
                tag.getBoolean(NBT_ENTRANCE_CLOSED),
                // Absent on a pre-183 record: not a sub-instance, no rolls, no entries spent.
                tag.contains(NBT_PARENT_INSTANCE_ID) ? tag.getUUID(NBT_PARENT_INSTANCE_ID) : null,
                tag.contains(NBT_OWNER_PLAYER_ID) ? tag.getUUID(NBT_OWNER_PLAYER_ID) : null,
                tag.getInt(NBT_GRANTED_TICKS),
                tag.getInt(NBT_ROLL_TYPE),
                tag.getInt(NBT_ROLL_TIER),
                tag.getInt(NBT_ROLL_DISTANCE),
                spent
        );
    }
}
