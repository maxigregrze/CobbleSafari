package maxigregrze.cobblesafari.block.csboss;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bloc « réactif » d'arène (plan 100 § 10) : implémenté par les blocs qui changent d'état
 * au démarrage/fin d'un combat de boss (porte d'entrée, etc.). Le scan ne touche QUE
 * les blocs portant cette interface — jamais d'édition de monde arbitraire.
 */
public interface BattleReactiveBlock {

    /** Bascule l'état idle↔combat. Doit être idempotent et purement local au bloc. */
    void setBattleState(ServerLevel level, BlockPos pos, BlockState state, boolean battle);

    /** {@code true} si ce bloc, dans cet état, doit être capturé par le scan. */
    default boolean isReactive(BlockState state) {
        return true;
    }
}
