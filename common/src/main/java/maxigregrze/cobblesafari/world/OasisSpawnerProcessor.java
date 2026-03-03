package maxigregrze.cobblesafari.world;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.compat.cte.TrialSpawnerNbtBuilder;
import maxigregrze.cobblesafari.init.ModProcessors;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class OasisSpawnerProcessor extends StructureProcessor {

    public static final MapCodec<OasisSpawnerProcessor> CODEC = MapCodec.unit(OasisSpawnerProcessor::new);

    private static final boolean CTE_LOADED = Services.PLATFORM.isModLoaded("cobblemontrialsedition");

    private static final ResourceLocation CTE_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath("cobblemontrialsedition", "cobblemon_trial_spawner");

    @Override
    @Nullable
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos pos,
            BlockPos pivot,
            StructureTemplate.StructureBlockInfo originalBlockInfo,
            StructureTemplate.StructureBlockInfo currentBlockInfo,
            StructurePlaceSettings settings) {

        if (!currentBlockInfo.state().is(Blocks.SPAWNER)) {
            return currentBlockInfo;
        }

        String mobType = extractMobType(currentBlockInfo.nbt());
        if (mobType == null) {
            return currentBlockInfo;
        }

        CobbleSafari.LOGGER.debug("Oasis processor: found spawner with mob type '{}' at {}",
                mobType, currentBlockInfo.pos());

        if (!CTE_LOADED) {
            CobbleSafari.LOGGER.debug("CTE not loaded - replacing spawner with air");
            return new StructureTemplate.StructureBlockInfo(
                    currentBlockInfo.pos(),
                    Blocks.AIR.defaultBlockState(),
                    null
            );
        }

        Optional<Block> cteBlockOpt = BuiltInRegistries.BLOCK.getOptional(CTE_BLOCK_ID);
        if (cteBlockOpt.isEmpty()) {
            CobbleSafari.LOGGER.warn("CTE block not found in registry despite mod being loaded - replacing spawner with air");
            return new StructureTemplate.StructureBlockInfo(
                    currentBlockInfo.pos(),
                    Blocks.AIR.defaultBlockState(),
                    null
            );
        }

        Block cteBlock = cteBlockOpt.get();

        CompoundTag trialSpawnerNbt = TrialSpawnerNbtBuilder.buildNbt(level, mobType);
        if (trialSpawnerNbt == null) {
            CobbleSafari.LOGGER.warn("Failed to build trial spawner NBT for mob '{}' - replacing spawner with air", mobType);
            return new StructureTemplate.StructureBlockInfo(
                    currentBlockInfo.pos(),
                    Blocks.AIR.defaultBlockState(),
                    null
            );
        }

        CobbleSafari.LOGGER.debug("CTE loaded - replacing spawner with trial spawner for mob '{}'", mobType);
        return new StructureTemplate.StructureBlockInfo(
                currentBlockInfo.pos(),
                cteBlock.defaultBlockState(),
                trialSpawnerNbt
        );
    }

    @Nullable
    private static String extractMobType(@Nullable CompoundTag nbt) {
        if (nbt == null) return null;
        if (!nbt.contains("SpawnData")) return null;
        CompoundTag spawnData = nbt.getCompound("SpawnData");
        if (!spawnData.contains("entity")) return null;
        CompoundTag entity = spawnData.getCompound("entity");
        return entity.contains("id") ? entity.getString("id") : null;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModProcessors.OASIS_SPAWNER_PROCESSOR;
    }
}
