package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.world.OasisSpawnerProcessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public class ModProcessors {

    private ModProcessors() {}

    public static final StructureProcessorType<OasisSpawnerProcessor> OASIS_SPAWNER_PROCESSOR =
            Registry.register(
                    BuiltInRegistries.STRUCTURE_PROCESSOR,
                    ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "oasis_spawner_processor"),
                    () -> OasisSpawnerProcessor.CODEC
            );

    public static void register() {
        CobbleSafari.LOGGER.info("Registering structure processors for " + CobbleSafari.MOD_ID);
    }
}
