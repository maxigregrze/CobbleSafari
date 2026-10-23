package maxigregrze.cobblesafari.client.renderer;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.animation.ActiveAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.animation.PrimaryAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.animation.PoseAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockActiveAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockBoneTimeline;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockBoneValue;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockKeyFrameBoneValue;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockPoseAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.pose.Pose;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.pokestatue.PokeStatueRenderStyle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Draws a borrowed Cobblemon species model as a statue: optionally desaturated, flat-tinted, and
 * frozen on one chosen frame of one chosen animation.
 *
 * <p>Deliberately separate from {@link CsBossModelRenderer} - three things differ and none should
 * leak into boss rendering: the texture may be swapped for a greyscale derivative, the animation
 * clock is pinned instead of running, and Cobblemon's emissive/translucent resolver layers are
 * skipped (they would render in full colour and glow).</p>
 */
public final class PokeStatueModelRenderer {

    /** Cobblemon's own "no model" fallback, used when the species string does not resolve. */
    private static final ResourceLocation SUBSTITUTE =
            ResourceLocation.fromNamespaceAndPath("cobblemon", "substitute");

    /**
     * Window the progress value sweeps for a clip with no playable length of its own. Such clips are
     * static postures, but the pose they belong to usually carries procedural idles - wing flap, tail
     * sway - driven by the state clock, so this gives the progress field something real to scrub.
     */
    private static final float STATIC_WINDOW_SECONDS = 2.0f;

    /** Upper bound on the per-renderer species cache, so arbitrary player input cannot grow it forever. */
    private static final int MAX_CACHED_SPECIES = 128;

    private PokeStatueModelRenderer() {}

    /**
     * A resolved species plus the factor that makes its model fit a one-block cube.
     *
     * @param fitScale render scale for a 1-block result. The rendered size in blocks is
     *                 {@code hitbox × scaleParam}, so fitting means {@code 1 / max(width, height)};
     *                 the form's {@code baseScale} cancels out and must not be reapplied.
     * @param modelless the species exists in Cobblemon but no model is registered for it, so
     *                  {@code getPoser} would quietly draw the substitute (Celesteela, for one).
     *                  False for the substitute itself: an empty or unknown species string is not
     *                  this case (action plan 190, B7).
     */
    public record StatueSpecies(ResourceLocation species, Set<String> aspects, float fitScale, boolean modelless) {}

    /** Resolves a species string, falling back to the substitute model. Never returns null. */
    public static StatueSpecies resolve(String species, Map<String, StatueSpecies> cache) {
        String key = species == null ? "" : species;
        StatueSpecies cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        StatueSpecies resolved = resolveUncached(key);
        if (cache.size() >= MAX_CACHED_SPECIES) {
            cache.clear();
        }
        cache.put(key, resolved);
        return resolved;
    }

    private static StatueSpecies resolveUncached(String specie) {
        StatueSpecies fallback = new StatueSpecies(SUBSTITUTE, Set.of(), 1.0f, false);
        if (specie.isBlank()) {
            return fallback;
        }
        // Delegates to the boss resolver so both paths share the client-safe PokemonProperties.parse
        // route (it resolves aspect-triggered forms that Pokemon-level providers drop client-side).
        // A throwaway map is fine: the caller's cache means this runs at most once per species.
        CsBossModelRenderer.SpeciesInfo info = CsBossModelRenderer.resolve(specie, new HashMap<>());
        if (info == null) {
            return fallback;
        }
        try {
            Species species = PokemonSpecies.INSTANCE.getByIdentifier(info.species());
            if (species == null) {
                return fallback;
            }
            EntityDimensions box = species.getForm(info.aspects()).getHitbox();
            float largest = Math.max(box.width(), box.height());
            float fit = largest > 0.001f ? 1.0f / largest : 1.0f;
            // VaryingModelRepository.getPoser falls back to the substitute exactly when the species
            // has no variation entry, so that map is the test for "no model of its own".
            boolean modelless = !VaryingModelRepository.INSTANCE.getVariations().containsKey(info.species());
            return new StatueSpecies(info.species(), info.aspects(), fit, modelless);
        } catch (Exception e) {
            CobbleSafari.LOGGER.debug("[PokeStatue] could not size specie '{}'", specie, e);
            return fallback;
        }
    }

