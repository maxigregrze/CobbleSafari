package maxigregrze.cobblesafari.power;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.data.GuaranteedShinySavedData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GuaranteedShinyManager {

    private GuaranteedShinyManager() {}

    public static void request(ServerPlayer player, GuaranteedShinyRequest req) {
        GuaranteedShinySavedData.get(player.server).put(player.getUUID(), req);
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
        GuaranteedShinySavedData.get(player.server).remove(player.getUUID(), key);
    }

    public static void clearByPrefix(ServerPlayer player, String prefix) {
        GuaranteedShinySavedData.get(player.server).removeByPrefix(player.getUUID(), prefix);
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
                continue;
            }
            if (req.expiryGameTime() >= 0 && now > req.expiryGameTime()) {
                data.remove(uuid, req.key());
                continue;
            }
            if (now < req.triggerGameTime()) {
                continue;
            }
            Integer vi = req.variantIndex();
            if (vi != null && !PowerVariantRegistry.pokemonHasVariantType(pe.getPokemon(), vi)) {
                continue;
            }
            pe.getPokemon().setShiny(true);
            data.remove(uuid, req.key());
            return true;
        }
        return false;
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
