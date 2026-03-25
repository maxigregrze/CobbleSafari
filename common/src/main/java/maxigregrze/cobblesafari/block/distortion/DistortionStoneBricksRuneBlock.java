package maxigregrze.cobblesafari.block.distortion;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.network.OpenRuneEditorPayload;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.safari.SafariStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class DistortionStoneBricksRuneBlock extends BaseEntityBlock {

    public static final MapCodec<DistortionStoneBricksRuneBlock> CODEC = simpleCodec(DistortionStoneBricksRuneBlock::new);
    private static final int LETTER_INTERVAL_TICKS = 2;
    private static final int TITLE_HOLD_TICKS = 50;

    public DistortionStoneBricksRuneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DistortionStoneBricksRuneBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return handleUse(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = handleUse(level, pos, player);
        if (result.consumesAction()) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static InteractionResult handleUse(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof DistortionStoneBricksRuneBlockEntity runeBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (player.isCreative() && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            Services.PLATFORM.sendPayloadToPlayer(serverPlayer, new OpenRuneEditorPayload(pos, runeBlockEntity.getRuneText()));
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            playRuneTitle(serverPlayer, runeBlockEntity.getRuneText());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static void playRuneTitle(ServerPlayer player, String rawText) {
        String titleText = firstLine(rawText);
        if (titleText.isEmpty()) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, TITLE_HOLD_TICKS, 10));
        for (int i = 1; i <= titleText.length(); i++) {
            int delay = i * LETTER_INTERVAL_TICKS;
            String partial = titleText.substring(0, i);
            SafariStateManager.scheduleTickDelay(delay, () -> {
                if (!player.isRemoved()) {
                    player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(partial).withStyle(style -> style.withFont(ResourceLocation.fromNamespaceAndPath("cobblesafari", "unown")))));
                    
                }
            });
        }
        int clearDelay = titleText.length() * LETTER_INTERVAL_TICKS + TITLE_HOLD_TICKS;
        SafariStateManager.scheduleTickDelay(clearDelay, () -> {
            if (!player.isRemoved()) {
                player.connection.send(new ClientboundClearTitlesPacket(false));
            }
        });
    }

    private static String firstLine(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        String normalized = rawText.replace("\r", "");
        int firstBreak = normalized.indexOf('\n');
        String line = firstBreak >= 0 ? normalized.substring(0, firstBreak) : normalized;
        return line.trim();
    }
}
