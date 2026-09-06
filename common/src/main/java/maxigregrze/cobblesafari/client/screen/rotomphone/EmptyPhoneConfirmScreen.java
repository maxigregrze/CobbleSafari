package maxigregrze.cobblesafari.client.screen.rotomphone;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.gui.ProfileTransformType;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.network.EmptyPhoneConfirmPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;

/**
 * Confirmation shown before a Rotom is absorbed into an empty phone or earpiece. Built on the same
 * frame as {@link maxigregrze.cobblesafari.client.screen.TpAcceptScreen}: titled 272x184 top, one or
 * two 272x22 banners, then two 136x24 buttons. The destination artwork is replaced by the live model
 * of the Rotom being consumed - the aspect set carries both its shiny flag and its alternate form.
 */
public class EmptyPhoneConfirmScreen extends Screen {

    /** Matches Cobblemon's own {@code drawProfilePokemon} default: flat, well-lit inventory rendering. */
    private static final int BLOCK_LIGHT = 13;
    private static final int GUI_WIDTH = 272;
    private static final int TOP_HEIGHT = 184;
    private static final int BANNER_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 136;
    private static final int BUTTON_HEIGHT = 24;

    private static final int MODEL_X = 78;
    private static final int MODEL_Y = 40;
    private static final int MODEL_W = 116;
    private static final int MODEL_H = 128;
    /**
     * Extra clip width revealed on each side of the window, so the zoomed model is not cut off left
     * and right. Only widens the scissor - the model's centre stays on the GUI centre.
     */
    private static final int MODEL_CLIP_PAD_X = 40;
    /** Zoom applied on top of the Wonder Trade portrait's settings, which render a Rotom rather small. */
    private static final float MODEL_ZOOM = 1.8f;
    /**
     * Rendered size is {@code MODEL_BASE_SCALE * MODEL_SCALE}, so the zoom goes on one factor only -
     * multiplying both would square it.
     */
    private static final float MODEL_BASE_SCALE = 10.5f * MODEL_ZOOM;
    private static final float MODEL_SCALE = 6f;
    /** Unzoomed anchor: 1 as on the Wonder Trade portrait, +7 to centre it in this taller window. */
    private static final float MODEL_Y_ANCHOR_UNZOOMED = 8f;
    /**
     * Observed correction applied on top of the derived anchor, in plain screen pixels: negative
     * moves the Rotom up. Kept separate from the zoom compensation so it stays a pixel offset
     * whatever {@link #MODEL_ZOOM} becomes.
     */
    private static final float MODEL_Y_NUDGE = -20f;
    /**
     * The model hangs below its anchor by a distance proportional to the scale, so zooming in has to
     * pull the anchor up by the same factor to keep the Rotom centred instead of pushing it out of
     * the bottom of the window. Tune {@link #MODEL_ZOOM}, {@link #MODEL_Y_ANCHOR_UNZOOMED} or
     * {@link #MODEL_Y_NUDGE}, not this.
     */
    private static final float MODEL_Y_ANCHOR =
            MODEL_H / 2f - MODEL_ZOOM * (MODEL_H / 2f - MODEL_Y_ANCHOR_UNZOOMED) + MODEL_Y_NUDGE;

    private static final ResourceLocation TOP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/emptyphone/gui_emptyphone_top.png");
    private static final ResourceLocation BANNER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/emptyphone/gui_emptyphone_banner.png");
    private static final ResourceLocation ACCEPT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/emptyphone/gui_emptyphone_btn_accept.png");
    private static final ResourceLocation DENY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/emptyphone/gui_emptyphone_btn_deny.png");

    private final String rotomName;
    private final int rotomLevel;
    private final boolean rotomIsShiny;
    private final RenderablePokemon renderable;

    private int guiLeft;
    private int guiTop;
    private FloatingState floatingState;
    private boolean responded = false;

    public EmptyPhoneConfirmScreen(String rotomName, int rotomLevel, boolean rotomIsShiny,
                                   String speciesId, List<String> aspects) {
        super(Component.translatable("gui.cobblesafari.rotomphone.empty_confirm.title"));
        this.rotomName = rotomName;
        this.rotomLevel = rotomLevel;
        this.rotomIsShiny = rotomIsShiny;
        this.renderable = resolve(speciesId, aspects);
    }

