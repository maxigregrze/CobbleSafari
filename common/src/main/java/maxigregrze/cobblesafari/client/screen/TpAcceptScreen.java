package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.network.TpAcceptResponsePayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class TpAcceptScreen extends Screen {

    private static final int GUI_WIDTH = 272;
    private static final int GUI_HEIGHT_NO_FEE = 208;
    private static final int GUI_HEIGHT_WITH_FEE = 230;
    private static final int TOP_HEIGHT = 184;
    private static final int BANNER_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 136;
    private static final int BUTTON_HEIGHT = 24;
    private static final int LOGO_WIDTH = 116;
    private static final int LOGO_HEIGHT = 128;
    private static final int LOADING_HEIGHT = 24;

    private static final ResourceLocation TOP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/tpaccept/gui_tpaccept_top.png");
    private static final ResourceLocation ACCEPT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/tpaccept/gui_tpaccept_btn_accept.png");
    private static final ResourceLocation DENY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/tpaccept/gui_tpaccept_btn_deny.png");
    private static final ResourceLocation BANNER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/tpaccept/gui_tpaccept_pricebanner.png");
    private static final ResourceLocation LOADING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/tpaccept/gui_tpaccept_loading.png");

    public record FeeInfo(boolean hasEntryFee, boolean isCobbledollarFee,
                          int entryFeeAmount, String entryFeeItem, boolean alreadyPaidToday) {}

    private final String dimensionName;
    private final String dimensionId;
    private final boolean hasEntryFee;
    private final boolean isCobbledollarFee;
    private final int entryFeeAmount;
    private final String entryFeeItem;
    private final String source;
    private final boolean alreadyPaidToday;
    private final ResourceLocation logoTexture;

    private int guiLeft;
    private int guiTop;
    private int totalHeight;

    private boolean acceptHovered;
    private boolean denyHovered;
    private boolean loading = false;

    public TpAcceptScreen(String dimensionName, String dimensionId,
                          FeeInfo fee, String source) {
        this(dimensionName, dimensionId, fee.hasEntryFee(), fee.isCobbledollarFee(),
                fee.entryFeeAmount(), fee.entryFeeItem(), source, fee.alreadyPaidToday());
    }

    public TpAcceptScreen(String dimensionName, String dimensionId, boolean hasEntryFee,
                          boolean isCobbledollarFee, int entryFeeAmount, String entryFeeItem,
                          String source, boolean alreadyPaidToday) {
        super(Component.translatable("gui.cobblesafari.tpaccept.title"));
        this.dimensionName = dimensionName;
        this.dimensionId = dimensionId;
        this.hasEntryFee = hasEntryFee;
        this.isCobbledollarFee = isCobbledollarFee;
        this.entryFeeAmount = entryFeeAmount;
        this.entryFeeItem = entryFeeItem;
        this.source = source;
        this.alreadyPaidToday = alreadyPaidToday;
        this.logoTexture = ResourceLocation.fromNamespaceAndPath(
                CobbleSafari.MOD_ID,
                "textures/gui/tpaccept/gui_tpaccept_destination_" + dimensionId + ".png"
        );
    }

    @Override
    protected void init() {
        super.init();
        this.totalHeight = hasEntryFee ? GUI_HEIGHT_WITH_FEE : GUI_HEIGHT_NO_FEE;
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - totalHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int buttonsY = hasEntryFee ? guiTop + TOP_HEIGHT + BANNER_HEIGHT : guiTop + TOP_HEIGHT;

        graphics.blit(TOP_TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, TOP_HEIGHT, GUI_WIDTH, TOP_HEIGHT);

        Component titleText = Component.translatable("gui.cobblesafari.tpaccept.teleporting_to", dimensionName);
        int titleWidth = this.font.width(titleText);
        graphics.drawString(this.font, titleText, guiLeft + 136 - titleWidth / 2, guiTop + 8, 0xFFFFFF, true);

        graphics.blit(logoTexture, guiLeft + 78, guiTop + 40, 0, 0, LOGO_WIDTH, LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);

        if (hasEntryFee) {
            graphics.blit(BANNER_TEXTURE, guiLeft, guiTop + TOP_HEIGHT, 0, 0, GUI_WIDTH, BANNER_HEIGHT, GUI_WIDTH, BANNER_HEIGHT);

            Component feeText;
            if (alreadyPaidToday) {
                feeText = Component.translatable("gui.cobblesafari.tpaccept.already_paid");
            } else if (isCobbledollarFee) {
                feeText = Component.translatable("gui.cobblesafari.tpaccept.entry_fee", entryFeeAmount);
            } else {
                Component itemName = getItemDisplayName(entryFeeItem);
                feeText = Component.translatable("gui.cobblesafari.tpaccept.entry_fee_item", 1, itemName);
            }
            int feeWidth = this.font.width(feeText);
            graphics.drawString(this.font, feeText, guiLeft + 136 - feeWidth / 2, guiTop + TOP_HEIGHT + 2 + (BANNER_HEIGHT - 8) / 2, 0xFFFFFF, true);
        }

        if (loading) {
            graphics.blit(LOADING_TEXTURE, guiLeft, buttonsY, 0, 0, GUI_WIDTH, LOADING_HEIGHT, GUI_WIDTH, LOADING_HEIGHT);

            Component loadingText = Component.translatable("gui.cobblesafari.tpaccept.loading");
            int loadingWidth = this.font.width(loadingText);
            graphics.drawString(this.font, loadingText,
                    guiLeft + GUI_WIDTH / 2 - loadingWidth / 2,
                    buttonsY + (LOADING_HEIGHT - 8) / 2,
                    0xFFFFFF, true);
        } else {
            acceptHovered = isInBounds(mouseX, mouseY, guiLeft, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT);
            denyHovered = isInBounds(mouseX, mouseY, guiLeft + BUTTON_WIDTH, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT);

            int acceptV = acceptHovered ? BUTTON_HEIGHT : 0;
            graphics.blit(ACCEPT_TEXTURE, guiLeft, buttonsY, 0, acceptV, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT * 2);

            int denyV = denyHovered ? BUTTON_HEIGHT : 0;
            graphics.blit(DENY_TEXTURE, guiLeft + BUTTON_WIDTH, buttonsY, 0, denyV, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT * 2);

            Component acceptText = Component.translatable("gui.cobblesafari.tpaccept.accept");
            int acceptWidth = this.font.width(acceptText);
            graphics.drawString(this.font, acceptText,
                    guiLeft + BUTTON_WIDTH / 2 - acceptWidth / 2,
                    buttonsY + (BUTTON_HEIGHT - 8) / 2,
                    0xFFFFFF, true);

            Component denyText = Component.translatable("gui.cobblesafari.tpaccept.cancel");
            int denyWidth = this.font.width(denyText);
            graphics.drawString(this.font, denyText,
                    guiLeft + BUTTON_WIDTH + BUTTON_WIDTH / 2 - denyWidth / 2,
                    buttonsY + (BUTTON_HEIGHT - 8) / 2,
                    0xFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (loading) {
            return false;
        }

        if (button == 0) {
            int buttonsY = hasEntryFee ? guiTop + TOP_HEIGHT + BANNER_HEIGHT : guiTop + TOP_HEIGHT;

            if (isInBounds((int) mouseX, (int) mouseY, guiLeft, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                loading = true;
                Services.PLATFORM.sendPayloadToServer(new TpAcceptResponsePayload(true, source));
                return true;
            }

            if (isInBounds((int) mouseX, (int) mouseY, guiLeft + BUTTON_WIDTH, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                Services.PLATFORM.sendPayloadToServer(new TpAcceptResponsePayload(false, source));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !loading;
    }

    @Override
    public void onClose() {
        if (!loading) {
            Services.PLATFORM.sendPayloadToServer(new TpAcceptResponsePayload(false, source));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void closeFromServer() {
        this.loading = false;
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private static boolean isInBounds(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static Component getItemDisplayName(String itemId) {
        try {
            ResourceLocation location = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(location);
            if (item != Items.AIR) {
                return item.getDescription();
            }
        } catch (Exception e) {
        }
        return Component.literal(itemId);
    }
}
