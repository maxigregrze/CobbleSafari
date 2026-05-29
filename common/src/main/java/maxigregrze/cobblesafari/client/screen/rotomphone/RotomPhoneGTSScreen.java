package maxigregrze.cobblesafari.client.screen.rotomphone;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.gts.GenderFilter;
import maxigregrze.cobblesafari.gts.GtsOffer;
import maxigregrze.cobblesafari.gts.GtsService;
import maxigregrze.cobblesafari.network.GtsAppPayload;
import maxigregrze.cobblesafari.network.GtsAppResultPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RotomPhoneGTSScreen extends RotomPhoneBaseScreen {

    private enum SubScreen {
        LOADING,
        BEGIN,
        SELECT,
        PARAMETERS,
        CONFIRM,
        DEPOSIT_ANIM,
        RETRIEVAL_ANIM,
        RECEIVE_ANIM,
        SEEK,
        CHECK,
        TRADE_ANIM,
        ERROR
    }

    private static final String GENDER_GENDERLESS = "cobblemon.gender.genderless";
    private static final String GENDER_FEMALE = "cobblemon.gender.female";
    private static final String GENDER_MALE = "cobblemon.gender.male";
    private static final String TT_SHINY_YES = "gui.cobblesafari.rotomphone.wonder.tt.shiny.yes";
    private static final String TT_SHINY_NO = "gui.cobblesafari.rotomphone.wonder.tt.shiny.no";
    private static final String TT_LEVEL = "gui.cobblesafari.rotomphone.wonder.tt.level";
    private static final String GTS_LEVEL_PREFIX = "gui.cobblesafari.rotomphone.gts.level.";
    private static final String GTS_EXIT = "gui.cobblesafari.rotomphone.gts.exit";
    private static final String STATUS_SUCCESS = "SUCCESS";

    /** Begin hub layout modes (retrieve confirmation is local UI). */
    private enum BeginBeginMode {
        NO_OFFER,
        HAS_OFFER,
        HAS_SUCCESS,
        CONFIRM_RETRIEVE
    }

    private static final ResourceLocation TEX_GTS_LOGO = loc("gts/rotomphone_gui_icon_gts.png");
    private static final ResourceLocation TEX_DOUBLE = loc("rotomphone_gui_icon_double.png");
    private static final ResourceLocation TEX_RIGHT = loc("rotomphone_gui_icon_right.png");
    private static final ResourceLocation TEX_LEFT = loc("rotomphone_gui_icon_left.png");
    private static final ResourceLocation TEX_EMPTY = loc("rotomphone_gui_icon_empty.png");
    private static final ResourceLocation TEX_TRADEANIM = loc("wonder/rotomphone_gui_tradeanim.png");
    private static final ResourceLocation TEX_TRADE = loc("gts/rotomphone_gui_icon_trade.png");
    private static final ResourceLocation TEX_VALID = loc("gts/rotomphone_gui_icon_valid.png");
    private static final ResourceLocation TEX_INVALID = loc("gts/rotomphone_gui_icon_invalid.png");

    private static final float SCALED_TEXT_Y_OFFSET = -7f;
    private static final int PARTY_SLOT_SIZE = 32;
    private static final float PARTY_SLOT_BASE_SCALE = 3.5f;
    private static final float PARTY_SLOT_MODEL_SCALE = 5.5f;
    private static final float PARTY_SLOT_MODEL_Y_OFFSET = 4f;
    private static final float TRADE_SLOT_BASE_SCALE = 10.5f;
    private static final float TRADE_SLOT_MODEL_SCALE = 6f;
    private static final int TRADE_SLOT_SIZE = 114;
    private static final int TRADE_SLOT_CLIP_WIDTH = TRADE_SLOT_SIZE * 2;
    private static final int TRADE_SLOT_X = 117;
    private static final int TRADE_SLOT_Y = 11;
    private static final int ANIM_FRAME_COUNT = 14;
    private static final int ANIM_FRAME_MS = 100;
    private static final long ANIM_INITIAL_DELAY_MS = 800L;
    private static final long ANIM_PAUSE_MS = 500L;
    private static final long ANIM_FORWARD_MS = (long) ANIM_FRAME_COUNT * ANIM_FRAME_MS;
    private static final long ANIM_TRADE_TOTAL_MS = ANIM_INITIAL_DELAY_MS
            + ANIM_FORWARD_MS
            + ANIM_PAUSE_MS
            + ANIM_FORWARD_MS;
    private static final long ANIM_HALF_TOTAL_MS = ANIM_INITIAL_DELAY_MS + ANIM_FORWARD_MS;

    private static final int CONFIRM_SLOT_SIZE = 72;
    private static final int CONFIRM_SLOT_CLIP_WIDTH = CONFIRM_SLOT_SIZE * 2;
    private static final float CONFIRM_SLOT_BASE_SCALE = 8.5f;
    private static final float CONFIRM_SLOT_MODEL_SCALE = 6.2f;
    /** Screen-pixel lift for confirm slots; negative moves the model up. */
    private static final float CONFIRM_SLOT_MODEL_Y_OFFSET = -20f;
    private static final long TRADE_ICON_ROTATION_PERIOD_MS = 4000L;
    private static final int ANIM_DISAPPEAR_FRAME = 4;
    private static final int ANIM_APPEAR_FORWARD_INDEX = 10;
    private static final int ANIM_RECEIVED_VISIBLE_FROM_END = 4;
    private static final float ANIM_MIN_POKEMON_SCALE = 0.5f;

    private static final int PARAM_SUGGESTION_X = 59;
    private static final int PARAM_SUGGESTION_WIDTH = 230;
    private static final int SEEK_SUGGESTION_X = 99;
    private static final int SEEK_SUGGESTION_WIDTH = 150;
    /** Dropdown top Y = species box bottom + this offset (4px above prior +2). */
    private static final int SUGGESTION_Y_BELOW_BOX = -2;

    private static final int CONFIRM_ACTION_Y = 136;
    private static final int HALF_ANIM_EXIT_Y = 136;
    private static final int BEGIN_CONFIRM_BUTTON_Y = 96;

    private static final int[] PARTY_SLOT_X = {58, 98, 138, 178, 218, 258};
    /** First row of seek results (6 slots). */
    private static final int[] SEEK_SLOT_ROW1_X = {58, 98, 138, 178, 218, 258};
    /** Second row (4 slots). */
    private static final int[] SEEK_SLOT_ROW2_X = {98, 138, 178, 218};
    private static final int SEEK_ROW1_Y = 96;
    private static final int SEEK_ROW2_Y = 136;

    private static final long SEEK_DEBOUNCE_MS = 500L;

    private static final Stat[] TOOLTIP_STATS = {
            Stats.HP,
            Stats.ATTACK,
            Stats.DEFENCE,
            Stats.SPECIAL_ATTACK,
            Stats.SPECIAL_DEFENCE,
            Stats.SPEED
    };
    private static final String[] TOOLTIP_STAT_KEYS = {"hp", "atk", "def", "spatk", "spdef", "spd"};

    private SubScreen state = SubScreen.LOADING;
    private BeginBeginMode beginMode = BeginBeginMode.NO_OFFER;

    private int offerCount;
    private int ownActiveOfferId = -1;
    private int successCount;
    private int oldestSuccessId = -1;

    private int selectedSlot = -1;

    private EditBox paramSpeciesBox;
    private EditBox seekSpeciesBox;
    private boolean paramSpeciesChecked;
    private boolean paramSpeciesInvalid;
    private GenderFilter paramGender = GenderFilter.ANY;
    private GtsOffer.ShinyWish paramShiny = GtsOffer.ShinyWish.ANY;
    private int paramLevelBucket = -1;
    private List<String> paramSuggestions = List.of();
    private int paramSuggestionHover = -1;

    private boolean confirmLocked;
    private long confirmLockedAtMillis;
    private boolean confirmPending;
    private boolean retrievePending;
    private boolean claimPending;

    private Pokemon offeredAnimPokemon;
    private Pokemon receivedAnimPokemon;
    private final FloatingState[] partyStates = new FloatingState[6];
    private FloatingState offeredTradeState;
    private FloatingState receivedTradeState;
    private FloatingState offeredWishState;
    private final FloatingState[] seekSlotStates = new FloatingState[10];
    private final Map<Integer, Pokemon> seekPokemonCache = new HashMap<>();

    private long tradeStartedAtMillis;

    private String lastErrorKey = "";

    private long seekLastTypedAt;
    private boolean seekDirty;
    private GenderFilter seekGender = GenderFilter.ANY;
    private GtsOffer.ShinyWish seekShiny = GtsOffer.ShinyWish.ANY;
    private int seekPage = 1;
    private int seekTotalPages = 1;
    private List<GtsAppResultPayload.SearchEntry> seekResults = List.of();
    private List<String> seekSuggestions = List.of();
    private int seekSuggestionHover = -1;

    private GtsAppResultPayload.SearchEntry checkOffer;
    private boolean checkLaunched;
    private boolean checkNoMatch;
    private List<CompoundTag> checkCandidates = List.of();
    private int matchIndex;
    private boolean matchLocked;
    private long matchLockedAtMillis;
    private boolean tradeConfirmPending;

    public RotomPhoneGTSScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode, boolean rotoGlide) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.gts"), rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide);
    }

    private RotomPhoneGTSScreen(RotomPhoneShell shell) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.gts"), "", false, "", false, false, shell);
    }

    public static RotomPhoneGTSScreen forOnlinePc() {
        return new RotomPhoneGTSScreen(RotomPhoneShell.ONLINE_PC);
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 6; i++) {
            partyStates[i] = new FloatingState();
        }
        for (int i = 0; i < 10; i++) {
            seekSlotStates[i] = new FloatingState();
        }

        int pbx = originX + 60;
        int pby = originY + 58;
        paramSpeciesBox = new EditBox(this.font, pbx, pby, 226, 28, Component.empty());
        paramSpeciesBox.setMaxLength(64);
        paramSpeciesBox.setBordered(true);
        paramSpeciesBox.setResponder(s -> {
            if (paramSpeciesChecked) {
                paramSpeciesChecked = false;
            }
            paramSuggestions = SpeciesAutocompleteHelper.suggest(s, 5);
        });

        int sbx = originX + 100;
        int sby = originY + 58;
        seekSpeciesBox = new EditBox(this.font, sbx, sby, 144, 28, Component.empty());
        seekSpeciesBox.setMaxLength(64);
        seekSpeciesBox.setBordered(true);
        seekSpeciesBox.setResponder(s -> {
            seekLastTypedAt = System.currentTimeMillis();
            seekDirty = true;
            seekSuggestions = SpeciesAutocompleteHelper.suggest(s, 5);
        });

        Services.PLATFORM.sendPayloadToServer(new GtsAppPayload(GtsAppPayload.ACTION_REQUEST_STATE, 0, 0, "", "", ""));
    }

    public void applyServerSnapshot(GtsAppResultPayload p) {
        if (payloadCarriesBeginSnapshot(p.subscreen())) {
            offerCount = p.offerCount();
            ownActiveOfferId = p.ownActiveOfferId();
            successCount = p.successCount();
            oldestSuccessId = p.oldestSuccessId();
        }

        switch (p.subscreen()) {
            case GtsAppResultPayload.SUB_BEGIN -> {
                lastErrorKey = "";
                refreshBeginModeFromServer();
                if (state == SubScreen.LOADING || state == SubScreen.BEGIN || state == SubScreen.ERROR) {
                    state = SubScreen.BEGIN;
                }
            }
            case GtsAppResultPayload.SUB_VALIDATE_RESULT -> {
                if (state != SubScreen.PARAMETERS) {
                    break;
                }
                applyParamValidateResult(p.validateResult());
            }
            case GtsAppResultPayload.SUB_DEPOSIT -> {
                confirmPending = false;
                if (STATUS_SUCCESS.equals(p.operationResult())) {
                    offeredAnimPokemon = loadPokemon(p.offeredNbt());
                    offeredTradeState = new FloatingState();
                    tradeStartedAtMillis = System.currentTimeMillis();
                    state = SubScreen.DEPOSIT_ANIM;
                } else {
                    confirmLocked = false;
                    state = SubScreen.BEGIN;
                    lastErrorKey = "gui.cobblesafari.rotomphone.gts.error.error";
                }
            }
            case GtsAppResultPayload.SUB_RETRIEVAL -> {
                retrievePending = false;
                beginMode = BeginBeginMode.NO_OFFER;
                if (STATUS_SUCCESS.equals(p.operationResult())) {
                    offeredAnimPokemon = loadPokemon(p.offeredNbt());
                    offeredTradeState = new FloatingState();
                    tradeStartedAtMillis = System.currentTimeMillis();
                    state = SubScreen.RETRIEVAL_ANIM;
                } else {
                    state = SubScreen.BEGIN;
                }
            }
            case GtsAppResultPayload.SUB_RECEIVE -> {
                claimPending = false;
                if (STATUS_SUCCESS.equals(p.operationResult())) {
                    receivedAnimPokemon = loadPokemon(p.receivedNbt());
                    receivedTradeState = new FloatingState();
                    tradeStartedAtMillis = System.currentTimeMillis();
                    state = SubScreen.RECEIVE_ANIM;
                } else {
                    state = SubScreen.BEGIN;
                }
            }
            case GtsAppResultPayload.SUB_SEARCH_RESULT -> {
                seekPage = p.searchPage();
                seekTotalPages = Math.max(1, p.searchTotalPages());
                seekResults = p.searchEntries() == null ? List.of() : p.searchEntries();
                seekPokemonCache.clear();
            }
            case GtsAppResultPayload.SUB_START_TRADE_RESULT -> {
                if (state != SubScreen.CHECK || !checkLaunched) {
                    break;
                }
                checkNoMatch = false;
                if ("OK".equals(p.startTradeKind())) {
                    List<CompoundTag> c = p.candidateNbts() == null ? List.of() : p.candidateNbts();
                    checkCandidates = c;
                    if (c.isEmpty()) {
                        checkNoMatch = true;
                    } else {
                        matchIndex = 0;
                    }
                }
            }
            case GtsAppResultPayload.SUB_TRADE -> {
                tradeConfirmPending = false;
                matchLocked = false;
                offeredAnimPokemon = loadPokemon(p.offeredNbt());
                receivedAnimPokemon = loadPokemon(p.receivedNbt());
                offeredTradeState = new FloatingState();
                receivedTradeState = new FloatingState();
                tradeStartedAtMillis = System.currentTimeMillis();
                state = SubScreen.TRADE_ANIM;
            }
            case GtsAppResultPayload.SUB_ERROR -> {
                confirmPending = false;
                retrievePending = false;
                claimPending = false;
                tradeConfirmPending = false;
                resetCheckScreen();
                lastErrorKey = p.errorKey() == null ? "" : p.errorKey();
                if (beginMode == BeginBeginMode.CONFIRM_RETRIEVE) {
                    beginMode = BeginBeginMode.HAS_OFFER;
                }
                state = SubScreen.ERROR;
            }
            default -> {
            }
        }
    }

    /** Only these subscreens populate hub counters; others use 0 as an unused placeholder. */
    private static boolean payloadCarriesBeginSnapshot(int subscreen) {
        return subscreen == GtsAppResultPayload.SUB_BEGIN
                || subscreen == GtsAppResultPayload.SUB_DEPOSIT
                || subscreen == GtsAppResultPayload.SUB_RETRIEVAL
                || subscreen == GtsAppResultPayload.SUB_ERROR;
    }

    private void refreshBeginModeFromServer() {
        if (beginMode == BeginBeginMode.CONFIRM_RETRIEVE) {
            return;
        }
        if (successCount > 0) {
            beginMode = BeginBeginMode.HAS_SUCCESS;
        } else if (ownActiveOfferId >= 0) {
            beginMode = BeginBeginMode.HAS_OFFER;
        } else {
            beginMode = BeginBeginMode.NO_OFFER;
        }
    }

    private void applyParamValidateResult(String vr) {
        if (GtsService.ValidateSpeciesResult.OK.name().equals(vr)) {
            paramSpeciesChecked = true;
            paramSpeciesInvalid = false;
            lastErrorKey = "";
            return;
        }
        if (GtsService.ValidateSpeciesResult.INCOMPATIBLE_GENDER.name().equals(vr)) {
            paramGender = GenderFilter.ANY;
            paramSpeciesChecked = true;
            paramSpeciesInvalid = false;
            lastErrorKey = "";
            return;
        }
        paramSpeciesChecked = false;
        paramSpeciesInvalid = true;
        lastErrorKey = validateResultToErrorKey(vr);
    }

    private static String validateResultToErrorKey(String vr) {
        try {
            return switch (GtsService.ValidateSpeciesResult.valueOf(vr)) {
                case INVALID -> "gui.cobblesafari.rotomphone.gts.error.invalid";
                case BANNED -> "gui.cobblesafari.rotomphone.gts.error.banned";
                case INCOMPATIBLE_GENDER -> "gui.cobblesafari.rotomphone.gts.error.incompatible_gender";
                case OK -> "";
            };
        } catch (IllegalArgumentException e) {
            return "gui.cobblesafari.rotomphone.gts.error.error";
        }
    }

    private GenderFilter paramGenderForValidation(String speciesLine) {
        if (paramGender == GenderFilter.ANY) {
            return GenderFilter.ANY;
        }
        if (speciesLine.isBlank() || GtsService.isWishGenderCompatible(speciesLine, paramGender)) {
            return paramGender;
        }
        paramGender = GenderFilter.ANY;
        return GenderFilter.ANY;
    }

    private Pokemon loadPokemon(CompoundTag tag) {
        if (this.minecraft == null || this.minecraft.player == null || tag == null || tag.isEmpty()) {
            return null;
        }
        try {
            return Pokemon.Companion.loadFromNBT(this.minecraft.player.registryAccess(), tag);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    protected boolean showBackButton() {
        if (state == SubScreen.DEPOSIT_ANIM
                || state == SubScreen.RETRIEVAL_ANIM
                || state == SubScreen.RECEIVE_ANIM) {
            return System.currentTimeMillis() - tradeStartedAtMillis >= ANIM_HALF_TOTAL_MS;
        }
        if (state == SubScreen.TRADE_ANIM) {
            return System.currentTimeMillis() - tradeStartedAtMillis >= ANIM_TRADE_TOTAL_MS;
        }
        return state != SubScreen.LOADING;
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        switch (state) {
            case LOADING -> {
            }
            case BEGIN -> {
                renderGtsHeader(graphics, "gui.cobblesafari.rotomphone.gts.title");
                renderBegin(graphics, mouseX, mouseY);
            }
            case SELECT -> {
                renderGtsHeader(graphics, "gui.cobblesafari.rotomphone.gts.selecttitle");
                renderSelect(graphics, mouseX, mouseY, partialTick);
            }
            case PARAMETERS -> {
                renderGtsHeader(graphics, "gui.cobblesafari.rotomphone.gts.paramtitle");
                renderParameters(graphics, mouseX, mouseY, partialTick);
            }
            case CONFIRM -> {
                renderGtsHeader(graphics, "gui.cobblesafari.rotomphone.gts.paramtitle");
                renderConfirm(graphics, mouseX, mouseY, partialTick);
            }
            case DEPOSIT_ANIM -> renderDepositAnim(graphics, mouseX, mouseY, partialTick);
            case RETRIEVAL_ANIM -> renderRetrievalOrReceiveAnim(graphics, mouseX, mouseY, partialTick, true);
            case RECEIVE_ANIM -> renderRetrievalOrReceiveAnim(graphics, mouseX, mouseY, partialTick, false);
            case SEEK -> {
                renderGtsHeader(graphics, "gui.cobblesafari.rotomphone.gts.seektitle");
                renderSeek(graphics, mouseX, mouseY, partialTick);
            }
            case CHECK -> {
                renderGtsHeader(graphics, "gui.cobblesafari.rotomphone.gts.checktitle");
                renderCheck(graphics, mouseX, mouseY, partialTick);
            }
            case TRADE_ANIM -> renderTradeAnim(graphics, mouseX, mouseY, partialTick);
            case ERROR -> {
                renderGtsHeader(graphics, "gui.cobblesafari.rotomphone.gts.title");
                if (!lastErrorKey.isEmpty()) {
                    drawScaledCentered(graphics, Component.translatable(lastErrorKey),
                            originX + 174, originY + 152, 0xFFFFFFFF);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (state == SubScreen.SELECT && hasShiftDown()) {
            int slot = hoveredPartySlot(mouseX, mouseY);
            if (slot >= 0) {
                Pokemon po = getPartyPokemon(slot);
                if (po != null) {
                    graphics.renderComponentTooltip(this.font, buildPokemonTooltip(po), mouseX, mouseY);
                }
            }
        }
        if (state == SubScreen.CONFIRM && hasShiftDown()) {
            if (isInBounds(mouseX, mouseY, originX + 58, originY + 56, CONFIRM_SLOT_SIZE, CONFIRM_SLOT_SIZE)) {
                Pokemon po = getPartyPokemon(selectedSlot);
                if (po != null) {
                    graphics.renderComponentTooltip(this.font, buildPokemonTooltip(po), mouseX, mouseY);
                }
            }
            if (isInBounds(mouseX, mouseY, originX + 218, originY + 56, CONFIRM_SLOT_SIZE, CONFIRM_SLOT_SIZE)) {
                Pokemon w = getWishedPreviewPokemon();
                if (w != null) {
                    graphics.renderComponentTooltip(this.font, buildWishPreviewTooltip(w), mouseX, mouseY);
                }
            }
        }
        if (state == SubScreen.SEEK) {
            int si = hoveredSeekSlot(mouseX, mouseY);
            if (si >= 0 && si < seekResults.size()) {
                GtsAppResultPayload.SearchEntry e = seekResults.get(si);
                Pokemon po = getSeekPokemon(e);
                if (po != null) {
                    if (hasShiftDown()) {
                        graphics.renderComponentTooltip(this.font, buildPokemonTooltip(po), mouseX, mouseY);
                    } else {
                        graphics.renderComponentTooltip(this.font, buildOfferSummaryTooltip(po, e), mouseX, mouseY);
                    }
                }
            }
        }
        if (state == SubScreen.CHECK && checkOffer != null) {
            Pokemon offered = getSeekPokemon(checkOffer);
            if (offered != null && isInBounds(mouseX, mouseY, originX + 178, originY + 56, 32, 32)) {
                if (hasShiftDown()) {
                    graphics.renderComponentTooltip(this.font, buildPokemonTooltip(offered), mouseX, mouseY);
                } else {
                    graphics.renderComponentTooltip(this.font, buildOfferSummaryTooltip(offered, checkOffer), mouseX, mouseY);
                }
            }
            if (hasShiftDown() && !checkCandidates.isEmpty() && matchIndex < checkCandidates.size()) {
                Pokemon m = loadPokemon(checkCandidates.get(matchIndex));
                if (m != null && isInBounds(mouseX, mouseY, originX + 158, originY + 96, 32, 32)) {
                    graphics.renderComponentTooltip(this.font, buildPokemonTooltip(m), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (state == SubScreen.SEEK && seekDirty
                && System.currentTimeMillis() - seekLastTypedAt >= SEEK_DEBOUNCE_MS) {
            seekDirty = false;
            sendSearch();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return switch (state) {
            case BEGIN -> handleBeginClick(mouseX, mouseY);
            case SELECT -> handleSelectClick(mouseX, mouseY);
            case PARAMETERS -> handleParametersClick(mouseX, mouseY);
            case CONFIRM -> handleConfirmClick(mouseX, mouseY);
            case DEPOSIT_ANIM, RETRIEVAL_ANIM, RECEIVE_ANIM -> handleHalfAnimClick(mouseX, mouseY);
            case SEEK -> handleSeekClick(mouseX, mouseY);
            case CHECK -> handleCheckClick(mouseX, mouseY);
            case TRADE_ANIM -> handleTradeAnimClick(mouseX, mouseY);
            default -> super.mouseClicked(mouseX, mouseY, button);
        };
    }

    private void sendAbortIfPendingTrade() {
        if (state == SubScreen.CHECK && checkLaunched) {
            Services.PLATFORM.sendPayloadToServer(
                    new GtsAppPayload(GtsAppPayload.ACTION_ABORT_TRADE, 0, 0, "", "", ""));
        }
    }

    @Override
    public void onClose() {
        sendAbortIfPendingTrade();
        super.onClose();
    }

    @Override
    protected void onBackButtonClicked() {
        switch (state) {
            case SELECT -> {
                selectedSlot = -1;
                state = SubScreen.BEGIN;
                return;
            }
            case PARAMETERS -> {
                removeParameterFocus();
                state = SubScreen.SELECT;
                return;
            }
            case CONFIRM -> {
                if (!confirmLocked && !confirmPending) {
                    confirmLocked = false;
                    state = SubScreen.PARAMETERS;
                    return;
                }
            }
            case SEEK -> {
                hideSeekEdit();
                state = SubScreen.BEGIN;
                return;
            }
            case CHECK -> {
                sendAbortIfPendingTrade();
                resetCheckScreen();
                hideSeekEdit();
                state = SubScreen.SEEK;
                return;
            }
            case ERROR -> {
                lastErrorKey = "";
                state = SubScreen.BEGIN;
                Services.PLATFORM.sendPayloadToServer(new GtsAppPayload(GtsAppPayload.ACTION_REQUEST_STATE, 0, 0, "", "", ""));
                return;
            }
            default -> {
            }
        }
        sendAbortIfPendingTrade();
        super.onBackButtonClicked();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (state == SubScreen.PARAMETERS && paramSpeciesBox != null && paramSpeciesBox.isFocused()) {
                paramSpeciesBox.setFocused(false);
                return true;
            }
            if (state == SubScreen.SEEK && seekSpeciesBox != null && seekSpeciesBox.isFocused()) {
                seekSpeciesBox.setFocused(false);
                clearSeekSuggestions();
                flushSeekSearch(false);
                return true;
            }
            onBackButtonClicked();
            return true;
        }
        if (state == SubScreen.PARAMETERS && paramSpeciesBox != null) {
            if ((keyCode == 258 || keyCode == 257) && !paramSuggestions.isEmpty()) {
                paramSpeciesBox.setValue(paramSuggestions.get(0));
                paramSpeciesBox.moveCursorToEnd(false);
                clearParamSuggestions();
                return true;
            }
            if (paramSpeciesBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (state == SubScreen.SEEK && seekSpeciesBox != null) {
            if ((keyCode == 258 || keyCode == 257) && !seekSuggestions.isEmpty()) {
                seekSpeciesBox.setValue(seekSuggestions.get(0));
                seekSpeciesBox.moveCursorToEnd(false);
                seekSpeciesBox.setFocused(false);
                clearSeekSuggestions();
                flushSeekSearch(true);
                return true;
            }
            if (seekSpeciesBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (state == SubScreen.PARAMETERS && paramSpeciesBox != null && paramSpeciesBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (state == SubScreen.SEEK && seekSpeciesBox != null && seekSpeciesBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void renderGtsHeader(GuiGraphics g, String titleKey) {
        drawTinted(g, TEX_GTS_LOGO, originX + 58, originY + 16, 32, 32, 0xFFFFFFFF);
        drawScaledLeftAligned(g, Component.translatable(titleKey), originX + 98, originY + 32, 0xFFFFFFFF);
    }

    private void renderBegin(GuiGraphics g, int mx, int my) {
        int theme = getTintColor();

        if (beginMode == BeginBeginMode.CONFIRM_RETRIEVE) {
            drawScaledCentered(g, Component.translatable("gui.cobblesafari.rotomphone.gts.confirmation"),
                    originX + 174, originY + 72, 0xFFFFFFFF);
            if (retrievePending) {
                drawTinted(g, TEX_DOUBLE, originX + 98, originY + BEGIN_CONFIRM_BUTTON_Y, 72, 32, theme);
                drawTinted(g, TEX_DOUBLE, originX + 178, originY + BEGIN_CONFIRM_BUTTON_Y, 72, 32, theme);
            } else {
                boolean yHov = isInBounds(mx, my, originX + 98, originY + BEGIN_CONFIRM_BUTTON_Y, 72, 32);
                boolean nHov = isInBounds(mx, my, originX + 178, originY + BEGIN_CONFIRM_BUTTON_Y, 72, 32);
                drawButton(g, originX + 98, originY + BEGIN_CONFIRM_BUTTON_Y, yHov, theme,
                        Component.translatable("gui.cobblesafari.rotomphone.gts.yes"));
                drawButton(g, originX + 178, originY + BEGIN_CONFIRM_BUTTON_Y, nHov, theme,
                        Component.translatable("gui.cobblesafari.rotomphone.gts.no"));
            }
            return;
        }

        drawCenteredStatusLine(g, Component.translatable("gui.cobblesafari.rotomphone.gts.status", offerCount),
                originX + 174, originY + 72, theme);

        boolean depHov = isInBounds(mx, my, originX + 98, originY + 96, 72, 32);
        boolean seekHov = isInBounds(mx, my, originX + 178, originY + 96, 72, 32);
        if (claimPending) {
            drawTinted(g, TEX_DOUBLE, originX + 98, originY + 96, 72, 32, theme);
        } else {
            Component depLabel = switch (beginMode) {
                case NO_OFFER -> Component.translatable("gui.cobblesafari.rotomphone.gts.deposit");
                case HAS_OFFER -> Component.translatable("gui.cobblesafari.rotomphone.gts.retrieve");
                case HAS_SUCCESS -> Component.translatable("gui.cobblesafari.rotomphone.gts.receive");
                default -> Component.translatable("gui.cobblesafari.rotomphone.gts.deposit");
            };
            drawButton(g, originX + 98, originY + 96, depHov, theme, depLabel);
        }
        drawButton(g, originX + 178, originY + 96, seekHov, theme, Component.translatable("gui.cobblesafari.rotomphone.gts.seek"));

        if (!lastErrorKey.isEmpty()) {
            drawScaledCentered(g, Component.translatable(lastErrorKey), originX + 174, originY + 152, 0xFFFFFFFF);
        }
    }

    private void renderSelect(GuiGraphics g, int mx, int my, float partialTick) {
        int theme = getTintColor();
        ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
        for (int i = 0; i < 6; i++) {
            int x = originX + PARTY_SLOT_X[i];
            int y = originY + 56;
            Pokemon p = party.get(i);
            boolean hovered = isInBounds(mx, my, x, y, 32, 32);
            int tint = hovered ? 0xFFFFFFFF : theme;
            drawTinted(g, TEX_EMPTY, x, y, 32, 32, tint);
            if (p != null) {
                drawPokemonInArea(g, p, x, y, PARTY_SLOT_SIZE, PARTY_SLOT_SIZE, 0,
                        partialTick, partyStates[i], PARTY_SLOT_BASE_SCALE, PARTY_SLOT_MODEL_SCALE,
                        PARTY_SLOT_MODEL_Y_OFFSET);
            }
        }
        if (selectedSlot < 0) {
            drawScaledCentered(g, Component.translatable("gui.cobblesafari.rotomphone.wonder.choose"),
                    originX + 174, originY + 112, theme);
        } else {
            boolean nh = isInBounds(mx, my, originX + 138, originY + 96, 72, 32);
            drawButton(g, originX + 138, originY + 96, nh, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.gts.next"));
        }
    }

    private void renderParameters(GuiGraphics g, int mx, int my, float partialTick) {
        int theme = getTintColor();
        syncParamSpeciesBoxLayout();
        paramSpeciesBox.render(g, mx, my, partialTick);

        if (paramSpeciesInvalid) {
            drawBlitWithAlpha(g, TEX_INVALID, originX + 258, originY + 56, 0, 0, 32, 32, 32, 32);
        } else if (paramSpeciesChecked) {
            drawBlitWithAlpha(g, TEX_VALID, originX + 258, originY + 56, 0, 0, 32, 32, 32, 32);
        }

        boolean gHov = isInBounds(mx, my, originX + 58, originY + 96, 32, 32);
        drawTinted(g, genderTexture(paramGender), originX + 58, originY + 96, 32, 32, gHov ? 0xFFFFFFFF : theme);

        boolean sHov = isInBounds(mx, my, originX + 258, originY + 96, 32, 32);
        drawTinted(g, shinyTexture(paramShiny), originX + 258, originY + 96, 32, 32, sHov ? 0xFFFFFFFF : theme);

        boolean ldHov = isInBounds(mx, my, originX + 98, originY + 96, 32, 32);
        drawTinted(g, TEX_LEFT, originX + 98, originY + 96, 32, 32, ldHov ? 0xFFFFFFFF : theme);

        boolean luHov = isInBounds(mx, my, originX + 218, originY + 96, 32, 32);
        drawTinted(g, TEX_RIGHT, originX + 218, originY + 96, 32, 32, luHov ? 0xFFFFFFFF : theme);

        String levelKey = paramLevelBucket < 0 ? "any" : Integer.toString(paramLevelBucket);
        drawScaledCentered(g, Component.translatable(GTS_LEVEL_PREFIX + levelKey),
                originX + 174, originY + 112, theme);

        if (!paramSpeciesChecked) {
            boolean chHov = isInBounds(mx, my, originX + 138, originY + 136, 72, 32);
            drawButton(g, originX + 138, originY + 136, chHov, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.gts.check"));
        } else if (!paramSpeciesInvalid) {
            boolean nxHov = isInBounds(mx, my, originX + 138, originY + 136, 72, 32);
            drawButton(g, originX + 138, originY + 136, nxHov, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.gts.next"));
        }

        if (!paramSuggestions.isEmpty() && paramSpeciesBox != null && paramSpeciesBox.isFocused()) {
            int sugY = suggestionTopY();
            paramSuggestionHover = hoveredSuggestionIndex(mx, my, originX + PARAM_SUGGESTION_X, sugY,
                    PARAM_SUGGESTION_WIDTH, paramSuggestions);
            SpeciesAutocompleteHelper.renderSuggestions(g, this.font, originX + PARAM_SUGGESTION_X, sugY,
                    PARAM_SUGGESTION_WIDTH, paramSuggestions, paramSuggestionHover);
        } else {
            paramSuggestionHover = -1;
        }
    }

    private void renderConfirm(GuiGraphics g, int mx, int my, float partialTick) {
        int theme = getTintColor();
        Pokemon offered = getPartyPokemon(selectedSlot);
        int ox = originX + 58;
        int oy = originY + 56;
        if (offered != null) {
            drawPokemonInArea(g, offered, ox, oy, CONFIRM_SLOT_SIZE, CONFIRM_SLOT_SIZE, CONFIRM_SLOT_CLIP_WIDTH,
                    partialTick, partyStates[Math.max(0, selectedSlot)],
                    CONFIRM_SLOT_BASE_SCALE, CONFIRM_SLOT_MODEL_SCALE, CONFIRM_SLOT_MODEL_Y_OFFSET);
        }

        int wx = originX + 218;
        Pokemon wished = getWishedPreviewPokemon();
        if (wished != null) {
            if (offeredWishState == null) {
                offeredWishState = new FloatingState();
            }
            drawPokemonInArea(g, wished, wx, oy, CONFIRM_SLOT_SIZE, CONFIRM_SLOT_SIZE, CONFIRM_SLOT_CLIP_WIDTH,
                    partialTick, offeredWishState,
                    CONFIRM_SLOT_BASE_SCALE, CONFIRM_SLOT_MODEL_SCALE, CONFIRM_SLOT_MODEL_Y_OFFSET);
        }

        long spinElapsed = confirmLocked || confirmPending
                ? System.currentTimeMillis() - confirmLockedAtMillis
                : 0L;
        renderTradeIcon(g, spinElapsed);

        if (!confirmLocked) {
            boolean sh = isInBounds(mx, my, originX + 138, originY + CONFIRM_ACTION_Y, 72, 32);
            drawButton(g, originX + 138, originY + CONFIRM_ACTION_Y, sh, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.gts.send"));
        } else if (confirmPending) {
            drawTinted(g, TEX_DOUBLE, originX + 138, originY + CONFIRM_ACTION_Y, 72, 32, theme);
        } else {
            int digit = countdownDigit(confirmLockedAtMillis);
            if (digit > 0) {
                drawTinted(g, TEX_DOUBLE, originX + 138, originY + CONFIRM_ACTION_Y, 72, 32, theme);
                int textY = originY + CONFIRM_ACTION_Y + (32 - this.font.lineHeight) / 2;
                g.drawCenteredString(this.font, Component.literal(String.valueOf(digit)),
                        originX + 138 + 36, textY, theme);
            } else {
                boolean ch = isInBounds(mx, my, originX + 138, originY + CONFIRM_ACTION_Y, 72, 32);
                drawButton(g, originX + 138, originY + CONFIRM_ACTION_Y, ch, theme,
                        Component.translatable("gui.cobblesafari.rotomphone.wonder.confirm"));
            }
        }
    }

    private void renderTradeIcon(GuiGraphics g, long elapsedMs) {
        g.pose().pushPose();
        int cx = originX + 138 + 36;
        int cy = originY + 56 + 36;
        g.pose().translate(cx, cy, 0f);
        if (elapsedMs > 0L) {
            float deg = (elapsedMs % TRADE_ICON_ROTATION_PERIOD_MS) * 360f / TRADE_ICON_ROTATION_PERIOD_MS;
            g.pose().mulPose(Axis.ZP.rotationDegrees(deg));
        }
        drawTinted(g, TEX_TRADE, -36, -36, 72, 72, 0xFFFFFFFF);
        g.pose().popPose();
    }

    private void renderDepositAnim(GuiGraphics g, int mx, int my, float partialTick) {
        int theme = getTintColor();
        long elapsed = System.currentTimeMillis() - tradeStartedAtMillis;
        int sx = originX + TRADE_SLOT_X;
        int sy = originY + TRADE_SLOT_Y;
        if (depositShowPokemon(elapsed) && offeredAnimPokemon != null) {
            float scale = forwardDisappearPokemonScale(elapsed);
            drawPokemonInArea(g, offeredAnimPokemon, sx, sy, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_CLIP_WIDTH, partialTick, offeredTradeState, TRADE_SLOT_BASE_SCALE,
                    TRADE_SLOT_MODEL_SCALE, 0f, scale);
        }
        int f = getOneWayAnimFrame(elapsed, false);
        if (f >= 0) {
            int vOffset = f * TRADE_SLOT_SIZE;
            drawBlitWithAlpha(g, TEX_TRADEANIM, sx, sy, 0, vOffset, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_SIZE, TRADE_SLOT_SIZE * ANIM_FRAME_COUNT);
        }
        if (elapsed >= ANIM_HALF_TOTAL_MS) {
            boolean h = isInBounds(mx, my, originX + 138, originY + HALF_ANIM_EXIT_Y, 72, 32);
            drawButton(g, originX + 138, originY + HALF_ANIM_EXIT_Y, h, theme,
                    Component.translatable(GTS_EXIT));
        }
    }

    private void renderRetrievalOrReceiveAnim(GuiGraphics g, int mx, int my, float partialTick, boolean retrieval) {
        int theme = getTintColor();
        long elapsed = System.currentTimeMillis() - tradeStartedAtMillis;
        int sx = originX + TRADE_SLOT_X;
        int sy = originY + TRADE_SLOT_Y;
        Pokemon p = retrieval ? offeredAnimPokemon : receivedAnimPokemon;
        FloatingState st = retrieval ? offeredTradeState : receivedTradeState;
        if (retrievalShowPokemon(elapsed) && p != null && st != null) {
            float scale = reverseAppearPokemonScale(elapsed);
            drawPokemonInArea(g, p, sx, sy, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_CLIP_WIDTH, partialTick, st, TRADE_SLOT_BASE_SCALE,
                    TRADE_SLOT_MODEL_SCALE, 0f, scale);
        }
        int f = getOneWayAnimFrame(elapsed, true);
        if (f >= 0) {
            int vOffset = f * TRADE_SLOT_SIZE;
            drawBlitWithAlpha(g, TEX_TRADEANIM, sx, sy, 0, vOffset, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_SIZE, TRADE_SLOT_SIZE * ANIM_FRAME_COUNT);
        }
        if (elapsed >= ANIM_HALF_TOTAL_MS) {
            boolean h = isInBounds(mx, my, originX + 138, originY + HALF_ANIM_EXIT_Y, 72, 32);
            drawButton(g, originX + 138, originY + HALF_ANIM_EXIT_Y, h, theme,
                    Component.translatable(GTS_EXIT));
        }
    }

    private void renderTradeAnim(GuiGraphics g, int mx, int my, float partialTick) {
        int theme = getTintColor();
        long elapsed = System.currentTimeMillis() - tradeStartedAtMillis;
        int sx = originX + TRADE_SLOT_X;
        int sy = originY + TRADE_SLOT_Y;

        boolean showOffered = shouldShowOfferedModel(elapsed);
        boolean showReceived = shouldShowReceivedModel(elapsed);
        if (showOffered && offeredAnimPokemon != null && offeredTradeState != null) {
            float scale = forwardDisappearPokemonScale(elapsed);
            drawPokemonInArea(g, offeredAnimPokemon, sx, sy, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_CLIP_WIDTH, partialTick, offeredTradeState, TRADE_SLOT_BASE_SCALE,
                    TRADE_SLOT_MODEL_SCALE, 0f, scale);
        } else if (showReceived && receivedAnimPokemon != null && receivedTradeState != null) {
            float scale = receivedAppearPokemonScale(elapsed);
            drawPokemonInArea(g, receivedAnimPokemon, sx, sy, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_CLIP_WIDTH, partialTick, receivedTradeState, TRADE_SLOT_BASE_SCALE,
                    TRADE_SLOT_MODEL_SCALE, 0f, scale);
        }

        int animFrame = getTradeAnimFrame(elapsed);
        if (animFrame >= 0) {
            int vOffset = animFrame * TRADE_SLOT_SIZE;
            drawBlitWithAlpha(g, TEX_TRADEANIM, sx, sy, 0, vOffset, TRADE_SLOT_SIZE, TRADE_SLOT_SIZE,
                    TRADE_SLOT_SIZE, TRADE_SLOT_SIZE * ANIM_FRAME_COUNT);
        }

        if (elapsed >= ANIM_TRADE_TOTAL_MS) {
            boolean h = isInBounds(mx, my, originX + 138, originY + 136, 72, 32);
            drawButton(g, originX + 138, originY + 136, h, theme,
                    Component.translatable(GTS_EXIT));
        }
    }

    private boolean depositShowPokemon(long elapsed) {
        if (elapsed < ANIM_INITIAL_DELAY_MS) {
            return true;
        }
        long animElapsed = elapsed - ANIM_INITIAL_DELAY_MS;
        if (animElapsed < ANIM_FORWARD_MS) {
            int frame = (int) (animElapsed / ANIM_FRAME_MS);
            return frame < 4;
        }
        return false;
    }

    private boolean retrievalShowPokemon(long elapsed) {
        if (elapsed < ANIM_INITIAL_DELAY_MS) {
            return false;
        }
        long e = elapsed - ANIM_INITIAL_DELAY_MS;
        if (e >= ANIM_FORWARD_MS) {
            return true;
        }
        int idx = (int) (e / ANIM_FRAME_MS);
        return idx >= 10;
    }

    private int getOneWayAnimFrame(long elapsed, boolean reverse) {
        if (elapsed < ANIM_INITIAL_DELAY_MS) {
            return -1;
        }
        long e = elapsed - ANIM_INITIAL_DELAY_MS;
        if (e >= ANIM_FORWARD_MS) {
            return -1;
        }
        int f = (int) (e / ANIM_FRAME_MS);
        return reverse ? (ANIM_FRAME_COUNT - 1 - f) : f;
    }

    private boolean shouldShowOfferedModel(long elapsed) {
        if (elapsed < ANIM_INITIAL_DELAY_MS) {
            return true;
        }
        long animElapsed = elapsed - ANIM_INITIAL_DELAY_MS;
        if (animElapsed < ANIM_FORWARD_MS) {
            int frame = (int) (animElapsed / ANIM_FRAME_MS);
            return frame < 4;
        }
        return false;
    }

    private boolean shouldShowReceivedModel(long elapsed) {
        long reverseStart = ANIM_INITIAL_DELAY_MS + ANIM_FORWARD_MS + ANIM_PAUSE_MS;
        if (elapsed < reverseStart) {
            return false;
        }
        long reverseElapsed = elapsed - reverseStart;
        if (reverseElapsed >= ANIM_FORWARD_MS) {
            return true;
        }
        int frameFromStart = (int) (reverseElapsed / ANIM_FRAME_MS);
        int frameFromEnd = ANIM_FRAME_COUNT - 1 - frameFromStart;
        return frameFromEnd <= 4;
    }

    private float forwardDisappearPokemonScale(long elapsed) {
        if (elapsed < ANIM_INITIAL_DELAY_MS) {
            return 1f;
        }
        long animElapsed = elapsed - ANIM_INITIAL_DELAY_MS;
        int frame = (int) (animElapsed / ANIM_FRAME_MS);
        if (frame < ANIM_DISAPPEAR_FRAME - 1) {
            return 1f;
        }
        if (frame >= ANIM_DISAPPEAR_FRAME) {
            return ANIM_MIN_POKEMON_SCALE;
        }
        float t = (animElapsed % ANIM_FRAME_MS) / (float) ANIM_FRAME_MS;
        return 1f - (1f - ANIM_MIN_POKEMON_SCALE) * t;
    }

    private float reverseAppearPokemonScale(long elapsed) {
        if (elapsed < ANIM_INITIAL_DELAY_MS) {
            return ANIM_MIN_POKEMON_SCALE;
        }
        long animElapsed = elapsed - ANIM_INITIAL_DELAY_MS;
        int frame = (int) (animElapsed / ANIM_FRAME_MS);
        if (frame < ANIM_APPEAR_FORWARD_INDEX) {
            return ANIM_MIN_POKEMON_SCALE;
        }
        if (frame > ANIM_APPEAR_FORWARD_INDEX) {
            return 1f;
        }
        float t = (animElapsed % ANIM_FRAME_MS) / (float) ANIM_FRAME_MS;
        return ANIM_MIN_POKEMON_SCALE + (1f - ANIM_MIN_POKEMON_SCALE) * t;
    }

    private float receivedAppearPokemonScale(long elapsed) {
        long reverseStart = ANIM_INITIAL_DELAY_MS + ANIM_FORWARD_MS + ANIM_PAUSE_MS;
        if (elapsed < reverseStart) {
            return ANIM_MIN_POKEMON_SCALE;
        }
        long reverseElapsed = elapsed - reverseStart;
        if (reverseElapsed >= ANIM_FORWARD_MS) {
            return 1f;
        }
        int frameFromStart = (int) (reverseElapsed / ANIM_FRAME_MS);
        int frameFromEnd = ANIM_FRAME_COUNT - 1 - frameFromStart;
        if (frameFromEnd > ANIM_RECEIVED_VISIBLE_FROM_END) {
            return ANIM_MIN_POKEMON_SCALE;
        }
        if (frameFromEnd < ANIM_RECEIVED_VISIBLE_FROM_END) {
            return 1f;
        }
        float t = (reverseElapsed % ANIM_FRAME_MS) / (float) ANIM_FRAME_MS;
        return ANIM_MIN_POKEMON_SCALE + (1f - ANIM_MIN_POKEMON_SCALE) * t;
    }

    private int getTradeAnimFrame(long elapsed) {
        if (elapsed < ANIM_INITIAL_DELAY_MS) {
            return -1;
        }
        long animElapsed = elapsed - ANIM_INITIAL_DELAY_MS;
        if (animElapsed < ANIM_FORWARD_MS) {
            return (int) (animElapsed / ANIM_FRAME_MS);
        }
        long afterForward = animElapsed - ANIM_FORWARD_MS;
        if (afterForward < ANIM_PAUSE_MS) {
            return -1;
        }
        long reverseElapsed = afterForward - ANIM_PAUSE_MS;
        if (reverseElapsed >= ANIM_FORWARD_MS) {
            return -1;
        }
        int reverseFrame = (int) (reverseElapsed / ANIM_FRAME_MS);
        return ANIM_FRAME_COUNT - 1 - reverseFrame;
    }

    private void renderSeek(GuiGraphics g, int mx, int my, float partialTick) {
        int theme = getTintColor();
        syncSeekSpeciesBoxLayout();
        seekSpeciesBox.render(g, mx, my, partialTick);

        boolean gHov = isInBounds(mx, my, originX + 58, originY + 56, 32, 32);
        drawTinted(g, genderTexture(seekGender), originX + 58, originY + 56, 32, 32, gHov ? 0xFFFFFFFF : theme);

        boolean sHov = isInBounds(mx, my, originX + 258, originY + 56, 32, 32);
        drawTinted(g, shinyTexture(seekShiny), originX + 258, originY + 56, 32, 32, sHov ? 0xFFFFFFFF : theme);

        if (seekResults.isEmpty()) {
            drawScaledCentered(g, Component.translatable("gui.cobblesafari.rotomphone.gts.nooffer"),
                    originX + 174, originY + 112, 0xFFFFFFFF);
        } else {
            int slotIdx = 0;
            for (int slotX : SEEK_SLOT_ROW1_X) {
                renderSeekSlot(g, mx, my, partialTick, theme, slotIdx++, originX + slotX, originY + SEEK_ROW1_Y);
            }
            for (int slotX : SEEK_SLOT_ROW2_X) {
                renderSeekSlot(g, mx, my, partialTick, theme, slotIdx++, originX + slotX, originY + SEEK_ROW2_Y);
            }

            if (seekPage > 1) {
                g.pose().pushPose();
                g.pose().translate(originX + 58 + 32, originY + 136, 0);
                g.pose().scale(-1f, 1f, 1f);
                boolean ph = isInBounds(mx, my, originX + 58, originY + 136, 32, 32);
                drawTinted(g, TEX_RIGHT, 0, 0, 32, 32, ph ? 0xFFFFFFFF : theme);
                g.pose().popPose();
                g.drawCenteredString(this.font, Component.literal(String.valueOf(seekPage - 1)),
                        originX + 58 + 16, originY + 136 + 10, 0xFFFFFFFF);
            }
            if (seekPage < seekTotalPages) {
                boolean nh = isInBounds(mx, my, originX + 258, originY + 136, 32, 32);
                drawTinted(g, TEX_RIGHT, originX + 258, originY + 136, 32, 32, nh ? 0xFFFFFFFF : theme);
                g.drawCenteredString(this.font, Component.literal(String.valueOf(seekPage + 1)),
                        originX + 258 + 16, originY + 136 + 10, 0xFFFFFFFF);
            }
        }

        if (!seekSuggestions.isEmpty() && seekSpeciesBox != null && seekSpeciesBox.isFocused()) {
            int sugY = suggestionTopY();
            seekSuggestionHover = hoveredSuggestionIndex(mx, my, originX + SEEK_SUGGESTION_X, sugY,
                    SEEK_SUGGESTION_WIDTH, seekSuggestions);
            SpeciesAutocompleteHelper.renderSuggestions(g, this.font, originX + SEEK_SUGGESTION_X, sugY,
                    SEEK_SUGGESTION_WIDTH, seekSuggestions, seekSuggestionHover);
        } else {
            seekSuggestionHover = -1;
        }
    }

    private void renderSeekSlot(GuiGraphics g, int mx, int my, float partialTick, int theme, int index, int x, int y) {
        boolean hovered = isInBounds(mx, my, x, y, 32, 32);
        int tint = hovered ? 0xFFFFFFFF : theme;
        drawTinted(g, TEX_EMPTY, x, y, 32, 32, tint);
        if (index < seekResults.size()) {
            GtsAppResultPayload.SearchEntry e = seekResults.get(index);
            Pokemon p = getSeekPokemon(e);
            if (p != null) {
                drawPokemonInArea(g, p, x, y, PARTY_SLOT_SIZE, PARTY_SLOT_SIZE, 0,
                        partialTick, seekSlotStates[index], PARTY_SLOT_BASE_SCALE, PARTY_SLOT_MODEL_SCALE,
                        PARTY_SLOT_MODEL_Y_OFFSET);
            }
        }
    }

    private void renderCheck(GuiGraphics g, int mx, int my, float partialTick) {
        int theme = getTintColor();
        g.drawCenteredString(this.font, Component.translatable("gui.cobblesafari.rotomphone.gts.offeredpkmn"),
                originX + 174, originY + 72, theme);

        Pokemon offered = checkOffer == null ? null : getSeekPokemon(checkOffer);
        int ox = originX + 178;
        int oy = originY + 56;
        drawTinted(g, TEX_EMPTY, ox, oy, 32, 32, theme);
        if (offered != null) {
            drawPokemonInArea(g, offered, ox, oy, 32, 32, 0, partialTick, seekSlotStates[0],
                    PARTY_SLOT_BASE_SCALE, PARTY_SLOT_MODEL_SCALE, PARTY_SLOT_MODEL_Y_OFFSET);
        }

        boolean noMatch = checkLaunched && checkNoMatch;

        if (!checkLaunched) {
            boolean ch = isInBounds(mx, my, originX + 138, originY + 96, 72, 32);
            drawButton(g, originX + 138, originY + 96, ch, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.gts.checkpkmn"));
        } else if (noMatch) {
            drawCenteredStatusLine(g, Component.translatable("gui.cobblesafari.rotomphone.gts.nomatch"),
                    originX + 174, originY + 112, theme);
            boolean retHov = isInBounds(mx, my, originX + 138, originY + 136, 72, 32);
            drawButton(g, originX + 138, originY + 136, retHov, theme,
                    Component.translatable("gui.cobblesafari.rotomphone.gts.return"));
        } else {
            if (checkCandidates.size() > 1) {
                boolean ph = isInBounds(mx, my, originX + 98, originY + 96, 32, 32);
                drawTinted(g, TEX_LEFT, originX + 98, originY + 96, 32, 32, ph ? 0xFFFFFFFF : theme);

                boolean nh = isInBounds(mx, my, originX + 218, originY + 96, 32, 32);
                drawTinted(g, TEX_RIGHT, originX + 218, originY + 96, 32, 32, nh ? 0xFFFFFFFF : theme);
            }

            int mxSlot = originX + 158;
            int mySlot = originY + 96;
            drawTinted(g, TEX_EMPTY, mxSlot, mySlot, 32, 32, theme);
            if (matchIndex >= 0 && matchIndex < checkCandidates.size()) {
                Pokemon m = loadPokemon(checkCandidates.get(matchIndex));
                if (m != null) {
                    drawPokemonInArea(g, m, mxSlot, mySlot, 32, 32, 0, partialTick, seekSlotStates[1],
                            PARTY_SLOT_BASE_SCALE, PARTY_SLOT_MODEL_SCALE, PARTY_SLOT_MODEL_Y_OFFSET);
                }
            }

            if (!matchLocked) {
                boolean tr = isInBounds(mx, my, originX + 138, originY + 136, 72, 32);
                drawButton(g, originX + 138, originY + 136, tr, theme,
                        Component.translatable("gui.cobblesafari.rotomphone.gts.accept"));
            } else if (tradeConfirmPending) {
                drawTinted(g, TEX_DOUBLE, originX + 138, originY + 136, 72, 32, theme);
            } else {
                int digit = countdownDigit(matchLockedAtMillis);
                if (digit > 0) {
                    drawTinted(g, TEX_DOUBLE, originX + 138, originY + 136, 72, 32, theme);
                    int textY = originY + 136 + (32 - this.font.lineHeight) / 2;
                    g.drawCenteredString(this.font, Component.literal(String.valueOf(digit)),
                            originX + 138 + 36, textY, theme);
                } else {
                    boolean cf = isInBounds(mx, my, originX + 138, originY + 136, 72, 32);
                    drawButton(g, originX + 138, originY + 136, cf, theme,
                            Component.translatable("gui.cobblesafari.rotomphone.wonder.confirm"));
                }
            }
        }
    }

    private boolean handleBeginClick(double mx, double my) {
        if (beginMode == BeginBeginMode.CONFIRM_RETRIEVE) {
            if (!retrievePending && isInBounds(mx, my, originX + 98, originY + BEGIN_CONFIRM_BUTTON_Y, 72, 32)) {
                retrievePending = true;
                Services.PLATFORM.sendPayloadToServer(
                        new GtsAppPayload(GtsAppPayload.ACTION_RETRIEVE, ownActiveOfferId, 0, "", "", ""));
                return true;
            }
            if (!retrievePending && isInBounds(mx, my, originX + 178, originY + BEGIN_CONFIRM_BUTTON_Y, 72, 32)) {
                beginMode = BeginBeginMode.HAS_OFFER;
                return true;
            }
            return super.mouseClicked(mx, my, 0);
        }
        if (isInBounds(mx, my, originX + 98, originY + 96, 72, 32)) {
            return switch (beginMode) {
                case NO_OFFER -> {
                    selectedSlot = -1;
                    state = SubScreen.SELECT;
                    yield true;
                }
                case HAS_OFFER -> {
                    beginMode = BeginBeginMode.CONFIRM_RETRIEVE;
                    yield true;
                }
                case HAS_SUCCESS -> {
                    if (!claimPending && oldestSuccessId >= 0) {
                        claimPending = true;
                        Services.PLATFORM.sendPayloadToServer(
                                new GtsAppPayload(GtsAppPayload.ACTION_CLAIM, oldestSuccessId, 0, "", "", ""));
                    }
                    yield true;
                }
                default -> false;
            };
        }
        if (isInBounds(mx, my, originX + 178, originY + 96, 72, 32)) {
            state = SubScreen.SEEK;
            seekPage = 1;
            seekDirty = false;
            sendSearch();
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleSelectClick(double mx, double my) {
        for (int i = 0; i < 6; i++) {
            int x = originX + PARTY_SLOT_X[i];
            int y = originY + 56;
            if (isInBounds(mx, my, x, y, 32, 32) && getPartyPokemon(i) != null) {
                selectedSlot = i;
                return true;
            }
        }
        if (selectedSlot >= 0 && isInBounds(mx, my, originX + 138, originY + 96, 72, 32)) {
            resetParametersForm();
            offeredWishState = new FloatingState();
            state = SubScreen.PARAMETERS;
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleParametersClick(double mx, double my) {
        int sugY = suggestionTopY();
        if (pickSuggestion(mx, my, originX + PARAM_SUGGESTION_X, sugY, PARAM_SUGGESTION_WIDTH,
                paramSuggestions, paramSuggestionHover)) {
            return true;
        }
        if (paramSpeciesBox != null) {
            boolean inBox = isInBounds(mx, my, originX + 58, originY + 56, 232, 32);
            if (inBox) {
                if (paramSpeciesChecked) {
                    paramSpeciesChecked = false;
                }
                if (paramSpeciesInvalid) {
                    paramSpeciesInvalid = false;
                }
                paramSpeciesBox.setFocused(true);
                paramSpeciesBox.mouseClicked(mx, my, 0);
                return true;
            }
            paramSpeciesBox.setFocused(false);
            clearParamSuggestions();
        }
        if (isInBounds(mx, my, originX + 58, originY + 96, 32, 32)) {
            paramGender = cycleGender(paramGender);
            paramSpeciesChecked = false;
            paramSpeciesInvalid = false;
            return true;
        }
        if (isInBounds(mx, my, originX + 258, originY + 96, 32, 32)) {
            paramShiny = paramShiny.next();
            return true;
        }
        if (isInBounds(mx, my, originX + 98, originY + 96, 32, 32)) {
            cycleLevelDown();
            return true;
        }
        if (isInBounds(mx, my, originX + 218, originY + 96, 32, 32)) {
            cycleLevelUp();
            return true;
        }
        if (!paramSpeciesChecked && isInBounds(mx, my, originX + 138, originY + 136, 72, 32)) {
            String line = sanitizeSpeciesLine(paramSpeciesBox.getValue());
            GenderFilter genderForValidate = paramGenderForValidation(line);
            Services.PLATFORM.sendPayloadToServer(
                    new GtsAppPayload(
                            GtsAppPayload.ACTION_VALIDATE_SPECIES,
                            0,
                            0,
                            line,
                            genderForValidate.name(),
                            ""));
            return true;
        }
        if (paramSpeciesChecked && !paramSpeciesInvalid && isInBounds(mx, my, originX + 138, originY + 136, 72, 32)) {
            state = SubScreen.CONFIRM;
            confirmLocked = false;
            confirmPending = false;
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleConfirmClick(double mx, double my) {
        if (!confirmLocked && isInBounds(mx, my, originX + 138, originY + CONFIRM_ACTION_Y, 72, 32)) {
            confirmLocked = true;
            confirmLockedAtMillis = System.currentTimeMillis();
            return true;
        }
        if (confirmLocked && !confirmPending && countdownDigit(confirmLockedAtMillis) == 0
                && isInBounds(mx, my, originX + 138, originY + CONFIRM_ACTION_Y, 72, 32)) {
            confirmPending = true;
            String species = sanitizeSpeciesLine(paramSpeciesBox.getValue());
            Services.PLATFORM.sendPayloadToServer(new GtsAppPayload(
                    GtsAppPayload.ACTION_DEPOSIT,
                    selectedSlot,
                    paramLevelBucket,
                    species,
                    paramGender.name(),
                    paramShiny.name()));
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleHalfAnimClick(double mx, double my) {
        long elapsed = System.currentTimeMillis() - tradeStartedAtMillis;
        if (elapsed < ANIM_HALF_TOTAL_MS) {
            return false;
        }
        if (isInBounds(mx, my, originX + 138, originY + HALF_ANIM_EXIT_Y, 72, 32)) {
            finishAnimationReturnBegin();
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleTradeAnimClick(double mx, double my) {
        long elapsed = System.currentTimeMillis() - tradeStartedAtMillis;
        if (elapsed < ANIM_TRADE_TOTAL_MS) {
            return false;
        }
        if (isInBounds(mx, my, originX + 138, originY + 136, 72, 32)) {
            finishAnimationReturnBegin();
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private void finishAnimationReturnBegin() {
        resetCheckScreen();
        selectedSlot = -1;
        confirmLocked = false;
        confirmPending = false;
        retrievePending = false;
        claimPending = false;
        state = SubScreen.BEGIN;
        Services.PLATFORM.sendPayloadToServer(new GtsAppPayload(GtsAppPayload.ACTION_REQUEST_STATE, 0, 0, "", "", ""));
    }

    private boolean handleSeekClick(double mx, double my) {
        int sugY = suggestionTopY();
        if (pickSuggestion(mx, my, originX + SEEK_SUGGESTION_X, sugY, SEEK_SUGGESTION_WIDTH,
                seekSuggestions, seekSuggestionHover)) {
            return true;
        }
        if (isInBounds(mx, my, originX + 58, originY + 56, 32, 32)) {
            blurSeekSpeciesBox();
            seekGender = cycleGender(seekGender);
            sendSearch();
            return true;
        }
        if (isInBounds(mx, my, originX + 258, originY + 56, 32, 32)) {
            blurSeekSpeciesBox();
            seekShiny = seekShiny.next();
            sendSearch();
            return true;
        }
        int slot = seekSlotIndexAt((int) mx, (int) my);
        if (slot >= 0 && slot < seekResults.size()) {
            blurSeekSpeciesBox();
            checkOffer = seekResults.get(slot);
            resetCheckScreen();
            state = SubScreen.CHECK;
            return true;
        }
        if (seekPage > 1 && isInBounds(mx, my, originX + 58, originY + 136, 32, 32)) {
            blurSeekSpeciesBox();
            seekPage--;
            sendSearch();
            return true;
        }
        if (seekPage < seekTotalPages && isInBounds(mx, my, originX + 258, originY + 136, 32, 32)) {
            blurSeekSpeciesBox();
            seekPage++;
            sendSearch();
            return true;
        }
        if (seekSpeciesBox != null) {
            boolean inBox = isInBounds(mx, my, originX + 98, originY + 56, 152, 32);
            if (inBox) {
                seekSpeciesBox.setFocused(true);
                seekSpeciesBox.mouseClicked(mx, my, 0);
                return true;
            }
            if (seekSpeciesBox.isFocused()) {
                seekSpeciesBox.setFocused(false);
                clearSeekSuggestions();
                flushSeekSearch(false);
            }
        }
        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleCheckClick(double mx, double my) {
        boolean noMatch = checkLaunched && checkNoMatch;
        if (noMatch && isInBounds(mx, my, originX + 138, originY + 136, 72, 32)) {
            sendAbortIfPendingTrade();
            resetCheckScreen();
            hideSeekEdit();
            state = SubScreen.SEEK;
            return true;
        }
        if (!checkLaunched && isInBounds(mx, my, originX + 138, originY + 96, 72, 32)) {
            if (checkOffer != null) {
                checkLaunched = true;
                checkNoMatch = false;
                checkCandidates = List.of();
                Services.PLATFORM.sendPayloadToServer(
                        new GtsAppPayload(GtsAppPayload.ACTION_START_TRADE, checkOffer.offerId(), 0, "", "", ""));
            }
            return true;
        }
        if (checkLaunched && !checkNoMatch && checkCandidates.size() > 1) {
            if (isInBounds(mx, my, originX + 98, originY + 96, 32, 32)) {
                matchIndex = (matchIndex - 1 + checkCandidates.size()) % checkCandidates.size();
                resetCheckTradeConfirmation();
                return true;
            }
            if (isInBounds(mx, my, originX + 218, originY + 96, 32, 32)) {
                matchIndex = (matchIndex + 1) % checkCandidates.size();
                resetCheckTradeConfirmation();
                return true;
            }
        }
        if (!matchLocked && checkLaunched && !checkNoMatch && !checkCandidates.isEmpty()
                && isInBounds(mx, my, originX + 138, originY + 136, 72, 32)) {
            matchLocked = true;
            matchLockedAtMillis = System.currentTimeMillis();
            return true;
        }
        if (matchLocked && !tradeConfirmPending && countdownDigit(matchLockedAtMillis) == 0
                && isInBounds(mx, my, originX + 138, originY + 136, 72, 32)) {
            tradeConfirmPending = true;
            if (checkOffer != null) {
                Services.PLATFORM.sendPayloadToServer(new GtsAppPayload(
                        GtsAppPayload.ACTION_CONFIRM_TRADE,
                        checkOffer.offerId(),
                        matchIndex + 1,
                        "",
                        "",
                        ""));
            }
            return true;
        }
        return super.mouseClicked(mx, my, 0);
    }

    private void sendSearch() {
        String species = seekSpeciesBox == null ? "" : seekSpeciesBox.getValue().trim();
        Services.PLATFORM.sendPayloadToServer(new GtsAppPayload(
                GtsAppPayload.ACTION_SEARCH,
                seekPage,
                0,
                species,
                seekGender.name(),
                seekShiny.name()));
    }

    /** Runs a pending debounced seek search immediately (e.g. blur). */
    private void flushSeekSearch(boolean force) {
        if (force || seekDirty) {
            seekDirty = false;
            sendSearch();
        }
    }

    private void blurSeekSpeciesBox() {
        if (seekSpeciesBox == null) {
            return;
        }
        if (seekSpeciesBox.isFocused()) {
            seekSpeciesBox.setFocused(false);
            clearSeekSuggestions();
            seekDirty = false;
        }
    }

    private int seekSlotIndexAt(int mx, int my) {
        int idx = 0;
        for (int slotX : SEEK_SLOT_ROW1_X) {
            if (isInBounds(mx, my, originX + slotX, originY + SEEK_ROW1_Y, 32, 32)) {
                return idx;
            }
            idx++;
        }
        for (int slotX : SEEK_SLOT_ROW2_X) {
            if (isInBounds(mx, my, originX + slotX, originY + SEEK_ROW2_Y, 32, 32)) {
                return idx;
            }
            idx++;
        }
        return -1;
    }

    private int hoveredSeekSlot(int mx, int my) {
        int i = seekSlotIndexAt(mx, my);
        return i < seekResults.size() ? i : -1;
    }

    private void resetParametersForm() {
        paramSpeciesChecked = false;
        paramSpeciesInvalid = false;
        paramGender = GenderFilter.ANY;
        paramShiny = GtsOffer.ShinyWish.ANY;
        paramLevelBucket = -1;
        if (paramSpeciesBox != null) {
            paramSpeciesBox.setValue("");
        }
    }

    private void resetCheckScreen() {
        checkLaunched = false;
        checkNoMatch = false;
        checkCandidates = List.of();
        matchIndex = 0;
        matchLocked = false;
        tradeConfirmPending = false;
    }

    private void resetCheckTradeConfirmation() {
        matchLocked = false;
        tradeConfirmPending = false;
    }

    private void clearParamSuggestions() {
        paramSuggestions = List.of();
        paramSuggestionHover = -1;
    }

    private void clearSeekSuggestions() {
        seekSuggestions = List.of();
        seekSuggestionHover = -1;
    }

    private int suggestionTopY() {
        return originY + 56 + 32 + SUGGESTION_Y_BELOW_BOX;
    }

    private void drawCenteredStatusLine(GuiGraphics g, Component c, int centerX, int y, int color) {
        int textY = y + (int) SCALED_TEXT_Y_OFFSET;
        g.drawCenteredString(this.font, c, centerX, textY, color);
    }

    private void removeParameterFocus() {
        if (paramSpeciesBox != null) {
            paramSpeciesBox.setFocused(false);
        }
    }

    private void hideSeekEdit() {
        if (seekSpeciesBox != null) {
            seekSpeciesBox.setFocused(false);
        }
    }

    private void syncParamSpeciesBoxLayout() {
        if (paramSpeciesBox == null) {
            return;
        }
        paramSpeciesBox.setX(originX + 60);
        paramSpeciesBox.setY(originY + 58);
        paramSpeciesBox.setWidth(226);
        paramSpeciesBox.setHeight(28);
    }

    private void syncSeekSpeciesBoxLayout() {
        if (seekSpeciesBox == null) {
            return;
        }
        seekSpeciesBox.setX(originX + 100);
        seekSpeciesBox.setY(originY + 58);
        seekSpeciesBox.setWidth(144);
        seekSpeciesBox.setHeight(28);
    }

    private static String sanitizeSpeciesLine(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    private static GenderFilter cycleGender(GenderFilter g) {
        return switch (g) {
            case ANY -> GenderFilter.MALE;
            case MALE -> GenderFilter.FEMALE;
            case FEMALE, GENDERLESS -> GenderFilter.ANY;
        };
    }

    private void cycleLevelUp() {
        paramLevelBucket = paramLevelBucket == 9 ? -1 : paramLevelBucket + 1;
    }

    private void cycleLevelDown() {
        paramLevelBucket = paramLevelBucket == -1 ? 9 : paramLevelBucket - 1;
    }

    private static ResourceLocation genderTexture(GenderFilter g) {
        String suffix = switch (g) {
            case MALE -> "male";
            case FEMALE -> "female";
            default -> "any";
        };
        return loc("gts/rotomphone_gui_icon_gender_" + suffix + ".png");
    }

    private static ResourceLocation shinyTexture(GtsOffer.ShinyWish s) {
        String suffix = switch (s) {
            case SHINY -> "shiny";
            case NOT_SHINY -> "notshiny";
            default -> "any";
        };
        return loc("gts/rotomphone_gui_icon_shiny_" + suffix + ".png");
    }

    private Pokemon getPartyPokemon(int slot) {
        if (slot < 0 || slot > 5) {
            return null;
        }
        return CobblemonClient.INSTANCE.getStorage().getParty().get(slot);
    }

    private int hoveredPartySlot(int mx, int my) {
        for (int i = 0; i < 6; i++) {
            if (isInBounds(mx, my, originX + PARTY_SLOT_X[i], originY + 56, 32, 32)) {
                return i;
            }
        }
        return -1;
    }

    private Pokemon getWishedPreviewPokemon() {
        if (paramSpeciesBox == null) {
            return null;
        }
        String line = sanitizeSpeciesLine(paramSpeciesBox.getValue());
        if (line.isEmpty()) {
            return null;
        }
        try {
            return PokemonProperties.Companion.parse(line).create();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Pokemon getSeekPokemon(GtsAppResultPayload.SearchEntry e) {
        return seekPokemonCache.computeIfAbsent(e.offerId(), id -> loadPokemon(e.offeredNbt()));
    }

    private List<Component> buildWishPreviewTooltip(Pokemon w) {
        List<Component> lines = new ArrayList<>();
        lines.add(w.getDisplayName(false));
        lines.add(Component.translatable(TT_LEVEL, w.getLevel()));
        lines.addAll(wishCriteriaLines());
        return lines;
    }

    private List<Component> wishCriteriaLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.gts.tt.wish.species",
                Component.literal(sanitizeSpeciesLine(paramSpeciesBox.getValue()))));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.gts.tt.wish.level",
                Component.translatable(GTS_LEVEL_PREFIX
                        + (paramLevelBucket < 0 ? "any" : Integer.toString(paramLevelBucket)))));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.gts.tt.wish.gender",
                genderFilterLabel(paramGender)));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.gts.tt.wish.shiny",
                shinyFilterLabel(paramShiny)));
        return lines;
    }

    private List<Component> buildOfferSummaryTooltip(Pokemon offered, GtsAppResultPayload.SearchEntry e) {
        List<Component> lines = new ArrayList<>();
        lines.add(offered.getDisplayName(false));
        lines.add(Component.translatable(TT_LEVEL, offered.getLevel()));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.gender", genderComponent(offered)));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.shiny",
                Component.translatable(offered.getShiny()
                        ? TT_SHINY_YES
                        : TT_SHINY_NO)));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.gts.tt.wish.species",
                Component.literal(e.wishSpecies() == null ? "" : e.wishSpecies())));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.gts.tt.wish.level",
                Component.translatable(GTS_LEVEL_PREFIX
                        + (e.wishLevelBucket() < 0 ? "any" : Integer.toString(e.wishLevelBucket())))));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.gts.tt.wish.gender",
                genderFilterLabel(GenderFilter.parse(e.wishGender()))));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.gts.tt.wish.shiny",
                shinyFilterLabel(GtsOffer.ShinyWish.parse(e.wishShiny()))));
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.gts.tt.holdshift"));
        return lines;
    }

    private static Component genderComponent(Pokemon p) {
        String genderKey = switch (p.getGender()) {
            case MALE -> GENDER_MALE;
            case FEMALE -> GENDER_FEMALE;
            default -> GENDER_GENDERLESS;
        };
        return Component.translatable(genderKey);
    }

    private static Component genderFilterLabel(GenderFilter g) {
        return switch (g) {
            case MALE -> Component.translatable(GENDER_MALE);
            case FEMALE -> Component.translatable(GENDER_FEMALE);
            case GENDERLESS -> Component.translatable(GENDER_GENDERLESS);
            default -> Component.translatable("gui.cobblesafari.rotomphone.gts.level.any");
        };
    }

    private static Component shinyFilterLabel(GtsOffer.ShinyWish s) {
        return switch (s) {
            case SHINY -> Component.translatable(TT_SHINY_YES);
            case NOT_SHINY -> Component.translatable(TT_SHINY_NO);
            default -> Component.translatable("gui.cobblesafari.rotomphone.gts.level.any");
        };
    }

    private List<Component> buildPokemonTooltip(Pokemon p) {
        List<Component> lines = new ArrayList<>();
        lines.add(p.getDisplayName(false));
        lines.add(Component.translatable(TT_LEVEL, p.getLevel()));

        String genderKey = switch (p.getGender()) {
            case MALE -> GENDER_MALE;
            case FEMALE -> GENDER_FEMALE;
            default -> GENDER_GENDERLESS;
        };
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.gender",
                Component.translatable(genderKey)));

        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.shiny",
                Component.translatable(p.getShiny()
                        ? TT_SHINY_YES
                        : TT_SHINY_NO)));

        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.ability",
                Component.translatable("cobblemon.ability." + p.getAbility().getName())));

        var ballId = p.getCaughtBall().getName();
        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.ball",
                Component.translatable("item." + ballId.getNamespace() + "." + ballId.getPath())));

        lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.stats"));
        for (int i = 0; i < TOOLTIP_STATS.length; i++) {
            Stat st = TOOLTIP_STATS[i];
            Integer ev = p.getEvs().get(st);
            Integer iv = p.getIvs().get(st);
            lines.add(Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.stat_line",
                    Component.translatable("gui.cobblesafari.rotomphone.wonder.tt.stat." + TOOLTIP_STAT_KEYS[i]),
                    ev == null ? 0 : ev,
                    iv == null ? 0 : iv));
        }
        return lines;
    }

    private int countdownDigit(long lockedAtMillis) {
        long elapsed = System.currentTimeMillis() - lockedAtMillis;
        if (elapsed < 1000L) {
            return 3;
        }
        if (elapsed < 2000L) {
            return 2;
        }
        if (elapsed < 3000L) {
            return 1;
        }
        return 0;
    }

    private static int hoveredSuggestionIndex(int mx, int my, int x, int y, int width, List<String> suggestions) {
        if (suggestions.isEmpty()) {
            return -1;
        }
        int lineH = 12;
        int h = suggestions.size() * lineH + 4;
        if (!isInBounds(mx, my, x, y, width, h)) {
            return -1;
        }
        int rel = my - (y + 2);
        if (rel < 0) {
            return -1;
        }
        return rel / lineH;
    }

    private boolean pickSuggestion(double mx, double my, int x, int y, int width, List<String> suggestions, int hover) {
        if (hover < 0 || hover >= suggestions.size()) {
            return false;
        }
        int lineH = 12;
        int h = suggestions.size() * lineH + 4;
        if (!isInBounds(mx, my, x, y, width, h)) {
            return false;
        }
        String pick = suggestions.get(hover);
        if (state == SubScreen.PARAMETERS && paramSpeciesBox != null) {
            paramSpeciesBox.setValue(pick);
            paramSpeciesBox.moveCursorToEnd(false);
            paramSpeciesBox.setFocused(false);
            clearParamSuggestions();
        } else if (state == SubScreen.SEEK && seekSpeciesBox != null) {
            seekSpeciesBox.setValue(pick);
            seekSpeciesBox.moveCursorToEnd(false);
            seekSpeciesBox.setFocused(false);
            clearSeekSuggestions();
            flushSeekSearch(true);
        }
        return true;
    }

    private void drawPokemonInArea(
            GuiGraphics g,
            Pokemon pokemon,
            int x,
            int y,
            int w,
            int h,
            int clipWidth,
            float partialTick,
            FloatingState floatingState,
            float baseScale,
            float modelScale,
            float modelYOffset) {
        drawPokemonInArea(g, pokemon, x, y, w, h, clipWidth, partialTick, floatingState,
                baseScale, modelScale, modelYOffset, 1f);
    }

    private void drawPokemonInArea(
            GuiGraphics g,
            Pokemon pokemon,
            int x,
            int y,
            int w,
            int h,
            int clipWidth,
            float partialTick,
            FloatingState floatingState,
            float baseScale,
            float modelScale,
            float modelYOffset,
            float animScaleMult) {
        int scissorW = clipWidth > 0 ? clipWidth : w;
        int clipX = x + (w - scissorW) / 2;
        g.enableScissor(clipX, y, clipX + scissorW, y + h);
        g.pose().pushPose();
        float scaledBase = baseScale * animScaleMult;
        float scaledModel = modelScale * animScaleMult;
        boolean confirmSlot = w == CONFIRM_SLOT_SIZE && h == CONFIRM_SLOT_SIZE;
        if (confirmSlot) {
            g.pose().translate(x + w * 0.5f, y + h * 0.5f, 0.0f);
            g.pose().scale(scaledBase, scaledBase, scaledBase);
            if (modelYOffset != 0f) {
                g.pose().translate(0f, modelYOffset / scaledBase, 0f);
            }
        } else {
            g.pose().translate(x + w * 0.5, y + 1.0 - modelYOffset, 0.0);
            g.pose().scale(scaledBase, scaledBase, scaledBase);
        }
        Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(
                new Quaternionf(), new Vector3f(13f, 35f, 0f));
        RenderablePokemon renderable = pokemon.asRenderablePokemon();
        floatingState.setCurrentAspects(renderable.getAspects());
        PokemonGuiUtilsKt.drawProfilePokemon(
                renderable,
                g.pose(),
                rotation,
                PoseType.PROFILE,
                floatingState,
                partialTick,
                scaledModel,
                true,
                false,
                1f,
                1f,
                1f,
                1f,
                0f,
                0f);
        g.pose().popPose();
        g.disableScissor();
    }

    private void drawScaledLeftAligned(GuiGraphics g, Component c, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y + SCALED_TEXT_Y_OFFSET, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(this.font, c, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawScaledCentered(GuiGraphics g, Component c, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y + SCALED_TEXT_Y_OFFSET, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawCenteredString(this.font, c, 0, 0, color);
        g.pose().popPose();
    }

    private void drawBlitWithAlpha(
            GuiGraphics g,
            ResourceLocation tex,
            int x,
            int y,
            int u,
            int v,
            int w,
            int h,
            int texW,
            int texH) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, 1f);
        g.blit(tex, x, y, u, v, w, h, texW, texH);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawTinted(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int argb) {
        float red = ((argb >> 16) & 0xFF) / 255f;
        float green = ((argb >> 8) & 0xFF) / 255f;
        float blue = (argb & 0xFF) / 255f;
        float alpha = ((argb >>> 24) & 0xFF) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(red, green, blue, alpha);
        g.blit(tex, x, y, 0, 0, w, h, w, h);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawButton(GuiGraphics g, int x, int y, boolean hovered, int themeTint, Component label) {
        int tint = hovered ? 0xFFFFFFFF : themeTint;
        drawTinted(g, TEX_DOUBLE, x, y, 72, 32, tint);
        int textY = y + (32 - this.font.lineHeight) / 2;
        g.drawCenteredString(this.font, label, x + 36, textY, tint);
    }
}
