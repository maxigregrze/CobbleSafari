package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackZoneRingEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * {@code jump_4} — Arc-en-ciel (plan 199 § 13.4, AREA ; revu après retours). Le carré est coupé en huit
 * cercles de même largeur ; les deux plus petits forment la zone rouge, les six autres vont de l'orange
 * au violet (le dernier jusqu'aux coins). À chaque cycle, Ho-Oh s'illumine de la couleur « sûre »
 * pendant la charge (4 s la première fois, 3 s ensuite) ; puis toutes les autres zones éclatent
 * (5 dégâts). La couleur suivante n'est jamais à plus de 3 couleurs de la précédente. L'attaque se
 * termine sur la dernière explosion.
 */
public class JumpRainbowArcAttack implements CsBossAttack {

    /** Couleurs des zones = couleurs d'équipe vanilla, pour que le contour du boss corresponde exactement. */
    private static final ChatFormatting[] FORMATS = {
            ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
            ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE
    };
    private static final String TEAM_PREFIX = "csboss_rainbow_";
    /** Huit cercles : le rouge prend les deux premiers. */
    private static final int BANDS = FORMATS.length + 1;
    private static final int MAX_COLOR_JUMP = 3;
    /** Charge de chaque cycle : la première un peu plus longue, aucune attente après la dernière explosion. */
    private static final int[] CHARGE_TICKS = {80, 60, 60};
    private static final int BLAST_TICKS = 8;
    private static final int REST_TICKS = 8;
    private static final int[] CYCLE_START = new int[CHARGE_TICKS.length];
    private static final int DURATION; // 240

    static {
        int t = 0;
        for (int i = 0; i < CHARGE_TICKS.length; i++) {
            CYCLE_START[i] = t;
            t += CHARGE_TICKS[i] + BLAST_TICKS + REST_TICKS;
        }
        DURATION = t - REST_TICKS;
    }

    private static final float IDLE_ALPHA = 0.2F;
    private static final float CHARGE_ALPHA = 0.5F;
    private static final float BLAST_ALPHA = 0.9F;
    private static final double HIT_HEIGHT = 3.0;
    private static final float DAMAGE = 5.0F; // base 5

    private final String id;
    private final AttackZoneRingEntity[] rings = new AttackZoneRingEntity[FORMATS.length];
    private double cx;
    private double cz;
    private double floorY;
    private double bandWidth;
    private int safe = -1;
    private int tick;
    private boolean done;

    public JumpRainbowArcAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.AREA;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        this.safe = -1;
        this.cx = boss.getX();
        this.cz = boss.getZ();
        this.floorY = session.getFloorY();
        double half = session.getPlayerRadius();
        this.bandWidth = half / BANDS;
        for (int i = 0; i < FORMATS.length; i++) {
            // Zone i = cercles i+1 (et 0 pour le rouge) ; la violette s'étend jusqu'aux coins du carré.
            float inner = i == 0 ? 0.0F : (float) ((i + 1) * bandWidth);
            float outer = i == FORMATS.length - 1 ? (float) (half * Math.sqrt(2.0) + 1.0) : (float) ((i + 2) * bandWidth);
            rings[i] = session.trackAttackEntity(AttackZoneRingEntity.spawn(level, cx, floorY, cz, session.getId(),
                    inner, outer, color(FORMATS[i]), IDLE_ALPHA, DURATION + 20));
        }
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        for (int i = 0; i < CYCLE_START.length; i++) {
            int t = tick - CYCLE_START[i];
            if (t == 0) {
                safe = nextSafe(level);
                setAlpha(CHARGE_ALPHA, -1);
                glow(level, boss, FORMATS[safe]);
                boss.triggerAttackAnimation();
                CsBossAttackLib.sound(level, cx, floorY + 1.0, cz,
                        "minecraft:block.amethyst_block.chime", SoundSource.HOSTILE, 2.0F, 0.6F + safe * 0.1F);
            } else if (t == CHARGE_TICKS[i]) {
                blast(level, session);
            } else if (t == CHARGE_TICKS[i] + BLAST_TICKS) {
                setAlpha(IDLE_ALPHA, -1);
                unglow(level, boss);
            }
        }
        if (++tick >= DURATION) {
            unglow(level, boss);
            for (AttackZoneRingEntity ring : rings) {
                if (ring != null && ring.isAlive()) {
                    ring.discard();
                }
            }
            done = true;
        }
    }

    /** Couleur sûre suivante : différente de la précédente et à au plus {@link #MAX_COLOR_JUMP} d'elle. */
    private int nextSafe(ServerLevel level) {
        java.util.List<Integer> options = new java.util.ArrayList<>();
        for (int c = 0; c < FORMATS.length; c++) {
            if (c != safe && (safe < 0 || Math.abs(c - safe) <= MAX_COLOR_JUMP)) {
                options.add(c);
            }
        }
        return options.get(level.getRandom().nextInt(options.size()));
    }

    private void blast(ServerLevel level, BossBattleSession session) {
        setAlpha(BLAST_ALPHA, safe);
        CsBossAttackLib.sound(level, cx, floorY + 1.0, cz,
                "minecraft:entity.firework_rocket.blast", SoundSource.HOSTILE, 2.0F, 0.8F);
        for (ServerPlayer p : session.aliveParticipants(level)) {
            double dx = p.getX() - cx;
            double dz = p.getZ() - cz;
            int band = Math.min(BANDS - 1, (int) Math.floor(Math.sqrt(dx * dx + dz * dz) / bandWidth));
            int zone = Math.max(0, band - 1);
            if (zone != safe && Math.abs(p.getY() - floorY) <= HIT_HEIGHT) {
                CsBossDamage.hurt(p, session, DAMAGE);
            }
        }
    }

    /** Opacité de toutes les zones, sauf {@code except} (laissée à l'opacité de charge). */
    private void setAlpha(float alpha, int except) {
        for (int i = 0; i < rings.length; i++) {
            if (rings[i] != null && rings[i].isAlive()) {
                rings[i].setAlpha(i == except ? CHARGE_ALPHA : alpha);
            }
        }
    }

    /** Contour lumineux du boss dans la couleur sûre (même mécanisme que l'orbe de Giratina). */
    private static void glow(ServerLevel level, CsBossEntity boss, ChatFormatting format) {
        Scoreboard scoreboard = level.getScoreboard();
        String name = TEAM_PREFIX + format.getName();
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            team = scoreboard.addPlayerTeam(name);
            team.setColor(format);
        }
        scoreboard.addPlayerToTeam(boss.getScoreboardName(), team);
        boss.setGlowingTag(true);
    }

    private static void unglow(ServerLevel level, CsBossEntity boss) {
        boss.setGlowingTag(false);
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(boss.getScoreboardName());
        if (team != null && team.getName().startsWith(TEAM_PREFIX)) {
            scoreboard.removePlayerFromTeam(boss.getScoreboardName(), team);
        }
    }

    private static int color(ChatFormatting format) {
        Integer c = format.getColor();
        return c == null ? 0xFFFFFF : c;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
