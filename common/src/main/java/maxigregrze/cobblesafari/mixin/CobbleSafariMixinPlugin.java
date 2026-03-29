package maxigregrze.cobblesafari.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class CobbleSafariMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogManager.getLogger("CobbleSafari");

    private static final Set<String> JIGSAW_MIXIN_CLASSES = Set.of(
            "maxigregrze.cobblesafari.mixin.JigsawPlacementMixin",
            "maxigregrze.cobblesafari.mixin.JigsawStructureMixin"
    );

    private static final String[] CONFLICTING_MOD_CLASSES = {
            "com.convallyria.hugestructureblocks.HugeStructureBlocksMod"
    };

    private boolean jigsawMixinsEnabled = true;

    @Override
    public void onLoad(String mixinPackage) {
        for (String className : CONFLICTING_MOD_CLASSES) {
            if (isClassPresent(className)) {
                jigsawMixinsEnabled = false;
                LOGGER.info("Detected mod providing jigsaw unlimiting ({}), disabling CobbleSafari jigsaw mixins to avoid conflicts", className);
                break;
            }
        }
        if (jigsawMixinsEnabled) {
            LOGGER.info("No conflicting jigsaw mod detected, CobbleSafari jigsaw mixins will be applied");
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, CobbleSafariMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (JIGSAW_MIXIN_CLASSES.contains(mixinClassName)) {
            return jigsawMixinsEnabled;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // required by IMixinConfigPlugin
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // required by IMixinConfigPlugin
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // required by IMixinConfigPlugin
    }
}
