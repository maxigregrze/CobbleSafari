package maxigregrze.cobblesafari.client.renderer;

import com.bedrockk.molang.runtime.MoLangRuntime;
import com.cobblemon.mod.common.client.particle.ParticleStorm;
import com.cobblemon.mod.common.client.render.MatrixWrapper;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.animation.ActiveAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.animation.PoseAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.animation.PrimaryAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockActiveAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockEffectKeyframe;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockParticleKeyframe;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockPoseAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.pose.Pose;
import kotlin.Unit;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.pokestatue.AbstractPokeStatueBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Makes a playing Creative Statue emit its animation particles, the block entity standing in for
 * the Pokémon (action plan 194, B2).
 *
 * <p>Cobblemon cannot do it alone: {@code BedrockParticleKeyframe.run} returns at once without an
 * entity, and a statue's {@link FloatingState} has none. So this reads the particle keyframes of the
 * clips being played and builds each {@link ParticleStorm} itself, exactly as the keyframe would,
 * with the source's velocity / life / visibility bound to the statue instead of an entity. Only
 * particle keyframes are read: sound and instruction keyframes stay silent.</p>
 *
 * <p>Like {@code PosableState.poseParticles}, a keyframe whose storm is still alive is not spawned
 * again, so the persistent emitters of a pose (a flame, an aura) do not pile up loop after loop. A
 * storm lives while its statue is drawn, keeps the same prepared pose and keeps emitting.</p>
 */
public final class StatueParticleEmitter {

    /** A storm whose statue has not been drawn for this long dies (statue off screen, or switched off). */
    private static final long STALE_NANOS = 500_000_000L;
    /** A clock jump larger than this (reconfiguration, wrap, first frame) restarts the window silently. */
    private static final float MAX_STEP_SECONDS = 1.0f;

    private static final Map<AbstractPokeStatueBlockEntity, StatueParticleEmitter> EMITTERS = new WeakHashMap<>();

    /** Weak: the emitter is the value of a WeakHashMap keyed by this very statue. */
    private final WeakReference<AbstractPokeStatueBlockEntity> statue;
    private final Set<BedrockParticleKeyframe> active = new HashSet<>();
    private Object pose;
    private float lastSeconds = Float.NaN;
    private long lastDrawn;

    private StatueParticleEmitter(AbstractPokeStatueBlockEntity statue) {
        this.statue = new WeakReference<>(statue);
    }

    /**
     * Called by the renderer every frame a statue is drawn playing. The locators of {@code state}
     * must already hold this frame's world-space matrices.
     *
     * @param enabled    false for a held statue or one with particles off: every storm is dropped.
     * @param animation  the selected active animation, or null on the pose route.
     * @param clipStart  where that animation started on the state clock.
     */
    public static void update(AbstractPokeStatueBlockEntity statue, boolean enabled, Object poseIdentity,
                              PosableModel model, FloatingState state, ActiveAnimation animation,
                              float seconds, float clipStart) {
        if (!enabled) {
            StatueParticleEmitter existing = EMITTERS.remove(statue);
            if (existing != null) {
                existing.active.clear();
            }
            return;
        }
        if (!(statue.getLevel() instanceof ClientLevel level)) {
            return;
        }
        StatueParticleEmitter emitter = EMITTERS.computeIfAbsent(statue, StatueParticleEmitter::new);
        try {
            emitter.advance(level, poseIdentity, model, state, animation, seconds, clipStart);
        } catch (Exception e) {
            // A borrowed model with an odd effect must never take the frame down with it.
            CobbleSafari.LOGGER.debug("[PokeStatue] particle emission failed", e);
        }
    }

    private void advance(ClientLevel level, Object poseIdentity, PosableModel model, FloatingState state,
                         ActiveAnimation animation, float seconds, float clipStart) {
        lastDrawn = System.nanoTime();
        if (pose != poseIdentity) {
            pose = poseIdentity;
            active.clear();
            lastSeconds = Float.NaN;
        }
        float previous = lastSeconds;
        lastSeconds = seconds;
        // No usable window on the first frame or after a jump: timed keyframes wait for the next one.
        boolean window = !Float.isNaN(previous) && seconds > previous && seconds - previous <= MAX_STEP_SECONDS;
        for (BedrockAnimation clip : poseClips(model, state)) {
            emitClip(level, state, clip, window, previous, seconds);
        }
        BedrockAnimation selected = bedrockOf(animation);
        if (selected != null) {
            emitClip(level, state, selected, window, previous - clipStart, seconds - clipStart);
        }
    }

