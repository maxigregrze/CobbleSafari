package maxigregrze.cobblesafari.client.renderer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;

public final class ShaderCompatHelper {

    private static final String CAPTURED_RENDERING_STATE_CLASS = "net.irisshaders.iris.uniforms.CapturedRenderingState";
    private static final String WORLD_RENDERING_SETTINGS_CLASS = "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings";

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