    /** Builds the renderable straight from the payload, so no party synchronisation is involved. */
    private static RenderablePokemon resolve(String speciesId, List<String> aspects) {
        try {
            Species species = PokemonSpecies.INSTANCE.getByIdentifier(ResourceLocation.parse(speciesId));
            if (species != null) {
                return new RenderablePokemon(species, Set.copyOf(aspects), ItemStack.EMPTY);
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("Could not resolve species '{}' for the empty phone screen", speciesId, e);
        }
        return null;
    }

    /** One banner for the question; two when the Rotom is shiny (question + warning). */
    private int bannerCount() {
        return rotomIsShiny ? 2 : 1;
    }

    private int buttonsY() {
        return guiTop + TOP_HEIGHT + BANNER_HEIGHT * bannerCount();
    }

    @Override
    protected void init() {
        super.init();
        int totalHeight = TOP_HEIGHT + BANNER_HEIGHT * bannerCount() + BUTTON_HEIGHT;
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - totalHeight) / 2;
        this.floatingState = new FloatingState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.blit(TOP_TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, TOP_HEIGHT, GUI_WIDTH, TOP_HEIGHT);

        int titleWidth = this.font.width(this.title);
        graphics.drawString(this.font, this.title, guiLeft + GUI_WIDTH / 2 - titleWidth / 2,
                guiTop + 8, 0xFFFFFF, true);

        if (renderable != null) {
            drawRotom(graphics, partialTick);
        }

        int firstBannerY = guiTop + TOP_HEIGHT;
        Component desc = Component.translatable("gui.cobblesafari.rotomphone.empty_confirm.desc",
                rotomName, rotomLevel);
        drawBanner(graphics, firstBannerY, desc, 0xFFFFFF);

        if (rotomIsShiny) {
            Component shinyText = Component.translatable("gui.cobblesafari.rotomphone.empty_confirm.shiny");
            drawBanner(graphics, firstBannerY + BANNER_HEIGHT, shinyText, 0xFFFFC0);
        }

        int buttonsY = buttonsY();
        boolean confirmHovered = isInBounds(mouseX, mouseY, guiLeft, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean cancelHovered = isInBounds(mouseX, mouseY, guiLeft + BUTTON_WIDTH, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT);

        graphics.blit(ACCEPT_TEXTURE, guiLeft, buttonsY, 0, confirmHovered ? BUTTON_HEIGHT : 0,
                BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT * 2);
        graphics.blit(DENY_TEXTURE, guiLeft + BUTTON_WIDTH, buttonsY, 0, cancelHovered ? BUTTON_HEIGHT : 0,
                BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT * 2);

        drawButtonLabel(graphics, Component.translatable("gui.cobblesafari.rotomphone.confirm"),
                guiLeft, buttonsY);
        drawButtonLabel(graphics, Component.translatable("gui.cobblesafari.rotomphone.cancel"),
                guiLeft + BUTTON_WIDTH, buttonsY);
    }

    private void drawBanner(GuiGraphics graphics, int y, Component text, int color) {
        graphics.blit(BANNER_TEXTURE, guiLeft, y, 0, 0, GUI_WIDTH, BANNER_HEIGHT, GUI_WIDTH, BANNER_HEIGHT);
        int textWidth = this.font.width(text);
        graphics.drawString(this.font, text, guiLeft + GUI_WIDTH / 2 - textWidth / 2,
                y + 2 + (BANNER_HEIGHT - 8) / 2, color, true);
    }

    private void drawButtonLabel(GuiGraphics graphics, Component text, int x, int y) {
        int textWidth = this.font.width(text);
        graphics.drawString(this.font, text, x + BUTTON_WIDTH / 2 - textWidth / 2,
                y + (BUTTON_HEIGHT - 8) / 2, 0xFFFFFF, true);
    }

    /** Renders the target Rotom in the frame's window, on the Wonder Trade portrait's settings. */
    private void drawRotom(GuiGraphics graphics, float partialTick) {
        int x = guiLeft + MODEL_X;
        int y = guiTop + MODEL_Y;
        graphics.enableScissor(x - MODEL_CLIP_PAD_X, y, x + MODEL_W + MODEL_CLIP_PAD_X, y + MODEL_H);
        graphics.pose().pushPose();
        graphics.pose().translate(x + MODEL_W * 0.5, y + MODEL_Y_ANCHOR, 0.0);
        graphics.pose().scale(MODEL_BASE_SCALE, MODEL_BASE_SCALE, MODEL_BASE_SCALE);
        Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(
                new Quaternionf(), new Vector3f(13f, 35f, 0f));
        floatingState.setCurrentAspects(renderable.getAspects());
        // Species-id overload rather than the RenderablePokemon one: it is the only one exposing
        // doQuirks, which is what makes the Rotom blink and fidget instead of merely looping its pose.
        PokemonGuiUtilsKt.drawProfilePokemon(
                renderable.getSpecies().getResourceIdentifier(),
                graphics.pose(),
                rotation,
                PoseType.PROFILE,
                floatingState,
                partialTick,
                MODEL_SCALE,
                ProfileTransformType.PROFILE,
                false,
                true,
                1f,
                1f,
                1f,
                1f,
                0f,
                0f,
                BLOCK_LIGHT);
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int buttonsY = buttonsY();
            if (isInBounds((int) mouseX, (int) mouseY, guiLeft, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                respond(true);
                this.onClose();
                return true;
            }
            if (isInBounds((int) mouseX, (int) mouseY, guiLeft + BUTTON_WIDTH, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                respond(false);
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void respond(boolean confirmed) {
        if (responded) {
            return;
        }
        responded = true;
        Services.PLATFORM.sendPayloadToServer(new EmptyPhoneConfirmPayload(confirmed));
    }

    @Override
    public void onClose() {
        // Escape or any other close path must release the server-side pending fill - and with it the
        // claim held on the block.
        respond(false);
        super.onClose();
    }

    /** Called from the server-driven close payload: the pending fill already expired, stay silent. */
    public void closeFromServer() {
        this.responded = true;
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isInBounds(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
