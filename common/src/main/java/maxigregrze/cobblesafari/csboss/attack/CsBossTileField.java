package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Suivi des tuiles d'attaque posées par une attaque (plan 199 § 10.1) : les blocs
 * ({@code CsBossTileBlock}) ne font que le visuel et leur cycle ; l'attaque enregistre chaque lot
 * avec sa fenêtre active et applique dégâts / effets <b>une fois par joueur et par lot</b>.
 */
public final class CsBossTileField {

    /** Lot de tuiles posées ensemble (même fenêtre active). */
    public static final class Batch {
        private final Set<BlockPos> cells;
        private final int activeFrom;
        private final int activeTo;
        private final Set<UUID> hit = new HashSet<>();

        private Batch(Set<BlockPos> cells, int activeFrom, int activeTo) {
            this.cells = cells;
            this.activeFrom = activeFrom;
            this.activeTo = activeTo;
        }

        public boolean isEmpty() {
            return cells.isEmpty();
        }
    }

    private final List<Batch> batches = new ArrayList<>();

    /**
     * Pose {@code state} au-dessus de chaque surface libre (ou décorée) et enregistre le lot : actif de
     * {@code now + warnTicks} à {@code now + warnTicks + activeTicks}.
     */
    public Batch place(ServerLevel level, Collection<BlockPos> surfaces, BlockState state,
                       int now, int warnTicks, int activeTicks) {
        Set<BlockPos> cells = new HashSet<>();
        for (BlockPos surface : surfaces) {
            if (CsBossSurfaceScanner.placeOnSurface(level, surface, state)) {
                cells.add(surface.above());
            }
        }
        Batch batch = new Batch(cells, now + warnTicks, now + warnTicks + activeTicks);
        if (!cells.isEmpty()) {
            batches.add(batch);
        }
        return batch;
    }

    /** Appelle {@code onHit} une fois par joueur et par lot quand ses pieds sont dans une tuile active. */
    public void tick(ServerLevel level, BossBattleSession session, int now, BiConsumer<ServerPlayer, Batch> onHit) {
        Iterator<Batch> it = batches.iterator();
        while (it.hasNext()) {
            Batch b = it.next();
            if (now >= b.activeTo) {
                it.remove();
                continue;
            }
            if (now < b.activeFrom) {
                continue;
            }
            for (ServerPlayer p : session.aliveParticipants(level)) {
                if (b.hit.contains(p.getUUID())) {
                    continue;
                }
                BlockPos cell = BlockPos.containing(p.getX(), p.getY() + 0.1, p.getZ());
                // Au-dessus : pieds dans un sol plus bas qu'un bloc (dalle, escalier), tuile posée sur lui.
                if (b.cells.contains(cell) || b.cells.contains(cell.above())) {
                    b.hit.add(p.getUUID());
                    onHit.accept(p, b);
                }
            }
        }
    }

    public void clear() {
        batches.clear();
    }
}
