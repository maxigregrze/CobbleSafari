package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.client.UltraLootClientTicker;
import maxigregrze.cobblesafari.item.ultraloot.ReconArmorLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Ultra Recon trail on the warp ride's rider (action plan 200, section 4.2): the world's glide
 * trail ({@link UltraLootClientTicker}) does not reach the screen, so the screen keeps its own.
 *
 * <p>Same emitters - the backpack nozzle and the tip of each wing - and the same pair per tick: a
 * firework spark and a puff of smoke, blue at the wings. A particle keeps the lateral position it
 * was born at and lives on the <b>scenery's</b> distance ({@code decorOdometer}), not the ride's:
 * that one holds at 0 through the intro, while the scenery already streams at the start tunnel's
 * pace. So the trail is left behind from the intro on, and dies past the fifth plane.</p>
 */
public final class WarpRideReconTrail {

    private static final double EMIT_INTERVAL_SECONDS = 1.0 / 20.0;
    private static final long LIFE_NANOS = 800_000_000L;
    private static final int MAX_PARTICLES = 512;
    private static final int SPRITE_FRAMES = 8;
    private static final float SPARK_SIZE_BLOCKS = 0.1f;
    private static final float SMOKE_SIZE_BLOCKS = 0.2f;
    /** Sideways scatter and the smoke's rise, in canvas px per second. */
    private static final double SCATTER_PX = 20.0;
    private static final double SMOKE_RISE_PX = 15.0;

    private record Particle(double x, double y, double vx, double vy, double trackPos, long born,
                            boolean spark, float[] tint) {}

    private final List<Particle> particles = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private double emitDebt;

    public void clear() {
        particles.clear();
        emitDebt = 0.0;
    }

    /** Ages the trail, and emits while the rider rides in the full set (never during the warp). */
    public void advance(WarpRideClientState state, WarpRideRiderProxy rider, double deltaSeconds) {
        long now = System.nanoTime();
        particles.removeIf(p -> now - p.born() > LIFE_NANOS
                || p.trackPos() - state.decorOdometer() < state.fifthPlaneDepth);
        // From the intro on (the rider already flies through the start tunnel), never on the
        // tutorial pages nor during the warp.
        boolean emitting = rider != null
                && (state.phase == WarpRideClientState.PHASE_INTRO || state.phase == WarpRideClientState.PHASE_RIDING)
                && ReconArmorLogic.isFullSet(rider);
        if (!emitting) {
            emitDebt = 0.0;
            return;
        }
        emitDebt += deltaSeconds;
        while (emitDebt >= EMIT_INTERVAL_SECONDS) {
            emitDebt -= EMIT_INTERVAL_SECONDS;
            emitAll(state, now);
        }
    }

    private void emitAll(WarpRideClientState state, long now) {
        double px = WarpRideRenderer.RIDER_PX_PER_BLOCK;
        double radius = WarpRideRenderer.riderRadius(state);
        double bank = Math.toRadians(state.bankDegrees());
        double cos = Math.cos(bank);
        double sin = Math.sin(bank);
        // Rider-local offsets: x to the side, y up the back (towards the axis), before the roll.
        double[][] emitters = {
                {0.0, UltraLootClientTicker.NOZZLE_UP * px},
                {UltraLootClientTicker.WING_TIP_SIDE * px, UltraLootClientTicker.WING_TIP_UP * px},
                {-UltraLootClientTicker.WING_TIP_SIDE * px, UltraLootClientTicker.WING_TIP_UP * px}
        };
        for (int i = 0; i < emitters.length; i++) {
            double ox = emitters[i][0] * cos - emitters[i][1] * sin;
            double oy = emitters[i][0] * sin + emitters[i][1] * cos;
            float[] tint = i == 0 ? null : UltraLootClientTicker.WING_TRAIL_RGB;
            spawn(ox, oy - radius, state.decorOdometer(), now, true, tint);
            spawn(ox, oy - radius, state.decorOdometer(), now, false, tint);
        }
    }

    private void spawn(double x, double y, double trackPos, long now, boolean spark, float[] tint) {
        if (particles.size() >= MAX_PARTICLES) {
            particles.remove(0);
        }
        double vx = (random.nextDouble() * 2.0 - 1.0) * SCATTER_PX;
        double vy = (random.nextDouble() * 2.0 - 1.0) * SCATTER_PX + (spark ? 0.0 : SMOKE_RISE_PX);
        particles.add(new Particle(x, y, vx, vy, trackPos, now, spark, tint));
    }

    /** Camera-facing sprite quads in the open perspective pass, after the rider. */
    public void render(PoseStack pose, MultiBufferSource buffers, WarpRideClientState state) {
        if (particles.isEmpty()) {
            return;
        }
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_PARTICLES);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_PARTICLES));
        long now = System.nanoTime();
        // Farthest first: the oldest particles are the nearest to the eye, so walk newest to oldest.
        Iterator<Particle> it = particles.reversed().iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            double age = (now - p.born()) / (double) LIFE_NANOS;
            double depth = p.trackPos() - state.decorOdometer();
            float alpha = (float) ((1.0 - age) * WarpRideRenderer.opacity(state, Math.min(0.0, depth)));
            if (alpha <= 0.01f) {
                continue;
            }
            int frame = SPRITE_FRAMES - 1 - Math.min(SPRITE_FRAMES - 1, (int) (age * SPRITE_FRAMES));
            TextureAtlasSprite sprite = atlas.getSprite(ResourceLocation.withDefaultNamespace(
                    (p.spark() ? "spark_" : "generic_") + frame));
            double seconds = (now - p.born()) / 1_000_000_000.0;
            float x = (float) (p.x() + p.vx() * seconds);
            float y = (float) (p.y() + p.vy() * seconds);
            float z = WarpRideScene3D.zFor(WarpRideRenderer.depthScale(state, Math.min(0.0, depth)));
            float half = (p.spark() ? SPARK_SIZE_BLOCKS : SMOKE_SIZE_BLOCKS) * WarpRideRenderer.RIDER_PX_PER_BLOCK / 2.0f;
            float[] rgb = p.tint() == null ? new float[]{1.0f, 1.0f, 1.0f} : p.tint();
            quad(consumer, pose.last(), x, y, z, half, sprite, rgb, alpha);
        }
    }

    @SuppressWarnings("java:S107")
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float half,
                             TextureAtlasSprite sprite, float[] rgb, float alpha) {
        vertex(consumer, pose, x - half, y - half, z, sprite.getU0(), sprite.getV1(), rgb, alpha);
        vertex(consumer, pose, x + half, y - half, z, sprite.getU1(), sprite.getV1(), rgb, alpha);
        vertex(consumer, pose, x + half, y + half, z, sprite.getU1(), sprite.getV0(), rgb, alpha);
        vertex(consumer, pose, x - half, y + half, z, sprite.getU0(), sprite.getV0(), rgb, alpha);
    }

    @SuppressWarnings("java:S107")
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float[] rgb, float alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(rgb[0], rgb[1], rgb[2], alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0f, 0.0f, 1.0f);
    }
}
