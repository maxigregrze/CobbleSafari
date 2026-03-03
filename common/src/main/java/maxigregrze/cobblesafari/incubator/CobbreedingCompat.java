package maxigregrze.cobblesafari.incubator;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

public final class CobbreedingCompat {

    private static final int COBBREEDING_DEFAULT_TIMER = 600;

    private CobbreedingCompat() {
    }

    private static Boolean cobbreedingLoaded = null;

    public static boolean isCobbreedingLoaded() {
        if (cobbreedingLoaded == null) {
            cobbreedingLoaded = Services.PLATFORM.isModLoaded("cobbreeding");
        }
        return cobbreedingLoaded;
    }

    public static boolean isCobbreedingEgg(ItemStack stack) {
        if (!isCobbreedingLoaded() || stack.isEmpty()) return false;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId.getNamespace().equals("cobbreeding")
                && (itemId.getPath().endsWith("_egg") || itemId.getPath().endsWith("_pokemon_egg"));
    }

    @SuppressWarnings("unchecked")
    public static int getTimer(ItemStack stack) {
        DataComponentType<Integer> timerType = (DataComponentType<Integer>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                        ResourceLocation.fromNamespaceAndPath("cobbreeding", "timer"));
        if (timerType == null) return COBBREEDING_DEFAULT_TIMER;
        Integer timer = stack.get(timerType);
        return timer != null ? timer : COBBREEDING_DEFAULT_TIMER;
    }

    @SuppressWarnings("unchecked")
    public static String getEggName(ItemStack stack) {
        DataComponentType<String> nameType = (DataComponentType<String>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                        ResourceLocation.fromNamespaceAndPath("cobbreeding", "name"));
        if (nameType == null) return "Unknown";
        String name = stack.get(nameType);
        return name != null ? name : "Unknown";
    }

    @SuppressWarnings("unchecked")
    public static PokemonProperties extractProperties(ItemStack stack) {
        DataComponentType<String> propsType = (DataComponentType<String>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                        ResourceLocation.fromNamespaceAndPath("cobbreeding", "pokemon_properties"));
        if (propsType != null) {
            String props = stack.get(propsType);
            if (props != null && !props.isEmpty()) {
                return PokemonProperties.Companion.parse(props);
            }
        }

        DataComponentType<String> eggInfoType = (DataComponentType<String>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                        ResourceLocation.fromNamespaceAndPath("cobbreeding", "egg_info"));
        if (eggInfoType != null) {
            String encrypted = stack.get(eggInfoType);
            if (encrypted != null && !encrypted.isEmpty()) {
                return decryptViaReflection(encrypted);
            }
        }

        return null;
    }

    private static PokemonProperties decryptViaReflection(String encrypted) {
        try {
            Class<?> eggUtilsClass = Class.forName("ludichat.cobbreeding.EggUtilities");
            Method decryptMethod = eggUtilsClass.getDeclaredMethod("decrypt", String.class);
            Object result = decryptMethod.invoke(null, encrypted);
            if (result instanceof PokemonProperties pp) {
                return pp;
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[Incubator] Failed to decrypt Cobbreeding egg via reflection", e);
        }
        return null;
    }
}
