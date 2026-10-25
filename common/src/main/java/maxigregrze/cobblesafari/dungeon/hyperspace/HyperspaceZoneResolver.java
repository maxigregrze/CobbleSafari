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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns the marker habitats of a fresh {@code dungeon_paris} instance into Z-A wild zones
 * (action plan 196, § 3): rolls the instance's star and types once, then writes one cluster into
 * every marker as an inline {@code custom__} pool. Cobblemon's natural spawning does the rest.
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

        Map<String, List<HyperspaceCluster>> bags = new HashMap<>();
        Set<String> targetSpecies = new LinkedHashSet<>();
        int resolved = 0;
        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                for (BlockEntity be : List.copyOf(level.getChunk(cx, cz).getBlockEntities().values())) {
                    if (!(be instanceof HabitatBlockEntity habitat) || !isMarker(habitat)) {
                        continue;
                    }
                    String type = types.get(rng.nextInt(types.size()));
                    HyperspaceCluster cluster = drawCluster(bags, star, type, rng);
                    if (cluster == null) {
                        continue;
                    }
                    apply(habitat, cluster);
                    targetSpecies.add(speciesId(cluster.lead()));
                    resolved++;
                }
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

    /** Weighted draw without replacement (decision § 3.3); the bag refills once empty. */
    private static HyperspaceCluster drawCluster(Map<String, List<HyperspaceCluster>> bags,
                                                 int star, String type, RandomSource rng) {
        List<HyperspaceCluster> bag = bags.computeIfAbsent(type, k -> new ArrayList<>());
        if (bag.isEmpty()) {
            bag.addAll(HyperspaceClusterRegistry.clusters(star, type));
            if (bag.isEmpty()) {
                return null;
            }
        }
        int total = 0;
        for (HyperspaceCluster c : bag) {
            total += c.weight();
        }
        int roll = rng.nextInt(total);
        for (int i = 0; i < bag.size(); i++) {
            roll -= bag.get(i).weight();
            if (roll < 0) {
                return bag.remove(i);
            }
        }
        return bag.remove(bag.size() - 1);
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
