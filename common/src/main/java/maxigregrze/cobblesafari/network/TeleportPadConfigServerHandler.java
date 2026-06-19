package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.block.teleporter.TeleportPadBlock;
import maxigregrze.cobblesafari.block.teleporter.TeleportPadBlockEntity;
import maxigregrze.cobblesafari.block.teleporter.TeleportPadMode;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.teleporter.TeleportPadManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server validation + handling for the teleport-pad GUI. Requires creative (creative pad)
 * or a Tinkhammer in hand (survival pad), plus proximity + a real {@link TeleportPadBlockEntity}.
 */
public final class TeleportPadConfigServerHandler {

    private static final double MAX_DISTANCE = 8.0D;

    private TeleportPadConfigServerHandler() {}

    public static void handleAction(ServerPlayer player, TeleportPadActionPayload payload) {
        if (!validate(player, payload.pos())) {
            return;
        }
        Level level = player.level();
        BlockState state = level.getBlockState(payload.pos());
        if (!(state.getBlock() instanceof TeleportPadBlock)) {
            return;
        }
        Direction facing = state.getValue(TeleportPadBlock.FACING);
        TeleportPadMode mode = TeleportPadMode.byName(payload.mode());

        if (payload.action() == TeleportPadActionPayload.Action.AUTODETECT) {
            BlockPos delta = TeleportPadManager.autoDetect(level, payload.pos(), mode, facing);
            if (delta == null) {
                send(player, TeleportPadResultPayload.Status.NONE, 0, 0, 0);
            } else {
                send(player, TeleportPadResultPayload.Status.FOUND, delta.getX(), delta.getY(), delta.getZ());
            }
            return;
        }

        BlockPos worldDelta = new BlockPos(payload.x(), payload.y(), payload.z());
        TeleportPadResultPayload.Status status =
                TeleportPadManager.validateManual(level, payload.pos(), mode, facing, worldDelta);
        if (status == TeleportPadResultPayload.Status.VALID) {
            send(player, status, payload.x(), payload.y(), payload.z());
        } else {
            send(player, status, 0, 0, 0);
        }
    }

    public static void handleSave(ServerPlayer player, SaveTeleportPadConfigPayload payload) {
        if (!validate(player, payload.pos())) {
            return;
        }
        Level level = player.level();
        BlockState state = level.getBlockState(payload.pos());
        if (!(state.getBlock() instanceof TeleportPadBlock)) {
            return;
        }
        if (!(level.getBlockEntity(payload.pos()) instanceof TeleportPadBlockEntity be)) {
            return;
        }

        int parsedColor = parseColor(payload.color());
        if (state.is(ModBlocks.SURVIVAL_TELEPORT_PAD) && parsedColor >= 0) {
            be.setTintColor(parsedColor);
        }

        TeleportPadMode mode = TeleportPadMode.byName(payload.mode());
        if (state.getValue(TeleportPadBlock.MODE) != mode) {
            state = state.setValue(TeleportPadBlock.MODE, mode);
            level.setBlock(payload.pos(), state, Block.UPDATE_ALL);
        }
        Direction facing = state.getValue(TeleportPadBlock.FACING);

        if (payload.x() == 0 && payload.y() == 0 && payload.z() == 0) {
            TeleportPadManager.breakLink(level, payload.pos());
            return;
        }

        BlockPos worldDelta = new BlockPos(payload.x(), payload.y(), payload.z());
        TeleportPadResultPayload.Status status =
                TeleportPadManager.validateManual(level, payload.pos(), mode, facing, worldDelta);
        if (status != TeleportPadResultPayload.Status.VALID) {
            player.sendSystemMessage(Component.translatable(invalidSaveKey(status)));
            return;
        }
        BlockPos target = payload.pos().offset(worldDelta);
        if (!TeleportPadManager.isTargetFreeForMe(level, payload.pos(), target)) {
            player.sendSystemMessage(Component.translatable("gui.cobblesafari.teleport_pad.save.already_paired"));
            return;
        }
        TeleportPadManager.breakLink(level, payload.pos());
        TeleportPadManager.linkBoth(level, payload.pos(), target);
        player.sendSystemMessage(Component.translatable("gui.cobblesafari.teleport_pad.save.pair_found",
                payload.x(), payload.y(), payload.z()));
    }

    private static int parseColor(int color) {
        return color & 0xFFFFFF;
    }

    private static String invalidSaveKey(TeleportPadResultPayload.Status status) {
        return switch (status) {
            case OBSTRUCTED -> "gui.cobblesafari.teleport_pad.save.obstructed";
            case WRONG_MODE -> "gui.cobblesafari.teleport_pad.check.wrong_mode";
            case OUT_OF_RANGE -> "gui.cobblesafari.teleport_pad.check.out_of_range";
            default -> "gui.cobblesafari.teleport_pad.check.not_found";
        };
    }

    private static boolean validate(ServerPlayer player, BlockPos pos) {
        if (!player.blockPosition().closerThan(pos, MAX_DISTANCE)) {
            return false;
        }
        Level level = player.level();
        if (!(level.getBlockEntity(pos) instanceof TeleportPadBlockEntity)) {
            return false;
        }
        Block block = level.getBlockState(pos).getBlock();
        if (block == ModBlocks.TELEPORT_PAD) {
            return player.isCreative();
        }
        if (block == ModBlocks.SURVIVAL_TELEPORT_PAD) {
            return player.getMainHandItem().is(ModItems.TINKHAMMER)
                    || player.getOffhandItem().is(ModItems.TINKHAMMER);
        }
        return false;
    }

    private static void send(ServerPlayer player, TeleportPadResultPayload.Status status, int x, int y, int z) {
        Services.PLATFORM.sendPayloadToPlayer(player, new TeleportPadResultPayload(status, x, y, z));
    }
}
