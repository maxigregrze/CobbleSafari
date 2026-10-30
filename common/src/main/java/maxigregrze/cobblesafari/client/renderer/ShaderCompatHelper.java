package maxigregrze.cobblesafari.client.renderer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ShaderCompatHelper {

    private static final String CAPTURED_RENDERING_STATE_CLASS = "net.irisshaders.iris.uniforms.CapturedRenderingState";
    private static final String WORLD_RENDERING_SETTINGS_CLASS = "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings";
    private static final String IRIS_CLASS = "net.irisshaders.iris.Iris";
    private static final String DIMENSION_ID_CLASS = "net.irisshaders.iris.shaderpack.DimensionId";
    private static final String NAMESPACED_ID_CLASS = "net.irisshaders.iris.shaderpack.materialmap.NamespacedId";

    /** Effects that make one of our dimensions an End for the shader pack (action plan 197, decision 2.2). */
    private static final Set<ResourceLocation> END_STYLE_EFFECTS = Set.of(
            ResourceLocation.withDefaultNamespace("the_end"),
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_distortion"),
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_paris"));

    private static Boolean irisLoaded;
    private static Method isShaderPackInUseMethod;
    private static Object irisApiInstance;

    private static Boolean blockEntityIdSpoofAvailable;
    private static boolean blockEntityIdSpoofWarned;
    private static Object capturedRenderingState;
    private static Method setCurrentBlockEntityMethod;
    private static Method getCurrentBlockEntityMethod;
    private static Object worldRenderingSettings;
    private static Method getBlockStateIdsMethod;

    private static Boolean dimensionOverrideAvailable;
    private static boolean dimensionOverrideWarned;
    private static Object irisEndDimension;
    private static Method getCurrentPackMethod;
    private static Constructor<?> namespacedIdConstructor;
    private static Method dimensionMapGetter;
    private static Field dimensionMapField;

    private ShaderCompatHelper() {}

    public static boolean isIrisShaderActive() {
        if (irisLoaded == null) {
            initIris();
        }
        if (!irisLoaded) return false;
        try {
            return (boolean) isShaderPackInUseMethod.invoke(irisApiInstance);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Runs {@code emitSurface} while Iris tags emitted vertices with the block entity id the current
     * shader pack gives to {@code minecraft:end_portal}, so the pack draws its end portal effect on them.
     * Falls back to a plain call when the pack has no such id or Iris internals are unavailable.
     */
    public static void renderAsEndPortal(Runnable emitSurface) {
        Integer endPortalId = lookupEndPortalBlockEntityId();
        if (endPortalId == null) {
            emitSurface.run();
            return;
        }
        int previousId;
        try {
            previousId = (int) getCurrentBlockEntityMethod.invoke(capturedRenderingState);
            setCurrentBlockEntityMethod.invoke(capturedRenderingState, endPortalId);
        } catch (Exception e) {
            disableBlockEntityIdSpoof(e);
            emitSurface.run();
            return;
        }
        try {
            emitSurface.run();
        } finally {
            restoreBlockEntityId(previousId);
        }
    }

    private static Integer lookupEndPortalBlockEntityId() {
        if (blockEntityIdSpoofAvailable == null) {
            initBlockEntityIdSpoof();
        }
        if (!blockEntityIdSpoofAvailable) return null;
        try {
            @SuppressWarnings("unchecked")
            Object2IntMap<BlockState> ids = (Object2IntMap<BlockState>) getBlockStateIdsMethod.invoke(worldRenderingSettings);
            BlockState endPortal = Blocks.END_PORTAL.defaultBlockState();
            if (ids == null || !ids.containsKey(endPortal)) return null;
            return ids.getInt(endPortal);
        } catch (Exception e) {
            disableBlockEntityIdSpoof(e);
            return null;
        }
    }

    private static void restoreBlockEntityId(int previousId) {
        try {
            setCurrentBlockEntityMethod.invoke(capturedRenderingState, previousId);
        } catch (Exception e) {
            disableBlockEntityIdSpoof(e);
        }
    }

    /**
     * Iris's {@code DimensionId.END} when Iris should run the shader pack's End programs in the current
     * level, else {@code null} (action plan 197, section 2.2). Called from Iris's own
     * {@code getCurrentDimension()}, with the value Iris was about to return.
     *
     * <p>Applies to our own dimensions whose effects are End-like, unless the pack names the dimension
     * in its {@code dimension.properties}: that is Iris 1.8.14's rule, extended to Iris 1.8.8, which
     * only knows the raw dimension id and sends every unlisted dimension to the overworld programs.</p>
     */
    public static Object irisEndDimensionOverride(Object irisChoice) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || irisChoice == null) return null;
        ResourceLocation dimension = level.dimension().location();
        if (!CobbleSafari.MOD_ID.equals(dimension.getNamespace())
                || !END_STYLE_EFFECTS.contains(level.dimensionType().effectsLocation())) {
            return null;
        }
        if (dimensionOverrideAvailable == null) {
            initDimensionOverride();
        }
        if (!dimensionOverrideAvailable) return null;
        try {
            if (irisEndDimension.equals(irisChoice) || packNamesDimension(dimension)) {
                return null;
            }
            return irisEndDimension;
        } catch (Exception e) {
            disableDimensionOverride(e);
            return null;
        }
    }

    private static boolean packNamesDimension(ResourceLocation dimension) throws ReflectiveOperationException {
        Optional<?> pack = (Optional<?>) getCurrentPackMethod.invoke(null);
        if (pack.isEmpty()) return false;
        Object shaderPack = pack.get();
        if (dimensionMapGetter == null && dimensionMapField == null) {
            try {
                // Iris 1.8.14 has a public accessor.
                dimensionMapGetter = shaderPack.getClass().getMethod("getDimensionMap");
            } catch (NoSuchMethodException e) {
                // Iris 1.8.8 only has the private field.
                dimensionMapField = shaderPack.getClass().getDeclaredField("dimensionMap");
                dimensionMapField.setAccessible(true);
            }
        }
        Map<?, ?> dimensionMap = (Map<?, ?>) (dimensionMapGetter != null
                ? dimensionMapGetter.invoke(shaderPack)
                : dimensionMapField.get(shaderPack));
        return dimensionMap != null && dimensionMap.containsKey(
                namespacedIdConstructor.newInstance(dimension.getNamespace(), dimension.getPath()));
    }

    private static void initDimensionOverride() {
        dimensionOverrideAvailable = false;
        try {
            irisEndDimension = Class.forName(DIMENSION_ID_CLASS).getField("END").get(null);
            getCurrentPackMethod = Class.forName(IRIS_CLASS).getMethod("getCurrentPack");
            namespacedIdConstructor = Class.forName(NAMESPACED_ID_CLASS).getConstructor(String.class, String.class);
            dimensionOverrideAvailable = true;
        } catch (Exception e) {
            disableDimensionOverride(e);
        }
    }

    private static void disableDimensionOverride(Exception cause) {
        dimensionOverrideAvailable = false;
        if (dimensionOverrideWarned) return;
        dimensionOverrideWarned = true;
        CobbleSafari.LOGGER.warn("Iris internals unavailable, shader packs will use their overworld programs in CobbleSafari's End-style dimensions: {}", cause.toString());
    }

    private static void initIris() {
        irisLoaded = Services.PLATFORM.isModLoaded("iris");
        if (irisLoaded) {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApiInstance = irisApiClass.getMethod("getInstance").invoke(null);
                isShaderPackInUseMethod = irisApiInstance.getClass().getMethod("isShaderPackInUse");
            } catch (Exception e) {
                irisLoaded = false;
            }
        }
    }

    private static void initBlockEntityIdSpoof() {
        blockEntityIdSpoofAvailable = false;
        if (!Services.PLATFORM.isModLoaded("iris")) return;
        try {
            Class<?> capturedClass = Class.forName(CAPTURED_RENDERING_STATE_CLASS);
            capturedRenderingState = capturedClass.getField("INSTANCE").get(null);
            setCurrentBlockEntityMethod = capturedClass.getMethod("setCurrentBlockEntity", int.class);
            getCurrentBlockEntityMethod = capturedClass.getMethod("getCurrentRenderedBlockEntity");

            Class<?> settingsClass = Class.forName(WORLD_RENDERING_SETTINGS_CLASS);
            worldRenderingSettings = settingsClass.getField("INSTANCE").get(null);
            getBlockStateIdsMethod = settingsClass.getMethod("getBlockStateIds");

            blockEntityIdSpoofAvailable = true;
        } catch (Exception e) {
            disableBlockEntityIdSpoof(e);
        }
    }

    private static void disableBlockEntityIdSpoof(Exception cause) {
        blockEntityIdSpoofAvailable = false;
        if (blockEntityIdSpoofWarned) return;
        blockEntityIdSpoofWarned = true;
        CobbleSafari.LOGGER.warn("Iris internals unavailable, shader packs will not apply their end portal effect to CobbleSafari portals: {}", cause.toString());
    }
}
