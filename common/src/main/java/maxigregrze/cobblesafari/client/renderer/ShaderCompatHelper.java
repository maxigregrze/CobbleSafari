package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.platform.Services;

import java.lang.reflect.Method;

public final class ShaderCompatHelper {

    private static Boolean irisLoaded;
    private static Method isShaderPackInUseMethod;
    private static Object irisApiInstance;

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
}
