package maxigregrze.cobblesafari.csmusic;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tracks, per player, the context of the Cobblemon battle they are currently in, so csmusic trigger
 * rules can react to it (is it a battle? wild / NPC / PVP? which wild species/form?). Modeled on
 * {@link maxigregrze.cobblesafari.event.PartyBattlePowerEffects}: subscribe to
 * {@code BATTLE_STARTED_PRE}, snapshot the context, and clear it via {@code battle.getOnEndHandlers()}.
 */
public final class BattleMusicTracker {

    public enum Kind { WILD, NPC, PVP }

    public record BattleCtx(Kind kind, @Nullable String species, @Nullable String form) {}

    private static final ConcurrentHashMap<UUID, BattleCtx> BY_PLAYER = new ConcurrentHashMap<>();

    private BattleMusicTracker() {}

    public static void register() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL,
                (Consumer<BattleStartedEvent.Pre>) BattleMusicTracker::onBattleStart);
    }

    private static Unit onBattleStart(BattleStartedEvent.Pre event) {
        PokemonBattle battle = event.getBattle();
        Kind kind = classify(battle);
        WildInfo wild = kind == Kind.WILD ? wildInfo(battle) : null;
        BattleCtx ctx = new BattleCtx(kind,
                wild != null ? wild.species() : null,
                wild != null ? wild.form() : null);

        List<UUID> players = new ArrayList<>();
        for (BattleActor actor : battle.getActors()) {
            if (actor.getType() != ActorType.PLAYER) {
                continue;
            }
            for (UUID uuid : actor.getPlayerUUIDs()) {
                BY_PLAYER.put(uuid, ctx);       // must be stored before evaluating conditions below
                players.add(uuid);
            }
            // Suppress Cobblemon's native battle music only when a csmusic battle-track owns this
            // fight. Nulling the actor's theme makes Cobblemon send BattleMusicPacket(null) → the
            // client plays no native battle music. The actor is transient, so nothing to restore.
            if (actor instanceof PlayerBattleActor pba) {
                ServerPlayer player = resolvePlayer(actor);
                if (player != null && CsMusicTriggerRegistry.hasMatchingBattleRule(player)) {
                    pba.setBattleTheme(null);
                }
            }
        }
        if (!players.isEmpty()) {
            battle.getOnEndHandlers().add(b -> {
                for (UUID uuid : players) {
                    BY_PLAYER.remove(uuid);
                }
                return Unit.INSTANCE;
            });
        }
        return Unit.INSTANCE;
    }

    private static Kind classify(PokemonBattle battle) {
        int players = 0;
        boolean wild = false;
        for (BattleActor actor : battle.getActors()) {
            if (actor.getType() == ActorType.WILD) {
                wild = true;
            } else if (actor.getType() == ActorType.PLAYER) {
                players++;
            }
        }
        if (wild) {
            return Kind.WILD;
        }
        return players >= 2 ? Kind.PVP : Kind.NPC;
    }

    private record WildInfo(String species, String form) {}

    @Nullable
    private static WildInfo wildInfo(PokemonBattle battle) {
        for (BattleActor actor : battle.getActors()) {
            if (actor.getType() != ActorType.WILD) {
                continue;
            }
            for (BattlePokemon bp : actor.getPokemonList()) {
                Pokemon p = bp.getEffectedPokemon();
                String species = p.getSpecies().getResourceIdentifier().toString();
                String form = p.getForm().getName();
                return new WildInfo(species, form);
            }
        }
        return null;
    }

    /** Resolves the {@link ServerPlayer} behind a player actor via one of its owned Pokémon. */
    @Nullable
    private static ServerPlayer resolvePlayer(BattleActor actor) {
        for (BattlePokemon bp : actor.getPokemonList()) {
            if (bp.getEffectedPokemon().getOwnerPlayer() instanceof ServerPlayer sp) {
                return sp;
            }
        }
        return null;
    }

    @Nullable
    public static BattleCtx of(UUID playerUuid) {
        return BY_PLAYER.get(playerUuid);
    }

    public static void clear(UUID playerUuid) {
        BY_PLAYER.remove(playerUuid);
    }
}