    // -- animation listing --------------------------------------------------

    /**
     * Names of the animations selectable for a species, sorted, without the "default pose" entry.
     *
     * <p>Three sources, all guaranteed to resolve: the current pose's {@code namedAnimations},
     * the model-level {@code animations} map, and the bedrock clips referenced by any pose (see
     * {@link #poseBedrockAnimations}). The third is what makes flying species work - Ho-Oh has
     * nothing in the first two.</p>
     */
    public static List<String> listAnimations(StatueSpecies info) {
        Set<String> names = new TreeSet<>();
        try {
            FloatingState state = new FloatingState();
            state.setCurrentAspects(info.aspects());
            PosableModel model = VaryingModelRepository.INSTANCE.getPoser(info.species(), state);
            state.setCurrentModel(model);
            state.setPoseToFirstSuitable(PoseType.PORTRAIT);
            String current = state.getCurrentPose();
            if (current != null) {
                Pose pose = model.getPoses().get(current);
                if (pose != null) {
                    names.addAll(pose.getNamedAnimations().keySet());
                }
            }
            names.addAll(model.getAnimations().keySet());
            names.addAll(poseBedrockAnimations(model).keySet());
        } catch (Exception e) {
            CobbleSafari.LOGGER.debug("[PokeStatue] could not list animations for '{}'", info.species(), e);
        }
        return new ArrayList<>(names);
    }

    /**
     * Bedrock animations reachable through the model's <b>poses</b>, keyed by short name
     * ({@code animation.ho_oh.air_fly} is listed as {@code air_fly}).
     *
     * <p>Necessary because most species declare their animations nowhere else. Of 661 posers,
     * 21.6% have an empty model-level {@code animations} map and only 34.5% define any
     * {@code namedAnimations} - Ho-Oh has neither, and reaches {@code air_idle} / {@code air_fly}
     * purely as pose idles. Those arrive at runtime as {@link BedrockPoseAnimation}, which exposes
     * the underlying {@link BedrockAnimation}, so they can be re-wrapped as a real
     * {@code BedrockActiveAnimation} - no need for the (private) animation repository map.</p>
     *
     * <p>Only bedrock entries are collected. The other pose animations are procedural MoLang
     * helpers ({@code q.look}, {@code q.sine_wing_flap}, {@code q.biped_walk}) with no underlying
     * clip at all, and nothing sensible to freeze.</p>
     */
    private static Map<String, PoseClip> poseBedrockAnimations(PosableModel model) {
        Map<String, PoseClip> found = new LinkedHashMap<>();
        for (Map.Entry<String, Pose> entry : model.getPoses().entrySet()) {
            for (PoseAnimation poseAnimation : entry.getValue().getAnimations()) {
                if (poseAnimation instanceof BedrockPoseAnimation bedrock) {
                    BedrockAnimation animation = bedrock.getAnimation();
                    found.putIfAbsent(shortName(animation.getName()),
                            new PoseClip(entry.getKey(), animation));
                }
            }
        }
        return found;
    }

    /**
     * A bedrock clip together with the pose that referenced it.
     *
     * <p>The pose matters as much as the clip. Poses carry {@code transformedParts}, which include
     * part <b>visibility</b>: Ho-Oh's {@code fly} pose shows {@code wing_open_*} and hides
     * {@code wing_folded_*}, while {@code standing} does the opposite. Applying {@code air_fly}
     * while parked in the standing pose animates bones that pose has hidden - which is why the
     * wings, and Garchomp's arms, went missing. Selecting a clip therefore switches to its pose.</p>
     */
    private record PoseClip(String poseName, BedrockAnimation animation) {}

    private static String shortName(String fullName) {
        int dot = fullName.lastIndexOf('.');
        return dot >= 0 && dot < fullName.length() - 1 ? fullName.substring(dot + 1) : fullName;
    }

    // -- posing -------------------------------------------------------------

