package maxigregrze.cobblesafari.item.donut;

import java.util.function.BooleanSupplier;

public final class DonutDebugClientAccess {

    private static BooleanSupplier shiftDown = () -> false;

    private DonutDebugClientAccess() {}

    public static void setShiftKeyDownSupplier(BooleanSupplier supplier) {
        shiftDown = supplier;
    }

    public static boolean isShiftDown() {
        return shiftDown.getAsBoolean();
    }
}