    private static List<BedrockAnimation> poseClips(PosableModel model, FloatingState state) {
        List<BedrockAnimation> out = new ArrayList<>();
        Pose current = state.getCurrentPose() == null ? null : model.getPoses().get(state.getCurrentPose());
        if (current != null) {
            for (PoseAnimation animation : current.getAnimations()) {
                if (animation instanceof BedrockPoseAnimation bedrock) {
                    out.add(bedrock.getAnimation());
                }
            }
        }
        return out;
    }

    private static BedrockAnimation bedrockOf(ActiveAnimation animation) {
        ActiveAnimation inner = animation instanceof PrimaryAnimation primary ? primary.getAnimation() : animation;
        return inner instanceof BedrockActiveAnimation bedrock ? bedrock.getAnimation() : null;
    }

    /**
     * Spawns the particle keyframes of {@code clip} due in {@code (from, to]}.
     *
     * <p>A clip with no length of its own - no {@code animation_length}, every keyframe at 0, like
     * Gastly's {@code ground_idle} smoke - has no instant to cross on a statue whose clock never
     * starts at zero. A live Pokémon fires such keyframes once, when its pose begins, and keeps the
     * emitter alive; the statue does the same: they are (re)spawned whenever not already running.</p>
     */
    private void emitClip(ClientLevel level, FloatingState state, BedrockAnimation clip, boolean window,
                          float from, float to) {
        double length = clip.getAnimationLength() > 0.0 ? clip.getAnimationLength() : lastKeyframe(clip);
        boolean held = length <= 0.0;
        if (!held && !window) {
            return;
        }
        boolean loops = clip.getShouldLoop();
        for (BedrockEffectKeyframe effect : clip.getEffects()) {
            if (effect instanceof BedrockParticleKeyframe keyframe
                    && (held || crossed(keyframe.getSeconds(), from, to, loops, length))) {
                spawn(level, state, keyframe);
            }
        }
    }

    private static double lastKeyframe(BedrockAnimation clip) {
        double last = 0.0;
        for (BedrockEffectKeyframe effect : clip.getEffects()) {
            last = Math.max(last, effect.getSeconds());
        }
        return last;
    }

    private static boolean crossed(double at, double from, double to, boolean loops, double length) {
        if (!loops) {
            return from < at && at <= to;
        }
        // The first repeat of `at` strictly after `from`, then whether it is still within `to`.
        double next = at + Math.floor((from - at) / length + 1.0) * length;
        return next <= to;
    }

    private void spawn(ClientLevel level, FloatingState state, BedrockParticleKeyframe keyframe) {
        if (active.contains(keyframe)) {
            return;
        }
        Map<String, MatrixWrapper> locators = state.getLocatorStates();
        MatrixWrapper root = locators.get("root");
        if (root == null) {
            return;
        }
        MatrixWrapper locator = locators.getOrDefault(keyframe.getLocator(), root);
        MatrixWrapper emitter = keyframe.getEffect().getEmitter().getSpace().initializeEmitterMatrix(root, locator);
        MoLangRuntime runtime = new MoLangRuntime();
        runtime.getEnvironment().query = state.getRuntime().getEnvironment().query;
        Object owner = pose;
        ParticleStorm storm = new ParticleStorm(keyframe.getEffect(), emitter, locator, level,
                () -> Vec3.ZERO,
                () -> isAlive(owner, keyframe),
                () -> true,
                null,
                () -> {
                    active.remove(keyframe);
                    return Unit.INSTANCE;
                },
                () -> null,
                runtime, null);
        active.add(keyframe);
        storm.getRuntime().execute(keyframe.getScripts());
        storm.spawn();
    }

    private boolean isAlive(Object owner, BedrockParticleKeyframe keyframe) {
        AbstractPokeStatueBlockEntity source = statue.get();
        return source != null && !source.isRemoved()
                && owner == pose
                && active.contains(keyframe)
                && System.nanoTime() - lastDrawn < STALE_NANOS;
    }
}
