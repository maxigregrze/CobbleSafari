package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Bascule des blocs réactifs d'une arène anneau par anneau (plan 199 § 8) : un anneau = une bande
 * d'un bloc de distance horizontale au trigger, un anneau par tick. Ouverture du centre vers le bord,
 * fermeture du bord vers le centre. Toutes les hauteurs d'un anneau basculent ensemble.
 */
final class ArenaWave {

    private final List<List<BlockPos>> rings = new ArrayList<>();
    private final List<BlockPos> all;
    private final boolean battle;
    private int cursor;

    ArenaWave(List<BlockPos> blocks, BlockPos origin, boolean battle) {
        this.all = new ArrayList<>(blocks);
        this.battle = battle;
        TreeMap<Integer, List<BlockPos>> byRing = new TreeMap<>();
        for (BlockPos p : blocks) {
            double dx = p.getX() - origin.getX();
            double dz = p.getZ() - origin.getZ();
            int ring = (int) Math.floor(Math.sqrt(dx * dx + dz * dz));
            byRing.computeIfAbsent(ring, k -> new ArrayList<>()).add(p);
        }
        // Anneaux vides conservés : la vague garde un rythme d'un bloc par tick.
        int max = byRing.isEmpty() ? -1 : byRing.lastKey();
        for (int r = 0; r <= max; r++) {
            rings.add(byRing.getOrDefault(r, List.of()));
        }
        if (!battle) {
            java.util.Collections.reverse(rings);
        }
    }

    /** Bascule l'anneau courant ; {@code true} quand la vague est terminée (passage de refresh fait). */
    boolean tick(ServerLevel level) {
        if (cursor >= rings.size()) {
            return true;
        }
        ArenaBlockScanner.setBattleState(level, rings.get(cursor++), battle);
        if (cursor >= rings.size()) {
            refreshMimics(level);
            return true;
        }
        return false;
    }

    /** Bascule immédiatement tout ce qui reste, puis refait le passage de refresh. */
    void flush(ServerLevel level) {
        while (cursor < rings.size()) {
            ArenaBlockScanner.setBattleState(level, rings.get(cursor++), battle);
        }
        refreshMimics(level);
    }

    /**
     * Pendant la vague, un anneau a résolu son apparence contre des voisins pas encore basculés :
     * on refait la résolution une fois tout l'arène dans son état final.
     */
    private void refreshMimics(ServerLevel level) {
        for (BlockPos p : all) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof CsBossMimicBlock) {
                CsBossMimicBlock.refresh(level, p, state, true);
            }
        }
    }
}
