package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.item.ultraloot.StakatakaShieldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client-only frame animation of the Stacked Shield (action plan 192, section 8.5).
 * <p>The animation belongs to a <i>holder's hand</i>, not to a stack: every server update of the
 * shield (wear, switching to rest) hands the client a new {@code ItemStack} instance, and the frame
 * must carry over - only the active / resting texture changes. Item renderers never see the holder,
 * so each tick also binds the stack instance currently in that hand to its state, which the renderer
 * looks up ({@code ItemStack} has identity equality). Once the shield leaves the hand, its state
 * stays bound to the last instance (now in the inventory) until the closing animation is over.</p>
 */
public final class StakatakaShieldAnimator {

    /** Short frames: 0.15 s (3 ticks), down from the spec's 0.5 s after the first in-game test. */
    private static final int SHORT_FRAME = 3;
    private static final int WAKE_DELAY_TICKS = 200;
    private static final int FIDGET_ROLL_INTERVAL = 20;
    private static final int FIDGET_ODDS = 15;

    private enum Anim {
        REST(new int[] {0}, new int[] {0}),
        WAKE(new int[] {1, 2}, new int[] {SHORT_FRAME, SHORT_FRAME}),
        AWAKE(new int[] {3}, new int[] {0}),
        FIDGET_A(new int[] {4, 6, 5}, new int[] {SHORT_FRAME, 80, SHORT_FRAME}),
        FIDGET_B(new int[] {7, 8, 9, 8, 7}, new int[] {SHORT_FRAME, SHORT_FRAME, 60, SHORT_FRAME, SHORT_FRAME}),
        SLEEP(new int[] {2, 1}, new int[] {SHORT_FRAME, SHORT_FRAME});

        final int[] frames;
        final int[] durations;

        Anim(int[] frames, int[] durations) {
            this.frames = frames;
            this.durations = durations;
        }

        boolean looping() {
            return this == REST || this == AWAKE;
        }
    }

    private static final class State {
        Anim anim = Anim.REST;
        int step;
        int stepTicks;
        int counter;
        boolean held;
        ItemStack boundStack;

        void start(Anim next) {
            anim = next;
            step = 0;
            stepTicks = 0;
            counter = 0;
        }

        int frame() {
            return anim.frames[step];
        }
    }

    /** Key: entity id × hand. */
    private static final Map<Long, State> HOLDER_STATES = new HashMap<>();
    private static final Map<ItemStack, State> STACK_STATES = new WeakHashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private StakatakaShieldAnimator() {}

    public static int frame(ItemStack stack) {
        State state = STACK_STATES.get(stack);
        return state == null ? 0 : state.frame();
    }

    public static void onClientTick(Minecraft minecraft) {
        if (minecraft.level == null) {
            HOLDER_STATES.clear();
            STACK_STATES.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        for (State state : HOLDER_STATES.values()) {
            state.held = false;
        }
        for (Player player : minecraft.level.players()) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof StakatakaShieldItem) {
                    State state = HOLDER_STATES.computeIfAbsent(key(player, hand), k -> new State());
                    state.held = true;
                    bind(state, stack);
                }
            }
        }
        Iterator<State> it = HOLDER_STATES.values().iterator();
        while (it.hasNext()) {
            State state = it.next();
            advance(state);
            if (!state.held && state.anim == Anim.REST) {
                STACK_STATES.remove(state.boundStack);
                it.remove();
            }
        }
    }

    private static long key(Player player, InteractionHand hand) {
        return ((long) player.getId() << 1) | hand.ordinal();
    }

    /** Points the stack instance now in the hand at the hand's state (new instance: same frame). */
    private static void bind(State state, ItemStack stack) {
        if (state.boundStack != stack) {
            if (state.boundStack != null) {
                STACK_STATES.remove(state.boundStack);
            }
            state.boundStack = stack;
            STACK_STATES.put(stack, state);
        }
    }

    private static void advance(State state) {
        switch (state.anim) {
            case REST -> {
                state.counter = state.held ? state.counter + 1 : 0;
                if (state.counter >= WAKE_DELAY_TICKS) {
                    state.start(Anim.WAKE);
                }
            }
            case AWAKE -> {
                if (!state.held) {
                    state.start(Anim.SLEEP);
                } else if (++state.counter % FIDGET_ROLL_INTERVAL == 0 && RANDOM.nextInt(FIDGET_ODDS) == 0) {
                    state.start(RANDOM.nextBoolean() ? Anim.FIDGET_A : Anim.FIDGET_B);
                }
            }
            default -> stepThrough(state);
        }
    }

    /** Plays a one-shot animation; it always runs to its end, held or not (spec). */
    private static void stepThrough(State state) {
        if (state.anim.looping()) {
            return;
        }
        if (++state.stepTicks < state.anim.durations[state.step]) {
            return;
        }
        state.stepTicks = 0;
        if (++state.step < state.anim.frames.length) {
            return;
        }
        // WAKE and both fidgets land on 3; the next tick sends a shield no longer held to SLEEP.
        state.start(state.anim == Anim.SLEEP ? Anim.REST : Anim.AWAKE);
    }
}
