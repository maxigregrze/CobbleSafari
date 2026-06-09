package maxigregrze.cobblesafari.client.hud;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.blaze3d.systems.RenderSystem;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.objectives.ObjectivesHudController;
import maxigregrze.cobblesafari.config.HudConfig;
import maxigregrze.cobblesafari.network.ObjectivesHudSyncPayload;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Renders the dimensional objectives HUD (plan 118 §9.1). Texture layout: 256×50 area, three
 * objective rows offset by +17 px each.
 */
public final class ObjectivesHudOverlay {

    private ObjectivesHudOverlay() {}

    private static final String TASK_LANG_PREFIX = "cobblesafari.dimensional_objectives.task.";
    private static final ResourceLocation TEX_MAIN = tex("objectiveshud_main.png");
    private static final ResourceLocation TEX_COUNT = tex("objectiveshud_countcomplete.png");
    private static final ResourceLocation TEX_DOT = tex("objectiveshud_dotcomplete.png");
    private static final ResourceLocation TEX_BAR = tex("objectiveshud_progressbar.png");

    private static final int ROW_OFFSET = 17;
    private static final int BAR_WIDTH = 22;

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/objectives/" + name);
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        if (!ObjectivesHudController.isVisible()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        List<ObjectivesHudSyncPayload.ObjectiveView> objectives = ObjectivesHudController.objectives();
        if (objectives.isEmpty()) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int areaLeft = Math.round(screenWidth - (float) ObjectivesHudController.AREA_WIDTH + ObjectivesHudController.offset());
        int topY = computeTopY(screenHeight);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.blit(TEX_MAIN, areaLeft, topY, 0, 0,
                ObjectivesHudController.AREA_WIDTH, ObjectivesHudController.AREA_HEIGHT,
                ObjectivesHudController.AREA_WIDTH, ObjectivesHudController.AREA_HEIGHT);

        Font font = mc.font;
        for (int i = 0; i < objectives.size() && i < 3; i++) {
            renderObjective(graphics, font, objectives.get(i), areaLeft, topY + i * ROW_OFFSET, i);
        }

        RenderSystem.disableBlend();
    }

    private static void renderObjective(GuiGraphics graphics, Font font,
                                        ObjectivesHudSyncPayload.ObjectiveView o,
                                        int areaLeft, int rowTop, int index) {
        if (o.complete()) {
            graphics.blit(TEX_COUNT, areaLeft + 6, rowTop + 1, 0, 0, 22, 12, 22, 12);
        }

        int barWidth = o.complete()
                ? BAR_WIDTH
                : (o.targetCount() <= 0 ? 0 : Math.round((float) BAR_WIDTH * o.progress() / o.targetCount()));
        if (barWidth > 0) {
            graphics.blit(TEX_BAR, areaLeft + 6, rowTop + 11, 0, 0, barWidth, 4, BAR_WIDTH, 4);
        }

        if (ObjectivesHudController.notifActive(index)) {
            graphics.blit(TEX_DOT, areaLeft + 2, rowTop + 8, 0, 0, 3, 3, 3, 3);
        }

        String progressText = o.progress() + "/" + o.targetCount();
        graphics.drawString(font, progressText, areaLeft + 8, rowTop + 3, 0xFFFFFF, true);

        graphics.drawString(font, formatTaskText(o), areaLeft + 30, rowTop + 3, 0xFFFFFF, true);
    }

    public static Component formatTaskText(ObjectivesHudSyncPayload.ObjectiveView o) {
        Component name = Component.empty();
        if (o.typeIndex() >= 0) {
            name = Component.translatable("cobblemon.type." + PowerVariantRegistry.suffix(o.typeIndex()));
        } else if (!o.speciesId().isEmpty()) {
            name = speciesName(o.speciesId());
        }
        return Component.translatable(TASK_LANG_PREFIX + o.taskId(), o.targetCount(), name);
    }

    private static Component speciesName(String speciesId) {
        ResourceLocation id = ResourceLocation.tryParse(speciesId);
        if (id != null) {
            Species species = PokemonSpecies.getByIdentifier(id);
            if (species != null) {
                return species.getTranslatedName();
            }
        }
        return Component.literal(speciesId);
    }

    private static int computeTopY(int screenHeight) {
        int travel = screenHeight - ObjectivesHudController.AREA_HEIGHT;
        if (HudConfig.isObjectivesUsePercentage()) {
            return Math.round(HudConfig.getObjectivesPlacementPercentage() * travel);
        }
        return travel / 2;
    }
}
