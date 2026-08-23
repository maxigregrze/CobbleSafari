package maxigregrze.cobblesafari.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Bone meal that never fails and works on a 3x3 of columns.
 *
 * <p>Vanilla gates {@code performBonemeal} behind {@code isBonemealSuccess} - 45% on a sapling,
 * 25% on a crop, a per-berry rate on the Cobblemon side - and consumes the item either way. This
 * skips that roll entirely and loops until the target stops changing, so one use always produces
 * the final result.</p>
 */
public class PhoenixAshesItem extends Item {

    /** Radius of the affected square, in blocks: 1 gives a 3x3 of columns. */
    private static final int RADIUS = 1;

    /** Hard cap on the growth loop, so a block whose performBonemeal never settles cannot stall the tick. */
    private static final int MAX_PASSES = 16;

    /** Vanilla's bone meal particle event, with the same particle count. */
    private static final int GROWTH_PARTICLES_EVENT = 1505;
    private static final int GROWTH_PARTICLES_COUNT = 15;

    public PhoenixAshesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();

        // Evaluated on both sides so the client predicts the swing correctly.
        if (hasAnyTarget(level, clicked)) {
            if (level instanceof ServerLevel serverLevel) {
                for (BlockPos cursor : BlockPos.betweenClosed(
                        clicked.offset(-RADIUS, 0, -RADIUS), clicked.offset(RADIUS, 0, RADIUS))) {
                    BlockPos target = cursor.immutable();
                    if (maxGrow(serverLevel, target)) {
                        level.levelEvent(GROWTH_PARTICLES_EVENT, target, GROWTH_PARTICLES_COUNT);
                    }
                }
                context.getItemInHand().shrink(1);
                finish(context);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // No growable column: fall back on the vanilla water-plant behaviour, clicked face only.
        BlockPos front = clicked.relative(context.getClickedFace());
        BlockState clickedState = level.getBlockState(clicked);
        if (clickedState.isFaceSturdy(level, clicked, context.getClickedFace())
                && BoneMealItem.growWaterPlant(context.getItemInHand(), level, front, context.getClickedFace())) {
            // growWaterPlant shrinks the stack itself - never shrink again here.
            if (!level.isClientSide()) {
                level.levelEvent(GROWTH_PARTICLES_EVENT, front, GROWTH_PARTICLES_COUNT);
                finish(context);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    /** Whether any column of the square can be grown at all. Safe to run client-side. */
    private static boolean hasAnyTarget(Level level, BlockPos clicked) {
        for (BlockPos cursor : BlockPos.betweenClosed(
                clicked.offset(-RADIUS, 0, -RADIUS), clicked.offset(RADIUS, 0, RADIUS))) {
            BlockState state = level.getBlockState(cursor);
            if (state.getBlock() instanceof BonemealableBlock bonemealable
                    && bonemealable.isValidBonemealTarget(level, cursor, state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Grows one column to its final state, ignoring the vanilla success roll.
     *
     * <p>GROWER blocks (crops, saplings, Cobblemon berries and apricorns) are advanced repeatedly
     * until their state stops changing, which is what "fully grown" means for them. A
     * NEIGHBOR_SPREADER (grass, moss, seagrass) never changes itself - it seeds its surroundings -
     * so it has no final state and gets a single guaranteed pass instead.</p>
     *
     * @return {@code true} when the column was a valid target, whether or not it actually moved.
     */
    private static boolean maxGrow(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock bonemealable)
                || !bonemealable.isValidBonemealTarget(level, pos, state)) {
            return false;
        }

        if (bonemealable.getType() == BonemealableBlock.Type.NEIGHBOR_SPREADER) {
            bonemealable.performBonemeal(level, level.random, pos, state);
            return true;
        }

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            bonemealable.performBonemeal(level, level.random, pos, state);
            BlockState next = level.getBlockState(pos);
            if (next == state) {
                // Nothing moved: a 2x2 sapling with no room, or an already-ripe berry.
                break;
            }
            state = next;
            if (!(state.getBlock() instanceof BonemealableBlock grown)
                    || !grown.isValidBonemealTarget(level, pos, state)) {
                // The tree replaced the sapling, or the crop reached its last age.
                break;
            }
            bonemealable = grown;
        }
        return true;
    }

    private void finish(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }
}
