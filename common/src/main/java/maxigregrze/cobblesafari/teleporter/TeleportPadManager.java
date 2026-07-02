package maxigregrze.cobblesafari.teleporter;

import maxigregrze.cobblesafari.block.teleporter.TeleportPadBlock;
import maxigregrze.cobblesafari.block.teleporter.TeleportPadBlockEntity;
import maxigregrze.cobblesafari.block.teleporter.TeleportPadMode;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.network.TeleportPadResultPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side teleport-pad logic: facing-relative offset maths, geometric pairing scan
 * (L-shape for TOP/BOTTOM, straight line with a configurable ±leeway window for FRONT — see
 * {@link MiscConfig#getTeleportpadForwardLeeway()}), link bookkeeping, and the
 * jump-triggered teleport with a per-player cooldown.
 */
public final class TeleportPadManager {

    public static final int MAX_RANGE = 100;
    private static final int COOLDOWN_TICKS = 20;
    private static final Map<UUID, Long> LAST_TELEPORT_TICK = new ConcurrentHashMap<>();

    private TeleportPadManager() {}

    // ------------------------------------------------------------------ offset maths

    /** Project a facing-relative offset {@code (forward, up, right)} into a world delta. */
    public static BlockPos worldOffset(Direction facing, int forward, int up, int right) {
        Vec3i f = facing.getNormal();
        Vec3i r = facing.getClockWise().getNormal();
        int x = f.getX() * forward + r.getX() * right;
        int z = f.getZ() * forward + r.getZ() * right;
        return new BlockPos(x, up, z);
    }

    /** Inverse of {@link #worldOffset}: world delta ⇒ {@code [forward, up, right]}. */
    public static int[] facingRelative(Direction facing, Vec3i worldDelta) {
        Vec3i f = facing.getNormal();
        Vec3i r = facing.getClockWise().getNormal();
        int forward = worldDelta.getX() * f.getX() + worldDelta.getZ() * f.getZ();
        int right = worldDelta.getX() * r.getX() + worldDelta.getZ() * r.getZ();
        return new int[]{forward, worldDelta.getY(), right};
    }

    // ------------------------------------------------------------------ predicates

    /** A cell is "clear" if it does not occlude (air, glass, leaves, plants… pass; full solids block). */
    public static boolean isClear(Level level, BlockPos pos) {
        return !level.getBlockState(pos).canOcclude();
    }

    private static boolean isCompatiblePad(Level level, BlockPos sourcePos, BlockPos targetPos,
                                           TeleportPadMode myMode, Direction myFacing) {
        Block sourceBlock = level.getBlockState(sourcePos).getBlock();
        BlockState targetState = level.getBlockState(targetPos);
        if (!(targetState.getBlock() instanceof TeleportPadBlock)) {
            return false;
        }
        return targetState.is(sourceBlock)
                && targetState.getValue(TeleportPadBlock.MODE) == myMode.opposite()
                && targetState.getValue(TeleportPadBlock.FACING) == myFacing.getOpposite();
    }

    /** Partner is linkable for me if it is unlinked, or already linked back to me. */
    private static boolean partnerFreeForMe(Level level, BlockPos myPos, BlockPos partnerPos) {
        if (!(level.getBlockEntity(partnerPos) instanceof TeleportPadBlockEntity be)) {
            return false;
        }
        if (!be.isLinked()) {
            return true;
        }
        BlockPos back = be.partnerPos();
        return back != null && back.equals(myPos);
    }

    // ------------------------------------------------------------------ scan

    /** First compatible and free partner reachable from {@code pos}, or {@code null}. */
    @Nullable
    public static BlockPos findPartnerByScan(Level level, BlockPos pos, TeleportPadMode mode, Direction facing) {
        return switch (mode) {
            case TOP -> scanVerticalUp(level, pos, facing);
            case BOTTOM -> scanVerticalDown(level, pos, facing);
            case FRONT -> scanFront(level, pos, facing);
        };
    }

    @Nullable
    private static BlockPos scanVerticalUp(Level level, BlockPos pos, Direction facing) {
        for (int d = 1; d <= MAX_RANGE; d++) {
            BlockPos shaftCell = pos.above(d);
            if (!isClear(level, shaftCell)) {
                break;
            }
            BlockPos candidate = shaftCell.relative(facing);
            if (isCompatiblePad(level, pos, candidate, TeleportPadMode.TOP, facing)
                    && partnerFreeForMe(level, pos, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos scanVerticalDown(Level level, BlockPos pos, Direction facing) {
        BlockPos base = pos.relative(facing);
        if (!isClear(level, base)) {
            return null;
        }
        for (int d = 1; d <= MAX_RANGE; d++) {
            BlockPos cell = base.below(d);
            if (isCompatiblePad(level, pos, cell, TeleportPadMode.BOTTOM, facing)
                    && partnerFreeForMe(level, pos, cell)) {
                return cell;
            }
            if (!isClear(level, cell)) {
                break;
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos scanFront(Level level, BlockPos pos, Direction facing) {
        Direction rightDir = facing.getClockWise();
        // Walk the full forward range without stopping at the first solid cell in our own row:
        // corridor clearance is validated per-candidate at the higher of the two pad levels
        // (see frontCorridorClear), so a partner raised on a structure is still found — and the
        // result is symmetric, i.e. either pad auto-pairs to the other.
        for (int d = 1; d <= MAX_RANGE; d++) {
            BlockPos found = checkFrontWindow(level, pos, d, facing, rightDir);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Perpendicular probe offsets {@code 0, +1, -1, +2, -2, …, +leeway, -leeway} (centre tried first). */
    private static int[] symmetricOffsets(int leeway) {
        int[] offsets = new int[2 * leeway + 1];
        offsets[0] = 0;
        for (int i = 1; i <= leeway; i++) {
            offsets[2 * i - 1] = i;
            offsets[2 * i] = -i;
        }
        return offsets;
    }

    @Nullable
    private static BlockPos checkFrontWindow(Level level, BlockPos pos, int forward, Direction facing, Direction rightDir) {
        // probe outward on each perpendicular axis (centre first) within the configured ±leeway window
        BlockPos center = pos.relative(facing, forward);
        int[] offsets = symmetricOffsets(MiscConfig.getTeleportpadForwardLeeway());
        for (int upIdx : offsets) {
            for (int sideIdx : offsets) {
                BlockPos candidate = center.relative(Direction.UP, upIdx).relative(rightDir, sideIdx);
                if (candidate.equals(pos)) {
                    continue;
                }
                if (isCompatiblePad(level, pos, candidate, TeleportPadMode.FRONT, facing)
                        && partnerFreeForMe(level, pos, candidate)
                        && frontCorridorClear(level, pos, facing, forward, upIdx)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * FRONT corridor clearance, evaluated at the <b>higher</b> of the two pad levels
     * ({@code lift = max(0, up)}). Because the higher level is the same physical row whichever
     * pad runs the test, the result is symmetric: it no longer depends on whether the caller is
     * the lower pad (whose own row is blocked by the structure the higher pad stands on) or the
     * higher pad (whose row is open air). The higher row also clears the partner's support block,
     * which sits one block below it.
     */
    private static boolean frontCorridorClear(Level level, BlockPos pos, Direction facing, int forward, int up) {
        int lift = Math.max(0, up);
        for (int d = 1; d < forward; d++) {
            if (!isClear(level, pos.relative(facing, d).above(lift))) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ path validation (manual offset)

    private static boolean shapeLegal(TeleportPadMode mode, int forward, int up, int right) {
        int leeway = MiscConfig.getTeleportpadForwardLeeway();
        return switch (mode) {
            case TOP -> forward == 1 && right == 0 && up >= 1 && up <= MAX_RANGE;
            case BOTTOM -> forward == 1 && right == 0 && up <= -1 && -up <= MAX_RANGE;
            case FRONT -> forward >= 1 && forward <= MAX_RANGE && Math.abs(up) <= leeway && Math.abs(right) <= leeway;
        };
    }

    private static boolean pathClear(Level level, BlockPos pos, TeleportPadMode mode, Direction facing,
                                     int forward, int up, int right) {
        switch (mode) {
            case TOP -> {
                for (int d = 1; d <= up; d++) {
                    if (!isClear(level, pos.above(d))) {
                        return false;
                    }
                }
                return true;
            }
            case BOTTOM -> {
                BlockPos base = pos.relative(facing);
                if (!isClear(level, base)) {
                    return false;
                }
                int depth = -up;
                for (int d = 1; d < depth; d++) {
                    if (!isClear(level, base.below(d))) {
                        return false;
                    }
                }
                return true;
            }
            case FRONT -> {
                return frontCorridorClear(level, pos, facing, forward, up);
            }
            default -> {
                return false;
            }
        }
    }

    /** Validate a manually-typed world-axis offset (GUI "Check" / Save). */
    public static TeleportPadResultPayload.Status validateManual(Level level, BlockPos pos, TeleportPadMode mode,
                                                                 Direction facing, BlockPos worldDelta) {
        int[] rel = facingRelative(facing, worldDelta);
        int forward = rel[0];
        int up = rel[1];
        int right = rel[2];
        if (!shapeLegal(mode, forward, up, right)) {
            return TeleportPadResultPayload.Status.OUT_OF_RANGE;
        }
        BlockPos target = pos.offset(worldDelta);
        BlockState ts = level.getBlockState(target);
        if (!(ts.getBlock() instanceof TeleportPadBlock)) {
            return TeleportPadResultPayload.Status.NOT_FOUND;
        }
        if (!ts.is(level.getBlockState(pos).getBlock())) {
            return TeleportPadResultPayload.Status.NOT_FOUND;
        }
        if (ts.getValue(TeleportPadBlock.MODE) != mode.opposite()
                || ts.getValue(TeleportPadBlock.FACING) != facing.getOpposite()) {
            return TeleportPadResultPayload.Status.WRONG_MODE;
        }
        if (!pathClear(level, pos, mode, facing, forward, up, right)) {
            return TeleportPadResultPayload.Status.OBSTRUCTED;
        }
        return TeleportPadResultPayload.Status.VALID;
    }

    /** Auto-detect: world delta to the closest free compatible pad, or {@code null}. */
    @Nullable
    public static BlockPos autoDetect(Level level, BlockPos pos, TeleportPadMode mode, Direction facing) {
        BlockPos partner = findPartnerByScan(level, pos, mode, facing);
        return partner == null ? null : partner.subtract(pos);
    }

    // ------------------------------------------------------------------ linking

    public static void linkBoth(Level level, BlockPos a, BlockPos b) {
        if (!(level.getBlockEntity(a) instanceof TeleportPadBlockEntity beA)
                || !(level.getBlockEntity(b) instanceof TeleportPadBlockEntity beB)) {
            return;
        }
        Direction fa = level.getBlockState(a).getValue(TeleportPadBlock.FACING);
        Direction fb = level.getBlockState(b).getValue(TeleportPadBlock.FACING);
        int[] relA = facingRelative(fa, b.subtract(a));
        int[] relB = facingRelative(fb, a.subtract(b));
        beA.setLink(relA[0], relA[1], relA[2]);
        beB.setLink(relB[0], relB[1], relB[2]);
    }

    /** Try to auto-pair a freshly placed pad by scanning along its facing. */
    public static boolean tryAutoPair(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TeleportPadBlock)) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof TeleportPadBlockEntity be)) {
            return false;
        }
        if (be.isLinked()) {
            return true; // structure-baked link kept as-is
        }
        BlockPos partner = findPartnerByScan(level, pos,
                state.getValue(TeleportPadBlock.MODE), state.getValue(TeleportPadBlock.FACING));
        if (partner == null) {
            return false;
        }
        linkBoth(level, pos, partner);
        return true;
    }

    /** Break this pad's link and its partner's link (if reachable and reciprocal). */
    public static void breakLink(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TeleportPadBlockEntity be) || !be.isLinked()) {
            return;
        }
        clearPartnerOf(level, pos, be);
        be.clearLink();
    }

    /** Reciprocal cleanup when a pad is removed (this BE is about to disappear). */
    public static void onPadRemoved(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TeleportPadBlockEntity be) || !be.isLinked()) {
            return;
        }
        clearPartnerOf(level, pos, be);
    }

    private static void clearPartnerOf(Level level, BlockPos pos, TeleportPadBlockEntity be) {
        BlockPos partner = be.partnerPos();
        if (partner != null && level.isLoaded(partner)
                && level.getBlockEntity(partner) instanceof TeleportPadBlockEntity pbe) {
            BlockPos back = pbe.partnerPos();
            if (back != null && back.equals(pos)) {
                pbe.clearLink();
            }
        }
    }

    public static boolean isTargetFreeForMe(Level level, BlockPos pos, BlockPos target) {
        return partnerFreeForMe(level, pos, target);
    }

    // ------------------------------------------------------------------ teleport

    public static void tryTeleport(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos padPos = findPadAt(level, player.blockPosition());
        if (padPos == null) {
            return;
        }
        long tick = level.getServer().getTickCount();
        Long last = LAST_TELEPORT_TICK.get(player.getUUID());
        if (last != null && tick - last < COOLDOWN_TICKS) {
            return;
        }
        if (!(level.getBlockEntity(padPos) instanceof TeleportPadBlockEntity be) || !be.isLinked()) {
            return;
        }
        BlockState state = level.getBlockState(padPos);
        TeleportPadMode mode = state.getValue(TeleportPadBlock.MODE);
        Direction facing = state.getValue(TeleportPadBlock.FACING);
        BlockPos target = be.partnerPos();

        boolean valid = target != null
                && level.isLoaded(target)
                && isCompatiblePad(level, padPos, target, mode, facing)
                && partnerFreeForMe(level, padPos, target)
                && pathClear(level, padPos, mode, facing, be.getLinkForward(), be.getLinkUp(), be.getLinkRight());

        if (!valid) {
            breakLink(level, padPos);
            player.sendSystemMessage(Component.translatable("cobblesafari.teleport_pad.broken"));
            level.playSound(null, padPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.0f);
            return;
        }

        LAST_TELEPORT_TICK.put(player.getUUID(), tick);
        level.getChunk(target.getX() >> 4, target.getZ() >> 4);
        double dx = target.getX() + 0.5D;
        double dy = target.getY() + 0.05D;
        double dz = target.getZ() + 0.5D;
        player.teleportTo(level, dx, dy, dz, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), Vec3.ZERO));
        level.playSound(null, padPos, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.BLOCKS, 0.5f, 1.0f);
        level.playSound(null, target, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    @Nullable
    private static BlockPos findPadAt(Level level, BlockPos feet) {
        if (level.getBlockState(feet).getBlock() instanceof TeleportPadBlock) {
            return feet;
        }
        BlockPos below = feet.below();
        if (level.getBlockState(below).getBlock() instanceof TeleportPadBlock) {
            return below;
        }
        return null;
    }
}
