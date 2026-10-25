package maxigregrze.cobblesafari.dungeon.hyperspace.obstacle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The replayable pieces of a Cobblemon move action effect (action plan 196, § 8.4). A battle
 * timeline needs a battle context and a target entity, so the obstacle sequence replays its parts
 * instead: the user's pose, its particles and sounds, then the target's particles and sounds.
 *
 * @param animations  the first animation keyframe of the user; the model plays the first it knows
 * @param targetDelayTicks time from the start of the move to the first effect on the target
 */
public record MoveFieldEffect(Set<String> animations, List<ActorParticle> actorParticles,
                              List<ResourceLocation> targetParticles, List<ResourceLocation> actorSounds,
                              List<ResourceLocation> targetSounds, int targetDelayTicks) {

    public record ActorParticle(ResourceLocation effect, List<String> locators) {}

    private static final String DIRECTORY = "action_effects/moves/";
    private static final String TARGET_CONDITION = "is_user == false";
    private static final Pattern SOUND = Pattern.compile("q\\.sound\\('([^']+)'\\)");
    private static final int DEFAULT_DELAY_TICKS = 10;
    private static final int MAX_DELAY_TICKS = 60;

    /** Empty optional = the move has no (readable) action effect: use the fallback. */
    private static final Map<String, Optional<MoveFieldEffect>> CACHE = new ConcurrentHashMap<>();

    static void clearCache() {
        CACHE.clear();
    }

    public static Optional<MoveFieldEffect> get(MinecraftServer server, String moveName) {
        return CACHE.computeIfAbsent(moveName, name -> read(server, name));
    }

    private static Optional<MoveFieldEffect> read(MinecraftServer server, String moveName) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("cobblemon", DIRECTORY + moveName + ".json");
        Optional<Resource> resource = server.getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return Optional.empty();
        }
        try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject() || !root.getAsJsonObject().has("timeline")) {
                return Optional.empty();
            }
            List<JsonObject> keyframes = new ArrayList<>();
            flatten(root.getAsJsonObject().getAsJsonArray("timeline"), keyframes);
            return Optional.of(parse(keyframes));
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("[Obstacles] unreadable action effect {} - using the fallback", id, e);
            return Optional.empty();
        }
    }

    /** Sequences are played in order, so they read as their keyframes inlined. */
    private static void flatten(JsonArray array, List<JsonObject> out) {
        for (JsonElement el : array) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject frame = el.getAsJsonObject();
            if (frame.has("keyframes") && frame.get("keyframes").isJsonArray()) {
                flatten(frame.getAsJsonArray("keyframes"), out);
            } else {
                out.add(frame);
            }
        }
    }

    private static MoveFieldEffect parse(List<JsonObject> keyframes) {
        Set<String> animations = new LinkedHashSet<>();
        List<ActorParticle> actorParticles = new ArrayList<>();
        Set<ResourceLocation> targetParticles = new LinkedHashSet<>();
        Set<ResourceLocation> actorSounds = new LinkedHashSet<>();
        Set<ResourceLocation> targetSounds = new LinkedHashSet<>();
        double seconds = 0;
        Double targetAt = null;
        for (JsonObject frame : keyframes) {
            String type = frame.has("type") ? frame.get("type").getAsString() : "";
            boolean onTarget = frame.has("entityCondition")
                    && frame.get("entityCondition").getAsString().contains(TARGET_CONDITION);
            switch (type) {
                case "animation" -> {
                    if (!onTarget && animations.isEmpty()) {
                        frame.getAsJsonArray("animation").forEach(a -> animations.add(a.getAsString()));
                    }
                }
                case "entity_particles" -> {
                    ResourceLocation effect = ResourceLocation.tryParse(frame.get("effect").getAsString());
                    if (effect == null) {
                        CobbleSafari.LOGGER.debug("[Obstacles] invalid particle id in an action effect");
                    } else if (onTarget) {
                        targetParticles.add(effect);
                        if (targetAt == null) {
                            targetAt = seconds;
                        }
                    } else {
                        actorParticles.add(new ActorParticle(effect, strings(frame, "locators")));
                    }
                }
                case "entity_molang" -> {
                    for (String expression : strings(frame, "expressions")) {
                        Matcher m = SOUND.matcher(expression);
                        while (m.find()) {
                            ResourceLocation sound = ResourceLocation.tryParse(
                                    m.group(1).contains(":") ? m.group(1) : "cobblemon:" + m.group(1));
                            if (sound != null) {
                                (onTarget ? targetSounds : actorSounds).add(sound);
                            }
                        }
                    }
                }
                case "entity_sound" -> {
                    ResourceLocation sound = ResourceLocation.tryParse(frame.get("sound").getAsString());
                    if (sound != null) {
                        (onTarget ? targetSounds : actorSounds).add(sound);
                    }
                }
                default -> {
                    // add_holds / remove_holds have no visible effect
                }
            }
            seconds += number(frame, "pause") + number(frame, "delay");
        }
        int delay = targetAt == null ? DEFAULT_DELAY_TICKS : (int) Math.round(targetAt * 20);
        return new MoveFieldEffect(animations, actorParticles, new ArrayList<>(targetParticles),
                new ArrayList<>(actorSounds), new ArrayList<>(targetSounds),
                Math.max(1, Math.min(MAX_DELAY_TICKS, delay)));
    }

    private static double number(JsonObject frame, String key) {
        JsonElement el = frame.get(key);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber() ? el.getAsDouble() : 0;
    }

    private static List<String> strings(JsonObject frame, String key) {
        List<String> out = new ArrayList<>();
        if (frame.has(key) && frame.get(key).isJsonArray()) {
            frame.getAsJsonArray(key).forEach(e -> out.add(e.getAsString()));
        }
        return out;
    }
}
