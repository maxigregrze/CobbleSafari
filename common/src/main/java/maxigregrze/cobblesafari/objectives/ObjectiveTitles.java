package maxigregrze.cobblesafari.objectives;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Screen titles of the dimensional objectives, shown one after the other (action plan 196, § 7.7):
 * a title shows for {@link #DURATION_TICKS}, so a queued one waits for the previous to fade out.
 */
public final class ObjectiveTitles {

    private static final int FADE_IN = 10;
    private static final int STAY = 40;
    private static final int FADE_OUT = 10;
    public static final int DURATION_TICKS = FADE_IN + STAY + FADE_OUT;

    private record Pending(UUID player, int dueTick, Component title) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    private ObjectiveTitles() {}

    public static void show(ServerPlayer player, Component title) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(FADE_IN, STAY, FADE_OUT));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** Shows {@code title} once the title sent now has faded out. */
    public static void showNext(ServerPlayer player, Component title) {
        PENDING.add(new Pending(player.getUUID(), player.getServer().getTickCount() + DURATION_TICKS, title));
    }

    /** Server tick: sends the titles now due; a player who left meanwhile simply misses theirs. */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        int now = server.getTickCount();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (pending.dueTick() > now) {
                continue;
            }
            it.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(pending.player());
            if (player != null) {
                show(player, pending.title());
            }
        }
    }
}
