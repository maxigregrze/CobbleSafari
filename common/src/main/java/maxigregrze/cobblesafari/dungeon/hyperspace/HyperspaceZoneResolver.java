package maxigregrze.cobblesafari.dungeon.hyperspace;

import com.cobblemon.mod.common.api.habitats.HabitatPool;
import com.cobblemon.mod.common.api.habitats.HabitatSpawn;
import com.cobblemon.mod.common.api.habitats.spawningstyle.NaturalHabitatSpawning;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.block.habitat.HabitatBlockEntity;
import com.cobblemon.mod.common.pokemon.Species;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.data.HyperspaceZoneSavedData;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns the marker habitats of a fresh {@code dungeon_paris} instance into Z-A wild zones
 * (action plan 196, § 3): rolls the instance's star, types and {@value #HERD_COUNT} herds once, then
 * shares the herds out evenly over the markers, each written as an inline {@code custom__} pool.
 * Cobblemon's natural spawning does the rest.
 */
public final class HyperspaceZoneResolver {

    public static final ResourceLocation MARKER_POOL =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "hyperspace_wild");
    private static final String RESOLVED_NAME = "cobblesafari.habitat.hyperspace_wild.resolved";
    private static final String BUCKET = "common";
    private static final float ALPHA_SHARE = 0.01F;

    /** Star weights, index 0 = ★1 (decision § 3.2). */
    private static final int[] STAR_WEIGHTS = {35, 25, 20, 13, 7};
    private static final int TWO_TYPES_PERCENT = 40;
    /** Distinct herds per instance, whatever the marker count, so some species show up twice (§ 3.3). */
    private static final int HERD_COUNT = 4;

    private HyperspaceZoneResolver() {}

    /** Rolls the instance, rewrites every marker habitat in the chunk window, records the summary. */
    public static void resolve(ServerLevel level, UUID instanceId,
                               int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ) {
        if (HyperspaceClusterRegistry.isEmpty()) {
            CobbleSafari.LOGGER.warn("[Hyperspace] no cluster loaded - instance {} stays empty", instanceId);
            return;
        }
        RandomSource rng = level.getRandom();
        int star = rollStar(rng);
        List<String> types = rollTypes(star, rng);
        if (types.isEmpty()) {
            CobbleSafari.LOGGER.warn("[Hyperspace] no type available at star {} - instance {} stays empty", star, instanceId);
            return;
        }

        List<HabitatBlockEntity> markers = new ArrayList<>();
        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                for (BlockEntity be : List.copyOf(level.getChunk(cx, cz).getBlockEntities().values())) {
                    if (be instanceof HabitatBlockEntity habitat && isMarker(habitat)) {
                        markers.add(habitat);
                    }
                }
            }
        }
        List<HyperspaceCluster> herds = pickHerds(star, types, rng);

        // Dealt round-robin over shuffled markers: 3 markers show 3 herds, 7 show each herd once or twice.
        Set<String> targetSpecies = new LinkedHashSet<>();
        int resolved = 0;
        if (!herds.isEmpty()) {
            Util.shuffle(markers, rng);
            Util.shuffle(herds, rng);
            for (int i = 0; i < markers.size(); i++) {
                HyperspaceCluster cluster = herds.get(i % herds.size());
                apply(markers.get(i), cluster);
                targetSpecies.add(speciesId(cluster.lead()));
                resolved++;
            }
        }
        if (resolved == 0) {
            CobbleSafari.LOGGER.warn("[Hyperspace] instance {}: no '{}' habitat found in chunks [{},{}]..[{},{}]",
                    instanceId, MARKER_POOL, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);
        }
        HyperspaceZoneSavedData.get(level.getServer())
                .put(instanceId, new HyperspaceZone(star, types, new ArrayList<>(targetSpecies)));
        CobbleSafari.LOGGER.info("[Hyperspace] instance {}: star {}, types {}, {} habitat(s) resolved",
                instanceId, star, types, resolved);
    }

    private static boolean isMarker(HabitatBlockEntity habitat) {
        HabitatPool pool = habitat.getPool();
        return pool != null && MARKER_POOL.equals(pool.getId());
    }

    private static int rollStar(RandomSource rng) {
        int total = 0;
        for (int w : STAR_WEIGHTS) {
            total += w;
        }
        int roll = rng.nextInt(total);
        for (int i = 0; i < STAR_WEIGHTS.length; i++) {
            roll -= STAR_WEIGHTS[i];
            if (roll < 0) {
                return i + 1;
            }
        }
        return 1;
    }

    private static List<String> rollTypes(int star, RandomSource rng) {
        List<String> available = new ArrayList<>(HyperspaceClusterRegistry.typesAt(star));
        int count = available.size() > 1 && rng.nextInt(100) < TWO_TYPES_PERCENT ? 2 : 1;
        List<String> picked = new ArrayList<>(count);
        for (int i = 0; i < count && !available.isEmpty(); i++) {
            picked.add(available.remove(rng.nextInt(available.size())));
        }
        return picked;
    }

    /**
     * The instance's herds (decision § 3.3), all with a different lead species. One type: four draws.
     * Two types: one herd of each, then the species found in both types' pools - one or two are all
     * taken (one is topped up by a random type), three or more give two draws, none gives one more of
     * each type. A pool too small to fill the four is topped up from the other type.
     */
    private static List<HyperspaceCluster> pickHerds(int star, List<String> types, RandomSource rng) {
        List<HyperspaceCluster> herds = new ArrayList<>();
        Set<String> leads = new HashSet<>();
        List<HyperspaceCluster> first = HyperspaceClusterRegistry.clusters(star, types.get(0));
        if (types.size() == 1) {
            drawInto(herds, leads, first, HERD_COUNT, rng);
            return herds;
        }
        List<HyperspaceCluster> second = HyperspaceClusterRegistry.clusters(star, types.get(1));
        drawInto(herds, leads, first, 1, rng);
        drawInto(herds, leads, second, 1, rng);
        List<HyperspaceCluster> shared = shared(first, second, leads, rng);
        if (shared.size() >= 2) {
            drawInto(herds, leads, shared, 2, rng);
        } else if (shared.size() == 1) {
            drawInto(herds, leads, shared, 1, rng);
            drawInto(herds, leads, rng.nextBoolean() ? first : second, 1, rng);
        } else {
            drawInto(herds, leads, first, 1, rng);
            drawInto(herds, leads, second, 1, rng);
        }
        List<HyperspaceCluster> both = new ArrayList<>(first);
        both.addAll(second);
        drawInto(herds, leads, both, HERD_COUNT - herds.size(), rng);
        return herds;
    }

    /** One cluster per lead species present in both pools (from either pool at random), leads not yet taken. */
    private static List<HyperspaceCluster> shared(List<HyperspaceCluster> first, List<HyperspaceCluster> second,
                                                  Set<String> taken, RandomSource rng) {
        Map<String, HyperspaceCluster> firstByLead = new LinkedHashMap<>();
        for (HyperspaceCluster c : first) {
            firstByLead.putIfAbsent(leadOf(c), c);
        }
        Map<String, HyperspaceCluster> shared = new LinkedHashMap<>();
        for (HyperspaceCluster c : second) {
            String lead = leadOf(c);
            HyperspaceCluster other = firstByLead.get(lead);
            if (other != null && !taken.contains(lead) && !shared.containsKey(lead)) {
                shared.put(lead, rng.nextBoolean() ? other : c);
            }
        }
        return new ArrayList<>(shared.values());
    }

    /** Up to {@code count} weighted draws without replacement, skipping leads already taken. */
    private static void drawInto(List<HyperspaceCluster> herds, Set<String> leads, List<HyperspaceCluster> pool,
                                 int count, RandomSource rng) {
        List<HyperspaceCluster> candidates = new ArrayList<>(pool);
        for (int n = 0; n < count; n++) {
            candidates.removeIf(c -> leads.contains(leadOf(c)));
            if (candidates.isEmpty()) {
                return;
            }
            double total = 0;
            for (HyperspaceCluster c : candidates) {
                total += c.weight();
            }
            double roll = rng.nextDouble() * total;
            HyperspaceCluster picked = candidates.get(candidates.size() - 1);
            for (HyperspaceCluster c : candidates) {
                roll -= c.weight();
                if (roll < 0) {
                    picked = c;
                    break;
                }
            }
            herds.add(picked);
            leads.add(leadOf(picked));
        }
    }

    private static String leadOf(HyperspaceCluster cluster) {
        return cluster.lead().speciesName();
    }

    private static void apply(HabitatBlockEntity habitat, HyperspaceCluster cluster) {
        if (!(habitat.getSpawningStyle() instanceof NaturalHabitatSpawning)) {
            CobbleSafari.LOGGER.warn("[Hyperspace] habitat at {} is a marker but not 'cobblemon:natural' - "
                    + "resolved anyway, fix its NBT", habitat.getBlockPos());
        }
        List<HabitatSpawn> spawns = new ArrayList<>();
        for (HyperspaceCluster.Member member : cluster.members()) {
            Species species = PokemonSpecies.INSTANCE.getByName(member.speciesName());
            if (species == null) {
                continue;
            }
            spawns.add(spawn(species, member, member.pokemon(), member.rarity() * (1F - ALPHA_SHARE), false));
            spawns.add(spawn(species, member, member.pokemon() + " alpha=true", member.rarity() * ALPHA_SHARE, true));
        }
        HabitatPool pool = new HabitatPool();
        pool.setId(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                "custom__hyperspace_" + cluster.star() + "_" + cluster.type() + "_" + cluster.id()));
        pool.setName(RESOLVED_NAME);
        pool.setSpawns(spawns);
        habitat.setPool(pool);
        habitat.refreshFromSettings();
        habitat.setChanged();
        if (habitat.getLevel() != null) {
            habitat.getLevel().sendBlockUpdated(habitat.getBlockPos(), habitat.getBlockState(), habitat.getBlockState(), 3);
        }
    }

    private static HabitatSpawn spawn(Species species, HyperspaceCluster.Member member, String properties,
                                      float weight, boolean alpha) {
        HabitatSpawn spawn = new HabitatSpawn();
        spawn.setSpecies(species);
        spawn.setSpawnablePositionType(member.position());
        spawn.setBucket(BUCKET);
        spawn.setWeight(weight);
        spawn.setLevelRange(alpha ? member.alphaLevel() : member.level());
        spawn.setModifiers(PokemonProperties.Companion.parse(properties));
        return spawn;
    }

    private static String speciesId(HyperspaceCluster.Member member) {
        Species species = PokemonSpecies.INSTANCE.getByName(member.speciesName());
        return species != null
                ? species.getResourceIdentifier().toString()
                : "cobblemon:" + member.speciesName();
    }
}
