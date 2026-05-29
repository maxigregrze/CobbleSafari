package maxigregrze.cobblesafari.power;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.data.GuaranteedShinySavedData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuaranteedShinyManager {

    private static final Set<String> READY_LOGGED = ConcurrentHashMap.newKeySet();

    private GuaranteedShinyManager() {}

    public static void request(ServerPlayer player, GuaranteedShinyRequest req) {
        UUID uuid = player.getUUID();
        GuaranteedShinySavedData.get(player.server).put(uuid, req);
        forgetReadyLogged(uuid, req.key());
        String variant = req.variantIndex() == null
                ? "all"
                : PowerVariantRegistry.suffix(req.variantIndex());
        CobbleSafari.LOGGER.info(
                "[GuaranteedShiny] armed: player={} ({}) key={} effect={} variant={} triggerGameTime={}",
                player.getName().getString(), uuid, req.key(), req.requiredEffectId(), variant,
                req.triggerGameTime());
    }

    public static void requestForEffect(ServerPlayer player, String key, Holder<MobEffect> effect,
                                        Integer variantIndex, int remainingDurationTicks, float windowFraction) {
        long now = player.serverLevel().getGameTime();
        int window = Math.max(1, Math.round(remainingDurationTicks * windowFraction));
        long trigger = now + player.getRandom().nextInt(window);
        ResourceLocation effectId = effect.unwrapKey().map(ResourceKey::location).orElse(null);
        request(player, new GuaranteedShinyRequest(key, trigger, -1L, effectId, variantIndex));
    }

    public static void clear(ServerPlayer player, String key) {
        UUID uuid = player.getUUID();
        GuaranteedShinySavedData.get(player.server).remove(uuid, key);
        forgetReadyLogged(uuid, key);
    }

    public static void clearByPrefix(ServerPlayer player, String prefix) {
        UUID uuid = player.getUUID();
        GuaranteedShinySavedData data = GuaranteedShinySavedData.get(player.server);
        for (String key : new ArrayList<>(data.getRequests(uuid).keySet())) {
            if (key.startsWith(prefix)) {
                forgetReadyLogged(uuid, key);
            }
        }
        data.removeByPrefix(uuid, prefix);
    }

    public static boolean tryConsume(ServerPlayer player, PokemonEntity pe) {
        GuaranteedShinySavedData data = GuaranteedShinySavedData.get(player.server);
        UUID uuid = player.getUUID();
        List<GuaranteedShinyRequest> snapshot = new ArrayList<>(data.getRequests(uuid).values());
        if (snapshot.isEmpty()) {
            return false;
        }
        long now = player.serverLevel().getGameTime();
        for (GuaranteedShinyRequest req : snapshot) {
            if (req.requiredEffectId() != null && !playerHasEffect(player, req.requiredEffectId())) {
                data.remove(uuid, req.key());
                forgetReadyLogged(uuid, req.key());
                continue;
            }
            if (req.expiryGameTime() >= 0 && now > req.expiryGameTime()) {
                data.remove(uuid, req.key());
                forgetReadyLogged(uuid, req.key());
                continue;
            }
            if (now < req.triggerGameTime()) {
                continue;
            }
            logReadyOnce(player, req, now);
            Integer vi = req.variantIndex();
            if (vi != null && !PowerVariantRegistry.pokemonHasVariantType(pe.getPokemon(), vi)) {
                continue;
            }
            String species = pe.getPokemon().getSpecies().getName();
            CobbleSafari.LOGGER.info(
                    "[GuaranteedShiny] appeared: player={} ({}) key={} species={} gameTime={}",
                    player.getName().getString(), uuid, req.key(), species, now);
            pe.getPokemon().setShiny(true);
            data.remove(uuid, req.key());
            forgetReadyLogged(uuid, req.key());
            return true;
        }
        return false;
    }

    private static void logReadyOnce(ServerPlayer player, GuaranteedShinyRequest req, long now) {
        String readyKey = readyLogKey(player.getUUID(), req.key());
        if (!READY_LOGGED.add(readyKey)) {
            return;
        }
        String variant = req.variantIndex() == null
                ? "all"
                : PowerVariantRegistry.suffix(req.variantIndex());
        CobbleSafari.LOGGER.info(
                "[GuaranteedShiny] ready: player={} ({}) key={} variant={} gameTime={} (next matching spawn will be shiny)",
                player.getName().getString(), player.getUUID(), req.key(), variant, now);
    }

    private static String readyLogKey(UUID uuid, String key) {
        return uuid + ":" + key;
    }

    private static void forgetReadyLogged(UUID uuid, String key) {
        READY_LOGGED.remove(readyLogKey(uuid, key));
    }

    private static boolean playerHasEffect(ServerPlayer player, ResourceLocation effectId) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect == null) {
            return false;
        }
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        return player.hasEffect(holder);
    }
}