    /**
     * Rough length of the clock a playing statue runs on before wrapping: about an hour of world time.
     * Short enough for the partial-tick float to keep sub-millisecond precision, long enough that the
     * one seam it introduces is never seen.
     */
    private static final float PLAYBACK_WRAP_TARGET_SECONDS = 3600f;

    /**
     * A model state prepared once and reused every frame (action plan 190, B7).
     *
     * @param seconds   the held instant for a fixed statue
     * @param period    the selected clip's length, or the idle window for a static posture
     * @param wrap      where a playing statue's clock wraps: a whole number of periods, so the selected
     *                  clip never sees the seam
     * @param clipLoops whether the selected active animation loops on its own; a one-shot one is
     *                  restarted every period by moving its start, never by rewinding the clock
     */
    public record FrozenPose(FloatingState state, ActiveAnimation animation, float seconds,
                             float period, float wrap, boolean clipLoops) {

        /** The held instant, for a fixed statue. */
        public Playhead held() {
            return new Playhead(seconds, 0f);
        }
    }

    /**
     * Where to sample a pose: {@code seconds} drives the state clock (pose idles, procedural parts,
     * the selected clip), {@code clipStart} is the selected active animation's start on that clock.
     */
    public record Playhead(float seconds, float clipStart) {}

    /** A whole number of periods close to {@link #PLAYBACK_WRAP_TARGET_SECONDS}. */
    private static float wrapFor(float period) {
        return period * Math.max(1, Math.round(PLAYBACK_WRAP_TARGET_SECONDS / period));
    }

    /**
     * Builds the frozen state for a species / animation / progress triple.
     *
     * <p>Seeking is done by pushing the animation's start point <em>into the past</em> rather than
     * by advancing the clock. {@code BedrockActiveAnimation.run} evaluates at
     * {@code state.animationSeconds - startedSeconds}, so leaving the clock at zero and setting
     * {@code startedSeconds = -seconds} lands on the wanted instant with continuous precision.
     * Advancing the clock instead would mean {@code updatePartialTicks}, which on
     * {@link FloatingState} <b>increments rather than replaces</b> and fires {@code runEffects} -
     * a frozen statue would emit the animation's particles and sounds every frame.</p>
     */
    public static FrozenPose freeze(StatueSpecies info, String animationName, float progress) {
        FloatingState state = new FloatingState();
        state.setCurrentAspects(info.aspects());
        PosableModel model = VaryingModelRepository.INSTANCE.getPoser(info.species(), state);
        state.setCurrentModel(model);

        boolean named = animationName != null && !animationName.isBlank();
        PoseClip clip = named ? poseBedrockAnimations(model).get(animationName) : null;

        // Two routes, and the distinction matters a great deal: bedrock bone transforms are
        // ADDITIVE (`xRot += …`, `xScale *= …` in BedrockAnimation.run). Applying one clip twice
        // therefore doubles every rotation - heads fold into torsos, limbs sink into bodies, a 45°
        // dive becomes 90°.
        if (clip != null) {
            // Pose route. The clip is one of this pose's own idle animations, so switching to the
            // pose is enough: applyAnimations already runs it, with the right part visibility and
            // alongside the pose's procedural companions (wing flap, look-at). Adding it as an
            // active animation on top would be the exact double application described above.
            float duration = effectiveDuration(clip.animation());
            float clockSeconds = clockFor(progress, duration);
            setClock(state, clockSeconds);
            state.setPose(clip.poseName());
            float period = periodFor(duration);
            // A looping pose clip wraps itself on its own length, like every other idle of the pose:
            // the clock must run on so none of them is cut. A one-shot pose clip has no start of its
            // own to move, so the only way to replay it is to wrap the clock on its length.
            float wrap = clip.animation().getShouldLoop() ? wrapFor(period) : period;
            return new FrozenPose(state, null, clockSeconds, period, wrap, true);
        }

        // Named route: namedAnimations, model-level animations, cry / faint. These are not part of
        // any pose's idle set, so they genuinely have to be layered over the portrait pose.
        ActiveAnimation anim = named ? resolveNamed(model, state, animationName) : null;
        // A duration of 0 is legitimate: many bedrock clips are a single static posture (Ho-Oh's
        // air_fly is 58 bones of constant rotations). They still pose the model.
        float duration = anim == null ? 0f : effectiveDuration(anim);
        float clockSeconds = clockFor(progress, duration);
        setClock(state, clockSeconds);
        state.setPoseToFirstSuitable(PoseType.PORTRAIT);
        float period = periodFor(duration);
        if (anim == null) {
            return new FrozenPose(state, null, clockSeconds, period, wrapFor(period), true);
        }
        applyAnimation(state, anim, 0f);
        return new FrozenPose(state, anim, clockSeconds, period, wrapFor(period), loopsItself(anim));
    }

