package maxigregrze.cobblesafari.gts;

import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.api.mark.Marks;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.GtsSettings;
import maxigregrze.cobblesafari.security.WishLineValidator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class GtsDataLoader {
    private static final String PREFIX = "gts/unique_offers";
    private static final Pattern OFFER_ID_PATTERN = Pattern.compile("[a-z0-9._-]+");

    private GtsDataLoader() {}

    public static void load(MinecraftServer server) {
        GtsUniqueOfferRegistry.clear();
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources =
                manager.listResources(PREFIX, id -> id.getPath().endsWith(".json"));
        int ok = 0;
        int skipped = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    skipped++;
                    continue;
                }
                GtsUniqueOfferDefinition def = parseDefinition(root.getAsJsonObject(), entry.getKey().toString());
                if (def == null) {
                    skipped++;
                    continue;
                }
                if (GtsUniqueOfferRegistry.get(def.getOfferId()).isPresent()) {
                    CobbleSafari.LOGGER.warn(
                            "[GTS] unique offer id '{}' overwritten by {}",
                            def.getOfferId(),
                            entry.getKey());
                }
                GtsUniqueOfferRegistry.register(def);
                ok++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[GTS] Failed to load unique offer {}", entry.getKey(), e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("[GTS] Loaded {} unique offer definition(s), {} skipped", ok, skipped);
    }

    private static GtsUniqueOfferDefinition parseDefinition(JsonObject json, String sourcePath) {
        if (!json.has("offerId") || !json.has("given") || !json.has("wish")) {
            CobbleSafari.LOGGER.warn("[GTS] unique offer {} missing offerId/given/wish", sourcePath);
            return null;
        }
        String offerId = json.get("offerId").getAsString().trim();
        if (offerId.isEmpty() || !OFFER_ID_PATTERN.matcher(offerId).matches()) {
            CobbleSafari.LOGGER.warn("[GTS] unique offer {} has invalid offerId '{}'", sourcePath, offerId);
            return null;
        }
        String given = json.get("given").getAsString().trim();
        if (given.isEmpty()) {
            CobbleSafari.LOGGER.warn("[GTS] unique offer {} has empty given", sourcePath);
            return null;
        }
        GtsSettings cfg = GtsSettings.get();
        PokemonProperties givenProps = PokemonProperties.Companion.parse(given);
        if (givenProps.getSpecies() == null) {
            CobbleSafari.LOGGER.warn("[GTS] unique offer {} given line has no species: {}", sourcePath, given);
            return null;
        }
        try {
            Pokemon testGiven = givenProps.create(null);
            String speciesKey = testGiven.getSpecies().getResourceIdentifier().getPath().toLowerCase(Locale.ROOT);
            String speciesId = testGiven.getSpecies().getResourceIdentifier().toString().toLowerCase(Locale.ROOT);
            if (cfg.isSpeciesBanned(speciesKey) || cfg.isSpeciesBanned(speciesId)) {
                CobbleSafari.LOGGER.warn("[GTS] unique offer {} given species is banned", sourcePath);
                return null;
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("[GTS] unique offer {} could not create given pokemon", sourcePath, e);
            return null;
        }

        List<ResourceLocation> markIds = new ArrayList<>();
        if (json.has("givenMarks") && json.get("givenMarks").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("givenMarks")) {
                if (!el.isJsonPrimitive()) {
                    continue;
                }
                ResourceLocation markId = ResourceLocation.tryParse(el.getAsString().trim());
                if (markId == null) {
                    CobbleSafari.LOGGER.warn("[GTS] unique offer {} invalid mark id '{}'", sourcePath, el.getAsString());
                    continue;
                }
                Mark mark = Marks.getByIdentifier(markId);
                if (mark == null) {
                    CobbleSafari.LOGGER.warn(
                            "[GTS] unique offer {} unknown mark '{}' — skipped",
                            sourcePath,
                            markId);
                    continue;
                }
                markIds.add(markId);
            }
        }

        ResourceLocation activeMarkId = null;
        if (json.has("givenActiveMark")) {
            activeMarkId = ResourceLocation.tryParse(json.get("givenActiveMark").getAsString().trim());
            if (activeMarkId == null) {
                CobbleSafari.LOGGER.warn("[GTS] unique offer {} invalid givenActiveMark", sourcePath);
            } else if (Marks.getByIdentifier(activeMarkId) == null) {
                CobbleSafari.LOGGER.warn(
                        "[GTS] unique offer {} unknown givenActiveMark '{}' — ignored",
                        sourcePath,
                        activeMarkId);
                activeMarkId = null;
            } else if (!markIds.isEmpty() && !markIds.contains(activeMarkId)) {
                CobbleSafari.LOGGER.warn(
                        "[GTS] unique offer {} givenActiveMark not in givenMarks — ignored",
                        sourcePath);
                activeMarkId = null;
            }
        }

        JsonObject wish = json.getAsJsonObject("wish");
        if (!wish.has("species")) {
            CobbleSafari.LOGGER.warn("[GTS] unique offer {} wish missing species", sourcePath);
            return null;
        }
        String wishSpecies = wish.get("species").getAsString().trim();
        if (wishSpecies.isEmpty() || !WishLineValidator.isSafe(wishSpecies)) {
            CobbleSafari.LOGGER.warn("[GTS] unique offer {} invalid wish species line", sourcePath);
            return null;
        }
        PokemonProperties wishProps = PokemonProperties.Companion.parse(wishSpecies);
        if (wishProps.getSpecies() == null) {
            CobbleSafari.LOGGER.warn("[GTS] unique offer {} wish species unresolved", sourcePath);
            return null;
        }
        String wishName = wishProps.getSpecies();
        if (wishName != null) {
            String ws = wishName.toLowerCase(Locale.ROOT);
            if (cfg.isSpeciesBanned(ws)) {
                CobbleSafari.LOGGER.warn("[GTS] unique offer {} wish species is banned", sourcePath);
                return null;
            }
        }

        int levelBucket = -1;
        if (wish.has("levelBucket")) {
            levelBucket = wish.get("levelBucket").getAsInt();
            if (levelBucket != -1 && (levelBucket < 0 || levelBucket > 9)) {
                CobbleSafari.LOGGER.warn("[GTS] unique offer {} invalid levelBucket {}", sourcePath, levelBucket);
                return null;
            }
        }
        GenderFilter wishGender = GenderFilter.ANY;
        if (wish.has("gender")) {
            wishGender = GenderFilter.parse(wish.get("gender").getAsString());
        }
        GtsOffer.ShinyWish wishShiny = GtsOffer.ShinyWish.ANY;
        if (wish.has("shiny")) {
            wishShiny = GtsOffer.ShinyWish.parse(wish.get("shiny").getAsString());
        }
        if (!GtsService.isWishGenderCompatible(wishSpecies, wishGender)) {
            CobbleSafari.LOGGER.warn("[GTS] unique offer {} incompatible wish gender", sourcePath);
            return null;
        }

        return new GtsUniqueOfferDefinition(
                offerId,
                given,
                markIds,
                activeMarkId,
                wishSpecies,
                levelBucket,
                wishGender,
                wishShiny,
                sourcePath);
    }
}
