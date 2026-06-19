package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Giratina orb (distortion_4): rendered with the block model/texture
 * {@code giratina_core_moving}. Carries the <b>vanilla luminous outline ("glow")</b> colored red
 * via a scoreboard team. Driven by the attack (follows a player, floats 1 block above the ground).
 */
public class AttackGiratinaOrbEntity extends AbstractAttackEntity {

    private static final String RED_TEAM = "cobblesafari_red_glow";

    public AttackGiratinaOrbEntity(EntityType<? extends AttackGiratinaOrbEntity> type, Level level) {
        super(type, level);
        this.setGlowingTag(true); // vanilla luminous outline
    }

    public static AttackGiratinaOrbEntity spawn(ServerLevel level, double x, double y, double z, int sessionId) {
        AttackGiratinaOrbEntity e = new AttackGiratinaOrbEntity(ModEntities.ATTACK_GIRATINA_ORB, level);
        e.setSessionId(sessionId);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        joinRedTeam(level, e);
        return e;
    }

    /** Places the entity on a red team so the "glow" outline is red. */
    private static void joinRedTeam(ServerLevel level, AttackGiratinaOrbEntity e) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(RED_TEAM);
        if (team == null) {
            team = scoreboard.addPlayerTeam(RED_TEAM);
            team.setColor(ChatFormatting.RED);
        }
        scoreboard.addPlayerToTeam(e.getScoreboardName(), team);
    }

    /** Rotates the orb to face a horizontal target, snapping the previous yaw so there is no lerp wobble. */
    public void facePlayer(double targetX, double targetZ) {
        double dx = targetX - this.getX();
        double dz = targetZ - this.getZ();
        if (Math.abs(dx) < 1.0E-4 && Math.abs(dz) < 1.0E-4) {
            return;
        }
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yRotO = yaw;
    }

    @Override
    protected int maxLifespan() {
        return 200; // safety TTL (10 s); the attack removes them at the end, darkness outlasts them by 1 s
    }
}