    /** Whether an active animation replays on its own once past its end. */
    private static boolean loopsItself(ActiveAnimation anim) {
        ActiveAnimation inner = anim instanceof PrimaryAnimation primary ? primary.getAnimation() : anim;
        // Anything that is not a bedrock clip is procedural (MoLang curves of the clock): it has no
        // end to fall off, so leaving its start alone is right.
        return !(inner instanceof BedrockActiveAnimation bedrock) || bedrock.getAnimation().getShouldLoop();
    }

    private static float clockFor(float progress, float duration) {
        return Mth.clamp(progress, 0f, 1f) * periodFor(duration);
    }

    /** The span progress sweeps and playback loops over: the clip, or the idle window when static. */
    private static float periodFor(float duration) {
        return duration > 0f ? duration : STATIC_WINDOW_SECONDS;
    }

    /**
     * Plays a prepared pose instead of holding it: moves its clock to {@code elapsedSeconds} and
     * returns where to sample it (action plan 190, B7).
     *
     * <p><b>The clock is not wrapped on the selected clip.</b> A pose layers several idles of
     * different lengths, plus procedural parts driven by the clock; each looping bedrock clip already
     * wraps itself on its own {@code animation_length} ({@code BedrockAnimation.run}). Wrapping the
     * shared clock on the selected clip's length snapped every other layer back to zero at each seam,
     * which read as the animation stopping dead. The clock now runs continuously and wraps only on
     * {@link FrozenPose#wrap()}, a whole number of periods about an hour long. A one-shot selected
     * clip is replayed by moving its start to the current period instead ({@link Playhead#clipStart()}).</p>
     *
     * <p>{@code FloatingState} only lets the clock be <em>advanced</em> - {@code updatePartialTicks}
     * adds to the partial ticks - so the whole clock is carried by the partial ticks with the age
     * pinned at zero, and each frame adds the difference to the wanted instant. The advance also
     * runs keyframe effects over the interval just played, as a living model's would; with no entity
     * attached, as here, Cobblemon has nothing to emit them from.</p>
     */
    public static Playhead seek(FrozenPose pose, double elapsedSeconds) {
        float seconds = (float) (elapsedSeconds % pose.wrap());
        FloatingState state = pose.state();
        state.updateAge(0);
        state.updatePartialTicks(seconds * 20f - state.getPartialTicks());
        float clipStart = pose.clipLoops() ? 0f : seconds - (seconds % pose.period());
        return new Playhead(seconds, clipStart);
    }

    /**
     * Parks the state clock at {@code seconds}.
     *
     * <p>Must be called <b>before</b> any pose is selected. {@code FloatingState.updatePartialTicks}
     * is incremental and calls {@code runEffects}, which fires the current pose's particle and sound
     * keyframes - but it only walks the <em>current</em> pose's animations plus the active ones, and
     * both are still empty at this point, so here it is a no-op. Selecting a pose first would make a
     * frozen statue spray particles every frame.</p>
     */
    private static void setClock(FloatingState state, float seconds) {
        int wholeTicks = (int) Math.floor(seconds * 20f);
        state.updateAge(wholeTicks);
        state.updatePartialTicks(seconds * 20f - wholeTicks);
    }

