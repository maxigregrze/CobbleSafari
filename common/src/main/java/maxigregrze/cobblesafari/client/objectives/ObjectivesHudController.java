package maxigregrze.cobblesafari.client.objectives;

import maxigregrze.cobblesafari.client.hud.ObjectivesHudOverlay;
import maxigregrze.cobblesafari.config.HudConfig;
import maxigregrze.cobblesafari.network.ObjectivesHudSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import java.util.List;

/**
 * Client-side state machine for the objectives HUD: holds the last synced
 * objectives, drives the open/closed slide animation, tracks activity/join timers and the
 * per-objective notification dots.
 */
public final class ObjectivesHudController {

    public static final int AREA_WIDTH = 256;
    public static final int AREA_HEIGHT = 50;
    private static final int CLOSED_VISIBLE = 29;
    public static final int CLOSED_OFFSET = AREA_WIDTH - CLOSED_VISIBLE; // 227
    private static final long NOTIF_DURATION_MS = 10_000L;
    private static final int MAX_OBJECTIVES = 3;
    /** X of the task-text component within the area. */
    private static final int TASK_TEXT_X = 30;
    /** Right padding added after the widest task string when sizing the open view. */
    private static final int OPEN_TEXT_PADDING = 2;

    private static boolean visible = false;
    private static List<ObjectivesHudSyncPayload.ObjectiveView> objectives = List.of();
    private static final int[] prevProgress = {-1, -1, -1};
    private static final long[] notifUntilMs = {0L, 0L, 0L};

    private static float offset = CLOSED_OFFSET;
    private static long clientTick = 0L;
    private static long joinOpenUntilTick = 0L;
    private static int inactiveTicks = 0;
    /** Session force-open flag; initialized from {@link HudConfig#isObjectivesForceOpen()}. */
    private static boolean forceOpen = false;

    private static boolean hasLast = false;
    private static double lastX;
    private static double lastY;
    private static double lastZ;
    private static float lastYRot;
    private static float lastXRot;

    private ObjectivesHudController() {}

    /** Re-apply config defaults (client start or {@code /cobblesafari refresh}). */
    public static void applyHudConfigDefaults() {
        forceOpen = HudConfig.isObjectivesForceOpen();
    }

    public static void toggleForceOpen() {
        forceOpen = !forceOpen;
    }

    public static void accept(ObjectivesHudSyncPayload payload) {
        boolean wasVisible = visible;
        visible = payload.visible();
        objectives = payload.objectives();
        if (!visible) {
            for (int i = 0; i < MAX_OBJECTIVES; i++) {
                prevProgress[i] = -1;
                notifUntilMs[i] = 0L;
            }
            return;
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < objectives.size() && i < MAX_OBJECTIVES; i++) {
            int p = objectives.get(i).progress();
            if (prevProgress[i] >= 0 && p > prevProgress[i]) {
                notifUntilMs[i] = now + NOTIF_DURATION_MS;
            }
            prevProgress[i] = p;
        }
        if (!wasVisible) {
            joinOpenUntilTick = clientTick + HudConfig.getObjectivesAutoHideTicks();
            offset = computeOpenOffset(Minecraft.getInstance()); // open on entry (content-sized)
        }
    }

    public static void clientTick(Minecraft mc) {
        clientTick++;
        if (!visible || mc.player == null) {
            return;
        }
        boolean active = detectActivity(mc);
        inactiveTicks = active ? 0 : inactiveTicks + 1;

        boolean shouldOpen;
        if (forceOpen) {
            shouldOpen = true;
        } else if (clientTick < joinOpenUntilTick) {
            shouldOpen = true;
        } else {
            shouldOpen = inactiveTicks >= HudConfig.getObjectivesInactivityShowsTicks();
        }

        float target = shouldOpen ? computeOpenOffset(mc) : CLOSED_OFFSET;
        float speed = (float) CLOSED_OFFSET / HudConfig.getObjectivesAnimationTicks();
        if (offset < target) {
            offset = Math.min(target, offset + speed);
        } else if (offset > target) {
            offset = Math.max(target, offset - speed);
        }
    }

    private static boolean detectActivity(Minecraft mc) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yRot = mc.player.getYRot();
        float xRot = mc.player.getXRot();
        boolean active = hasLast && (x != lastX || y != lastY || z != lastZ || yRot != lastYRot || xRot != lastXRot);
        lastX = x;
        lastY = y;
        lastZ = z;
        lastYRot = yRot;
        lastXRot = xRot;
        hasLast = true;
        return active;
    }

    /**
     * Open-state slide offset so the visible section matches the content width:
     * {@code openWidth = min(AREA_WIDTH, 30 + maxTaskTextWidth + 2)}, capped to the texture size.
     */
    private static float computeOpenOffset(Minecraft mc) {
        Font font = mc.font;
        int maxText = 0;
        for (int i = 0; i < objectives.size() && i < MAX_OBJECTIVES; i++) {
            maxText = Math.max(maxText, font.width(ObjectivesHudOverlay.formatTaskText(objectives.get(i))));
        }
        int openWidth = Math.min(AREA_WIDTH, TASK_TEXT_X + maxText + OPEN_TEXT_PADDING);
        return AREA_WIDTH - openWidth;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static List<ObjectivesHudSyncPayload.ObjectiveView> objectives() {
        return objectives;
    }

    public static float offset() {
        return offset;
    }

    /** Whether the notification dot for objective {@code i} should be shown. */
    public static boolean notifActive(int i) {
        if (i < 0 || i >= objectives.size()) {
            return false;
        }
        if (objectives.get(i).complete()) {
            return true;
        }
        return i < MAX_OBJECTIVES && System.currentTimeMillis() < notifUntilMs[i];
    }
}
