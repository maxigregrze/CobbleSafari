package maxigregrze.cobblesafari.compat.cte;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrialSpawnerNbtBuilder {

    private TrialSpawnerNbtBuilder() {}

    @Nullable
    public static CompoundTag buildNbt(LevelReader levelReader, String mobType) {
        OasisSpawnerPool pool = OasisSpawnerPool.POOLS.get(mobType);
        if (pool == null) return null;

        RegistryAccess registryAccess = getRegistryAccess(levelReader);
        if (registryAccess == null) return null;

        try {
            CompoundTag root = new CompoundTag();

            root.put("normal_config", buildConfig(registryAccess, pool, false));
            root.put("ominous_config", buildConfig(registryAccess, pool, true));
            root.putInt("target_cooldown_length", pool.spawnerCooldown());
            root.putInt("required_player_range", pool.playerDetectionRange());

            return root;
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Failed to build CTE trial spawner NBT for mob '{}'", mobType, e);
            return null;
        }
    }

    private static CompoundTag buildConfig(RegistryAccess registryAccess, OasisSpawnerPool pool,
                                           boolean isOminous) {
        CompoundTag config = new CompoundTag();
        config.putInt("spawn_range", pool.spawnRange());
        config.putFloat("total_mobs", pool.totalPokemonPerTrial());
        config.putFloat("simultaneous_mobs", pool.maxSimultaneousPokemon());
        config.putFloat("total_mobs_added_per_player", 1.0f);
        config.putFloat("simultaneous_mobs_added_per_player", 1.0f);
        config.putInt("ticks_between_spawn", 40);
        config.putBoolean("enable_ominous_spawner_attacks", false);

        List<OasisSpawnerPool.PokemonEntry> entries = isOminous
                ? pool.ominousPokemon() : pool.normalPokemon();
        config.put("spawn_potentials", buildSpawnPotentials(registryAccess, entries,
                pool.doPokemonSpawnedGlow()));

        String lootTable = isOminous ? pool.ominousLootTable() : pool.lootTable();
        config.put("loot_tables_to_eject", buildLootTables(lootTable));

        config.putString("items_to_drop_when_ominous",
                "minecraft:spawners/trial_chamber/items_to_drop_when_ominous");

        return config;
    }

    private static ListTag buildSpawnPotentials(RegistryAccess registryAccess,
                                                List<OasisSpawnerPool.PokemonEntry> entries,
                                                boolean glow) {
        ListTag list = new ListTag();
        for (OasisSpawnerPool.PokemonEntry entry : entries) {
            CompoundTag weighted = new CompoundTag();

            CompoundTag entityNbt = buildPokemonEntityNbt(registryAccess, entry, glow);
            CompoundTag spawnDataTag = new CompoundTag();
            spawnDataTag.put("entity", entityNbt);

            weighted.put("data", spawnDataTag);
            weighted.putInt("weight", entry.weight());
            list.add(weighted);
        }
        return list;
    }

    private static CompoundTag buildPokemonEntityNbt(RegistryAccess registryAccess,
                                                     OasisSpawnerPool.PokemonEntry entry,
                                                     boolean glow) {
        StringBuilder propsBuilder = new StringBuilder();
        propsBuilder.append("species=").append(entry.species());
        propsBuilder.append(" level=").append(entry.level());
        if (entry.form() != null && !entry.form().isEmpty()) {
            propsBuilder.append(" form=").append(entry.form());
        }
        if (entry.gender() != null && !entry.gender().isEmpty()) {
            propsBuilder.append(" gender=").append(entry.gender());
        }

        PokemonProperties props = PokemonProperties.Companion.parse(propsBuilder.toString());
        Pokemon pokemon = props.create();

        CompoundTag pokemonNbt = pokemon.saveToNBT(registryAccess, new CompoundTag());

        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", "cobblemon:pokemon");
        entityTag.put("Pokemon", pokemonNbt);
        entityTag.putString("PoseType", "WALK");

        if (glow) {
            entityTag.putByte("Glowing", (byte) 1);
        }

        if (entry.mustBeDefeatedInBattle()) {
            entityTag.putBoolean("Invulnerable", true);
        }

        return entityTag;
    }

    private static ListTag buildLootTables(String lootTableId) {
        ListTag list = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putString("data", lootTableId);
        entry.putInt("weight", 1);
        list.add(entry);
        return list;
    }

    @Nullable
    private static RegistryAccess getRegistryAccess(LevelReader levelReader) {
        if (levelReader instanceof Level level) {
            return level.registryAccess();
        }
        return null;
    }
}