    /** Cobblemon's own name lookup: namedAnimations, model animations, cry / faint, MoLang. */
    private static ActiveAnimation resolveNamed(PosableModel model, FloatingState state, String name) {
        try {
            return model.getAnimation(state, name, state.getRuntime());
        } catch (Exception e) {
            CobbleSafari.LOGGER.debug("[PokeStatue] animation '{}' failed to resolve", name, e);
            return null;
        }
    }

    /**
     * Playable length in seconds, or 0 for a static posture.
     *
     * <p>{@code animation_length} is optional in the bedrock format and defaults to {@code -1}, so
     * {@link ActiveAnimation#getDuration()} cannot be trusted on its own. When it is missing, the
     * last keyframe time across every bone timeline is the real length; animations built purely
     * from constant values or MoLang curves have neither, and are treated as static.</p>
     */
    private static float effectiveDuration(ActiveAnimation anim) {
        if (anim.getDuration() > 0f) {
            return anim.getDuration();
        }
        // A cry or an attack arrives wrapped in a PrimaryAnimation, which only copies the inner
        // duration: look through it, or a clip with no declared length would read as static.
        ActiveAnimation inner = anim instanceof PrimaryAnimation primary ? primary.getAnimation() : anim;
        if (inner instanceof BedrockActiveAnimation bedrock) {
            return effectiveDuration(bedrock.getAnimation());
        }
        return 0f;
    }

    /** Same, straight from a clip, for the pose route where no ActiveAnimation is ever built. */
    private static float effectiveDuration(BedrockAnimation animation) {
        double declared = animation.getAnimationLength();
        return declared > 0.0 ? (float) declared : lastKeyframeSeconds(animation);
    }

    private static float lastKeyframeSeconds(BedrockAnimation animation) {
        double last = 0.0;
        for (BedrockBoneTimeline timeline : animation.getBoneTimelines().values()) {
            for (BedrockBoneValue value : new BedrockBoneValue[]{
                    timeline.getPosition(), timeline.getRotation(), timeline.getScale()}) {
                if (value instanceof BedrockKeyFrameBoneValue keyframes && !keyframes.isEmpty()) {
                    last = Math.max(last, keyframes.lastKey());
                }
            }
        }
        return (float) last;
    }

    /**
     * (Re)installs the animation, started at {@code clipStart} on the state clock. Called every
     * frame: {@code applyAnimations} drops active animations whose {@code run} reports completion,
     * which would otherwise make the cached state silently decay back to the default pose.
     *
     * <p>{@code clipStart} is 0 for a held frame and for a looping clip: the clip is evaluated at
     * {@code animationSeconds - startedSeconds} and the clock already sits at the wanted instant.
     * Driving it by the clock rather than by the start is what lets the pose's own procedural idles -
     * wing flap, look-at, tail sway - move with it as well.</p>
     *
     * <p>A primary animation (cry, attack) needs its <b>inner</b> clip restarted too:
     * {@code addPrimaryAnimation} calls {@code start}, which stamps the inner clip with the current
     * clock, and re-adding it every frame therefore pinned it to its first frame forever - whatever
     * the progress, and with no playback at all.</p>
     */
    public static void applyAnimation(FloatingState state, ActiveAnimation anim, float clipStart) {
        if (anim instanceof PrimaryAnimation primary) {
            state.addPrimaryAnimation(primary);
            primary.setStarted(clipStart);
            if (primary.getAnimation() instanceof BedrockActiveAnimation inner) {
                inner.setStartedSeconds(clipStart);
            }
        } else {
            state.setActiveAnimations(anim);
            if (anim instanceof BedrockActiveAnimation bedrock) {
                bedrock.setStartedSeconds(clipStart);
            }
        }
    }

    // -- drawing ------------------------------------------------------------

    /**
     * Draws the statue model at the current {@link PoseStack} origin.
     *
     * @param playhead where to sample: {@link FrozenPose#held()} for a fixed statue, what
     *                 {@link #seek} returned for a playing one.
     * @param scale   final render scale ({@code fitScale × user scale}).
     * @param style   greyscale flag, multiply tint and alpha.
     * @param bodyYaw yaw in degrees the model should face.
     * @param locatorOrigin world position of the model origin when the locators must be filled for a
     *                      particle emitter (plan 194, B2), {@code null} otherwise.
     */
    public static void render(PoseStack ps, MultiBufferSource buffer, int packedLight,
                              StatueSpecies info, FrozenPose pose, Playhead playhead,
                              float scale, PokeStatueRenderStyle style, float bodyYaw,
                              @Nullable Vec3 locatorOrigin) {

        ResourceLocation species = info.species();
        FloatingState state = pose.state();
        PosableModel model = VaryingModelRepository.INSTANCE.getPoser(species, state);
        ResourceLocation texture = VaryingModelRepository.INSTANCE.getTexture(species, state);
        ResourceLocation drawn = style.greyscale() ? GreyscaleTextureCache.grey(texture) : texture;

        RenderContext ctx = new RenderContext();
        model.setContext(ctx);
        ctx.put(RenderContext.Companion.getRENDER_STATE(), RenderContext.RenderState.WORLD);
        ctx.put(RenderContext.Companion.getSPECIES(), species);
        ctx.put(RenderContext.Companion.getASPECTS(), info.aspects());
        ctx.put(RenderContext.Companion.getPOSABLE_STATE(), state);
        ctx.put(RenderContext.Companion.getTEXTURE(), drawn);
        ctx.put(RenderContext.Companion.getDO_QUIRKS(), false);
        state.setCurrentModel(model);

        // PosableModel instances are shared singletons via VaryingModelRepository: reset the tint
        // like Cobblemon's own renderers do, or a previous draw's colour bleeds in.
        model.setRed(1.0f);
        model.setGreen(1.0f);
        model.setBlue(1.0f);
        model.setAlpha(1.0f);

        if (pose.animation() != null) {
            applyAnimation(state, pose.animation(), playhead.clipStart());
        }
        int argb = packColor(style.rgb(), style.alpha());

        // try/finally keeps the PoseStack balanced even if the borrowed model throws while posing;
        // an unbalanced stack would corrupt every later draw in the frame.
        ps.pushPose();
        try {
            ps.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
            ps.scale(-scale, -scale, scale); // vanilla flip combined with the statue scale
            ps.translate(0.0, -0.001, 0.0);

            // Both the pose idles and the chosen animation are sampled at the same instant, so the
            // statue reads as one coherent moment rather than a frozen bust with an animated arm.
            model.applyAnimations(null, state, 0f, 0f, playhead.seconds() * 20f, 0f, 0f);
            if (locatorOrigin != null) {
                updateLocators(model, state, locatorOrigin, scale, bodyYaw);
            }

            RenderType renderType = style.translucent()
                    ? RenderType.entityTranslucent(drawn)
                    : RenderType.entityCutout(drawn);
            VertexConsumer vc = buffer.getBuffer(renderType);
            // No setLayerContext: emissive / translucent resolver layers are intentionally skipped,
            // they would draw in full colour over the desaturated base and glow on a statue.
            model.render(ctx, ps, vc, packedLight, OverlayTexture.NO_OVERLAY, argb);
            model.setDefault();
        } catch (Exception e) {
            CobbleSafari.LOGGER.debug("[PokeStatue] render failed for species '{}'", species, e);
        } finally {
            model.setRed(1.0f);
            model.setGreen(1.0f);
            model.setBlue(1.0f);
            model.setAlpha(1.0f);
            ps.popPose();
        }
    }

    /**
     * Fills the state's locator matrices in world space, the model posed as it is drawn. Cobblemon's
     * own {@code updateLocators} needs a Pokémon entity; {@code LocatorAccess.update} does not, so the
     * render transforms are replayed on a fresh stack anchored at the world origin.
     */
    private static void updateLocators(PosableModel model, FloatingState state, Vec3 origin,
                                       float scale, float bodyYaw) {
        PoseStack world = new PoseStack();
        world.translate(origin.x, origin.y, origin.z);
        world.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        world.scale(-scale, -scale, scale);
        world.translate(0.0, -0.001, 0.0);
        model.getLocatorAccess().update(world, null, scale, state.getLocatorStates(), true);
    }

    /** Packs a tint plus alpha into the colour int expected by {@code PosableModel.render}. */
    public static int packColor(int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
