package maxigregrze.cobblesafari.client.screen.rotomphone;

import com.mojang.blaze3d.systems.RenderSystem;
import maxigregrze.cobblesafari.data.ChatProgressSavedData;
import maxigregrze.cobblesafari.network.ChatAppPayload;
import maxigregrze.cobblesafari.network.ChatAppResultPayload;
import maxigregrze.cobblesafari.network.ChatConversationSyncPayload;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.rotomphone.ChatConversationClientCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Rotom Phone "Chat" app — data-driven questline messenger (cf. action plan 114 §8-12).
 * Client-driven message streaming; server-persisted position; server-authoritative progress.
 * The whole transcript (all steps up to the current one) stays on screen and is scrollable.
 */
public class RotomPhoneChatScreen extends RotomPhoneBaseScreen {

    // --- Layout (relative to originX/originY) ---
    private static final int SEP_V_X0 = 93;
    private static final int SEP_V_X1 = 95;
    private static final int CONTACTS_X0 = 58;
    private static final int CONTACTS_X1 = 98;
    private static final int CONTACTS_Y0 = 16;
    private static final int CONTACTS_Y1 = 168;
    private static final int CONTACT_BTN_X = 59; // 32px button in the 58..98 area (§8.4a)
    private static final int CONTACT_BTN_SIZE = 32;
    private static final int CONTACT_GAP = 8;
    private static final int CONTACTS_SLIDER_X0 = 48;
    private static final int CONTACTS_SLIDER_X1 = 56;

    private static final int CHAT_X0 = 98;
    private static final int CHAT_X1 = 290;
    private static final int CHAT_Y0 = 35;
    private static final int CHAT_Y1 = 168;
    private static final int CHAT_SLIDER_X0 = 292;
    private static final int CHAT_SLIDER_X1 = 300;

    private static final int BUBBLE_W = 192;
    private static final int BUBBLE_TEXT_W = BUBBLE_W - 4;
    private static final int BUBBLE_PAD = 2;
    private static final int BUBBLE_PAD_V = 4;
    private static final int CORNER_R = 5;

    private static final int TYPING_W = 40;
    private static final int TYPING_H = 12;
    private static final int TYPING_FRAMES = 3;
    private static final ResourceLocation TEX_TYPING = loc("chat/rotomphone_gui_typing.png");

    private static final int COL_WHITE = 0xFFFFFFFF;
    private static final int COL_TINT_HOVER = 0x80FFFFFF;

    private static final long STREAM_STEP_MS = 3000L;
    private static final long POLL_MS = 1000L;
    private static final int SLIDER_MAX_KNOB = 148;
    private static final int SLIDER_MIN_KNOB = 4;

    private enum Stream { IDLE, BEFORE, AFTER }

    private final List<ChatConversationSyncPayload.Entry> contacts = new ArrayList<>();
    private String activeConvId;
    private ChatAppResultPayload.StateData state;

    // streaming / animation (apply to the current step only)
    private Stream stream = Stream.IDLE;
    private boolean typing;
    private long nextEventAt;
    private int localBeforeShown;
    private int localAfterShown;
    private boolean taskVisible;
    private boolean claimPending;
    private long nextPollAt;

    // scrolling
    private int scrollContacts;
    private int scrollChat;
    private int lastChatOverflow;
    private int lastContactsOverflow;

    // slider interaction / last-rendered knob geometry
    private static final int SLIDER_STEP = 12;
    private int draggingSlider; // 0 none, 1 contacts, 2 chat
    private int dragGrabDy;
    private int contactsKnobY;
    private int contactsKnobH;
    private boolean contactsSliderShown;
    private int chatKnobY;
    private int chatKnobH;
    private boolean chatSliderShown;

    public RotomPhoneChatScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode, boolean rotoGlide) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.chat"), rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide);
    }

    @Override
    protected void init() {
        super.init();
        refreshContacts();
        Services.PLATFORM.sendPayloadToServer(
                new ChatAppPayload(ChatAppPayload.ACTION_REQUEST_CONTACTS, "", 0));
        // Open the lowest-priority contact by default (contacts are sorted asc by priority).
        if (activeConvId == null && !contacts.isEmpty()) {
            openConversation(contacts.get(0).id());
        }
    }

    private void refreshContacts() {
        contacts.clear();
        for (ChatConversationSyncPayload.Entry e : ChatConversationClientCache.getConversations()) {
            if (e.unlocked()) {
                contacts.add(e);
            }
        }
        contacts.sort((a, b) -> {
            int c = Integer.compare(a.priority(), b.priority());
            return c != 0 ? c : a.id().compareTo(b.id());
        });
    }

    private void openConversation(String convId) {
        this.activeConvId = convId;
        this.state = null;
        this.stream = Stream.IDLE;
        this.typing = false;
        this.taskVisible = false;
        this.claimPending = false;
        this.localBeforeShown = 0;
        this.localAfterShown = 0;
        this.scrollChat = 0;
        Services.PLATFORM.sendPayloadToServer(new ChatAppPayload(ChatAppPayload.ACTION_OPEN, convId, 0));
    }

    private ChatAppResultPayload.StepView currentStep() {
        if (state == null || state.steps().isEmpty()) {
            return null;
        }
        return state.steps().get(state.steps().size() - 1);
    }

    /** Called from the network receiver (Fabric/NeoForge). */
    public void applyServerSnapshot(ChatAppResultPayload p) {
        if (p.kind() == ChatAppResultPayload.KIND_ERROR) {
            // Benign failure (e.g. NOT_COMPLETE): just re-enable the task bar.
            this.claimPending = false;
            return;
        }
        ChatAppResultPayload.StateData s = p.state();
        if (s == null || activeConvId == null || !activeConvId.equals(s.convId())) {
            return;
        }
        this.state = s;
        this.claimPending = false;

        ChatAppResultPayload.StepView cur = currentStep();
        if (cur == null) {
            return;
        }
        int phase = s.phase();
        int beforeSize = cur.before().size();
        int afterSize = cur.after().size();

        if (phase == ChatProgressSavedData.Phase.BEFORE.ordinal()) {
            this.localBeforeShown = Math.min(cur.beforeShown(), beforeSize);
            this.localAfterShown = 0;
            this.taskVisible = false;
            startStreaming(Stream.BEFORE, beforeSize);
        } else {
            this.localBeforeShown = beforeSize;
            this.taskVisible = true;
            if (phase == ChatProgressSavedData.Phase.AFTER.ordinal()) {
                this.localAfterShown = Math.min(cur.afterShown(), afterSize);
                startStreaming(Stream.AFTER, afterSize);
            } else if (phase == ChatProgressSavedData.Phase.TASK.ordinal()) {
                this.localAfterShown = 0;
                this.stream = Stream.IDLE;
                this.typing = false;
                this.nextPollAt = System.currentTimeMillis() + POLL_MS;
            } else { // WAIT_NEXT_DAY / DONE
                this.localAfterShown = afterSize;
                this.stream = Stream.IDLE;
                this.typing = false;
            }
        }
    }

    private void startStreaming(Stream which, int total) {
        int shown = which == Stream.BEFORE ? localBeforeShown : localAfterShown;
        this.stream = which;
        this.typing = shown < total;
        this.nextEventAt = System.currentTimeMillis() + STREAM_STEP_MS;
    }

    @Override
    public void tick() {
        super.tick();
        // Late contact-list arrival (sync after init): auto-open the lowest-priority contact.
        if (activeConvId == null) {
            refreshContacts();
            if (!contacts.isEmpty()) {
                openConversation(contacts.get(0).id());
            }
            return;
        }
        ChatAppResultPayload.StepView cur = currentStep();
        if (cur == null) {
            return;
        }
        long now = System.currentTimeMillis();

        if (taskVisible && stream == Stream.IDLE
                && state.phase() == ChatProgressSavedData.Phase.TASK.ordinal()
                && !cur.done() && now >= nextPollAt) {
            nextPollAt = now + POLL_MS;
            Services.PLATFORM.sendPayloadToServer(
                    new ChatAppPayload(ChatAppPayload.ACTION_POLL_TASK, activeConvId, 0));
        }

        if (stream == Stream.IDLE || now < nextEventAt) {
            return;
        }

        if (stream == Stream.BEFORE) {
            int total = cur.before().size();
            if (localBeforeShown < total) {
                localBeforeShown++;
                Services.PLATFORM.sendPayloadToServer(
                        new ChatAppPayload(ChatAppPayload.ACTION_ADVANCE_MESSAGE, activeConvId, localBeforeShown));
                typing = localBeforeShown < total;
                nextEventAt = now + STREAM_STEP_MS;
            } else {
                typing = false;
                stream = Stream.IDLE;
                Services.PLATFORM.sendPayloadToServer(
                        new ChatAppPayload(ChatAppPayload.ACTION_ADVANCE_MESSAGE, activeConvId, total));
                Services.PLATFORM.sendPayloadToServer(
                        new ChatAppPayload(ChatAppPayload.ACTION_OPEN, activeConvId, 0));
            }
        } else if (stream == Stream.AFTER) {
            int total = cur.after().size();
            if (localAfterShown < total) {
                localAfterShown++;
                Services.PLATFORM.sendPayloadToServer(
                        new ChatAppPayload(ChatAppPayload.ACTION_ADVANCE_MESSAGE, activeConvId, localAfterShown));
                typing = localAfterShown < total;
                nextEventAt = now + STREAM_STEP_MS;
            } else {
                typing = false;
                stream = Stream.IDLE;
                // AFTER_DONE performs the step transition AND replies with the fresh transcript.
                Services.PLATFORM.sendPayloadToServer(
                        new ChatAppPayload(ChatAppPayload.ACTION_AFTER_DONE, activeConvId, 0));
            }
        }
    }

    @Override
    protected void renderPhoneContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int theme = getTintColor();

        fillRect(g, SEP_V_X0, CONTACTS_Y0, SEP_V_X1, CONTACTS_Y1, COL_WHITE);
        fillRect(g, CHAT_X0, 32, CHAT_X1, 33, COL_WHITE);

        Component title = activeConvId != null
                ? Component.literal(activeDisplayName())
                : Component.translatable("gui.cobblesafari.rotomphone.chat.no_contact");
        drawScaledCentered(g, title, originX + 194, originY + 21, theme);

        renderContacts(g, mouseX, mouseY);
        if (activeConvId != null && state != null) {
            renderChat(g, mouseX, mouseY, theme);
        }
    }

    private String activeDisplayName() {
        for (ChatConversationSyncPayload.Entry e : contacts) {
            if (e.id().equals(activeConvId)) {
                return e.displayName();
            }
        }
        return activeConvId;
    }

    // ---------------------------------------------------------------- contacts

    private void renderContacts(GuiGraphics g, int mouseX, int mouseY) {
        int areaH = CONTACTS_Y1 - CONTACTS_Y0;
        int totalH = contacts.isEmpty() ? 0 : contacts.size() * (CONTACT_BTN_SIZE + CONTACT_GAP) - CONTACT_GAP;
        lastContactsOverflow = Math.max(0, totalH - areaH);
        scrollContacts = clamp(scrollContacts, 0, lastContactsOverflow);

        g.enableScissor(originX + CONTACTS_X0, originY + CONTACTS_Y0, originX + CONTACTS_X1, originY + CONTACTS_Y1);
        int y = originY + CONTACTS_Y0 - scrollContacts;
        boolean inArea = isInBounds(mouseX, mouseY, originX + CONTACTS_X0, originY + CONTACTS_Y0,
                CONTACTS_X1 - CONTACTS_X0, areaH);
        for (ChatConversationSyncPayload.Entry e : contacts) {
            int bx = originX + CONTACT_BTN_X;
            ResourceLocation tex = loc("chat/rotomphone_gui_chat_" + e.textureFile() + ".png");
            drawTinted(g, tex, bx, y, CONTACT_BTN_SIZE, CONTACT_BTN_SIZE, COL_WHITE);
            if (inArea && isInBounds(mouseX, mouseY, bx, y, CONTACT_BTN_SIZE, CONTACT_BTN_SIZE)) {
                drawTinted(g, tex, bx, y, CONTACT_BTN_SIZE, CONTACT_BTN_SIZE, COL_TINT_HOVER);
            }
            y += CONTACT_BTN_SIZE + CONTACT_GAP;
        }
        g.disableScissor();

        // Hide the contacts scrollbar entirely when there are 4 contacts or fewer (§ requirement).
        contactsSliderShown = contacts.size() > 4;
        if (contactsSliderShown) {
            float f = lastContactsOverflow > 0 ? (float) scrollContacts / lastContactsOverflow : 0f;
            drawSlider(g, 1, CONTACTS_SLIDER_X0, CONTACTS_SLIDER_X1, totalH, areaH, f);
        }
    }

    // ---------------------------------------------------------------- chat

    private void renderChat(GuiGraphics g, int mouseX, int mouseY, int theme) {
        List<ChatElem> elems = buildChatElements();
        int areaH = CHAT_Y1 - CHAT_Y0;
        int totalH = totalHeight(elems);
        lastChatOverflow = Math.max(0, totalH - areaH);
        scrollChat = clamp(scrollChat, 0, lastChatOverflow);

        g.enableScissor(originX + CHAT_X0, originY + CHAT_Y0, originX + CHAT_X1, originY + CHAT_Y1);
        int x = originX + CHAT_X0;
        int y = originY + CHAT_Y0 - (lastChatOverflow - scrollChat);
        for (ChatElem e : elems) {
            e.render(g, x, y, theme, mouseX, mouseY);
            y += e.height + CONTACT_GAP;
        }
        g.disableScissor();

        // Chat anchors to the bottom: scrollChat == 0 ⇒ viewing the bottom ⇒ knob at the bottom.
        chatSliderShown = true;
        float f = lastChatOverflow > 0 ? (float) (lastChatOverflow - scrollChat) / lastChatOverflow : 1f;
        drawSlider(g, 2, CHAT_SLIDER_X0, CHAT_SLIDER_X1, totalH, areaH, f);
    }

    private int totalHeight(List<ChatElem> elems) {
        int total = 0;
        for (int i = 0; i < elems.size(); i++) {
            total += elems.get(i).height;
            if (i < elems.size() - 1) {
                total += CONTACT_GAP;
            }
        }
        return total;
    }

    private List<ChatElem> buildChatElements() {
        List<ChatElem> elems = new ArrayList<>();
        if (state == null) {
            return elems;
        }
        for (ChatAppResultPayload.StepView sv : state.steps()) {
            boolean isCur = sv.current();
            int bShown = isCur ? localBeforeShown : sv.before().size();
            for (int j = 0; j < bShown && j < sv.before().size(); j++) {
                elems.add(messageBubble(sv.before().get(j)));
            }
            if (isCur && stream == Stream.BEFORE && typing) {
                elems.add(new ChatElem(2, List.of(), TYPING_H, null, false));
            }
            boolean showTask = isCur ? taskVisible : sv.taskVisible();
            if (showTask) {
                elems.add(taskBubble(sv, isCur));
            }
            int aShown = isCur ? localAfterShown : sv.after().size();
            for (int j = 0; j < aShown && j < sv.after().size(); j++) {
                elems.add(messageBubble(sv.after().get(j)));
            }
            if (isCur && stream == Stream.AFTER && typing) {
                elems.add(new ChatElem(2, List.of(), TYPING_H, null, false));
            }
        }
        return elems;
    }

    private ChatElem messageBubble(String langKey) {
        List<FormattedCharSequence> lines = this.font.split(Component.translatable(langKey), BUBBLE_TEXT_W);
        int n = Math.max(1, lines.size());
        int h = n * 8 + (n + 1) * 2 + 4;
        return new ChatElem(0, lines, h, null, false);
    }

    private ChatElem taskBubble(ChatAppResultPayload.StepView sv, boolean isCur) {
        String taskKey = sv.taskTitleKey();
        if (taskKey != null && taskKey.endsWith(".title")) {
            taskKey = taskKey.substring(0, taskKey.length() - ".title".length()) + ".description";
        }
        List<FormattedCharSequence> lines =
                new ArrayList<>(this.font.split(Component.translatable(taskKey), BUBBLE_TEXT_W));
        if (sv.progressDen() > 0) {
            lines.addAll(this.font.split(
                    Component.literal(sv.progressNum() + "/" + sv.progressDen()), BUBBLE_TEXT_W));
        }
        int n = Math.max(1, lines.size());
        int h = n * 8 + (n + 1) * 2 + 20 + 4;
        ChatElem elem = new ChatElem(1, lines, h, Integer.valueOf(n), isCur);
        elem.taskStep = sv;
        return elem;
    }

    /** A renderable chat element: 0=message bubble, 1=task bubble, 2=typing. */
    private final class ChatElem {
        final int type;
        final List<FormattedCharSequence> lines;
        final int height;
        final Integer taskLineCount;
        final boolean taskCurrent;
        ChatAppResultPayload.StepView taskStep;

        ChatElem(int type, List<FormattedCharSequence> lines, int height, Integer taskLineCount, boolean taskCurrent) {
            this.type = type;
            this.lines = lines;
            this.height = height;
            this.taskLineCount = taskLineCount;
            this.taskCurrent = taskCurrent;
        }

        void render(GuiGraphics g, int x, int y, int theme, int mouseX, int mouseY) {
            switch (type) {
                case 0 -> {
                    fillRoundedRect(g, x, y, BUBBLE_W, height, CORNER_R, COL_WHITE, true, true, false, true);
                    drawLines(g, lines, x + BUBBLE_PAD, y, theme);
                }
                case 1 -> renderTask(g, x, y, theme, mouseX, mouseY);
                case 2 -> {
                    int frame = (int) ((System.currentTimeMillis() / 200L) % TYPING_FRAMES);
                    drawTypingFrame(g, x, y, frame);
                }
                default -> { /* nothing */ }
            }
        }

        boolean isClickableTask() {
            return type == 1 && taskCurrent && state != null
                    && state.phase() == ChatProgressSavedData.Phase.TASK.ordinal()
                    && taskStep != null && taskStep.done() && !claimPending;
        }

        private void renderTask(GuiGraphics g, int x, int y, int theme, int mouseX, int mouseY) {
            fillRoundedRect(g, x, y, BUBBLE_W, height, CORNER_R, theme, true, true, true, false);
            drawLines(g, lines, x + BUBBLE_PAD, y, COL_WHITE);

            int n = taskLineCount == null ? 1 : taskLineCount;
            int yb = n * 8 + (n + 1) * 2 + 2;
            int frameX0 = x + 4;
            int frameY0 = y + yb + 1;
            int frameX1 = x + 187;
            int frameY1 = y + yb + 17;
            boolean clickable = isClickableTask();
            boolean hovered = clickable
                    && isInBounds(mouseX, mouseY, frameX0, frameY0, frameX1 - frameX0, frameY1 - frameY0)
                    && isInBounds(mouseX, mouseY, originX + CHAT_X0, originY + CHAT_Y0,
                    CHAT_X1 - CHAT_X0, CHAT_Y1 - CHAT_Y0);

            int barBg = hovered ? theme : COL_WHITE;
            drawRectOutline(g, frameX0, frameY0, frameX1, frameY1, COL_WHITE);
            int innerL = x + 6;
            int innerR = x + 185;
            int innerT = y + yb + 3;
            int innerB = y + yb + 15;
            boolean done = taskStep != null && taskStep.done();
            float ratio;
            if (taskStep != null && taskStep.progressDen() > 0) {
                ratio = taskStep.progressNum() / (float) taskStep.progressDen();
            } else {
                ratio = done ? 1f : 0f;
            }
            ratio = Math.max(0f, Math.min(1f, ratio));
            int fillW = (int) ((innerR - innerL) * ratio);
            if (fillW > 0) {
                g.fill(innerL, innerT, innerL + fillW, innerB, barBg);
            }
            if (done) {
                String key = clickable
                        ? "gui.cobblesafari.rotomphone.chat.complete"
                        : "gui.cobblesafari.rotomphone.chat.completed";
                int textColor = hovered ? COL_WHITE : theme;
                int ty = innerT + (innerB - innerT - font.lineHeight) / 2;
                g.drawCenteredString(font, Component.translatable(key), (innerL + innerR) / 2, ty, textColor);
            }
        }

        private void drawLines(GuiGraphics g, List<FormattedCharSequence> ls, int x, int top, int color) {
            for (int i = 0; i < ls.size(); i++) {
                g.drawString(font, ls.get(i), x, top + BUBBLE_PAD_V + i * 10, color, true);
            }
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Slider knobs first (draggable).
            if (contactsSliderShown && inSliderColumn(mouseX, mouseY, CONTACTS_SLIDER_X0, CONTACTS_SLIDER_X1)) {
                startSliderDrag(1, (int) mouseY);
                return true;
            }
            if (chatSliderShown && state != null && inSliderColumn(mouseX, mouseY, CHAT_SLIDER_X0, CHAT_SLIDER_X1)) {
                startSliderDrag(2, (int) mouseY);
                return true;
            }
            int areaH = CONTACTS_Y1 - CONTACTS_Y0;
            if (isInBounds(mouseX, mouseY, originX + CONTACTS_X0, originY + CONTACTS_Y0,
                    CONTACTS_X1 - CONTACTS_X0, areaH)) {
                int y = originY + CONTACTS_Y0 - scrollContacts;
                for (ChatConversationSyncPayload.Entry e : contacts) {
                    int bx = originX + CONTACT_BTN_X;
                    if (isInBounds(mouseX, mouseY, bx, y, CONTACT_BTN_SIZE, CONTACT_BTN_SIZE)) {
                        openConversation(e.id());
                        return true;
                    }
                    y += CONTACT_BTN_SIZE + CONTACT_GAP;
                }
            }
            if (tryClickTaskBar((int) mouseX, (int) mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean tryClickTaskBar(int mouseX, int mouseY) {
        if (activeConvId == null || state == null) {
            return false;
        }
        List<ChatElem> elems = buildChatElements();
        int areaH = CHAT_Y1 - CHAT_Y0;
        int overflow = Math.max(0, totalHeight(elems) - areaH);
        int sc = clamp(scrollChat, 0, overflow);
        int x = originX + CHAT_X0;
        int y = originY + CHAT_Y0 - (overflow - sc);
        boolean inChat = isInBounds(mouseX, mouseY, originX + CHAT_X0, originY + CHAT_Y0,
                CHAT_X1 - CHAT_X0, areaH);
        for (ChatElem e : elems) {
            if (e.isClickableTask()) {
                int n = e.taskLineCount == null ? 1 : e.taskLineCount;
                int yb = n * 8 + (n + 1) * 2 + 2;
                int fx0 = x + 4;
                int fy0 = y + yb + 1;
                int fx1 = x + 187;
                int fy1 = y + yb + 17;
                if (inChat && isInBounds(mouseX, mouseY, fx0, fy0, fx1 - fx0, fy1 - fy0)) {
                    claimPending = true;
                    Services.PLATFORM.sendPayloadToServer(
                            new ChatAppPayload(ChatAppPayload.ACTION_CLAIM, activeConvId, 0));
                    return true;
                }
            }
            y += e.height + CONTACT_GAP;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingSlider != 0) {
            applySliderDrag((int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingSlider != 0) {
            draggingSlider = 0;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) (scrollY * SLIDER_STEP);
        if (isInBounds(mouseX, mouseY, originX + CHAT_X0, originY + CHAT_Y0,
                CHAT_X1 - CHAT_X0, CHAT_Y1 - CHAT_Y0)) {
            scrollChat = clamp(scrollChat + step, 0, lastChatOverflow);
            return true;
        }
        if (isInBounds(mouseX, mouseY, originX + CONTACTS_X0, originY + CONTACTS_Y0,
                CONTACTS_X1 - CONTACTS_X0, CONTACTS_Y1 - CONTACTS_Y0)) {
            scrollContacts = clamp(scrollContacts - step, 0, lastContactsOverflow);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KEY_ESCAPE) {
            onBackButtonClicked();
            return true;
        }
        if (keyCode == KEY_UP || keyCode == KEY_DOWN) {
            boolean up = keyCode == KEY_UP;
            if (activeConvId != null) {
                // chat: scrollChat 0 = bottom; Up scrolls toward the top.
                scrollChat = clamp(scrollChat + (up ? SLIDER_STEP : -SLIDER_STEP), 0, lastChatOverflow);
            } else {
                scrollContacts = clamp(scrollContacts + (up ? -SLIDER_STEP : SLIDER_STEP), 0, lastContactsOverflow);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---------------------------------------------------------------- draw helpers

    private void fillRect(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        g.fill(originX + x0, originY + y0, originX + x1, originY + y1, argb);
    }

    private void drawRectOutline(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        g.fill(x0, y0, x1, y0 + 1, argb);
        g.fill(x0, y1 - 1, x1, y1, argb);
        g.fill(x0, y0, x0 + 1, y1, argb);
        g.fill(x1 - 1, y0, x1, y1, argb);
    }

    /** Filled rectangle with beveled (approx. rounded) corners selected by the four flags. */
    private void fillRoundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int argb,
                                 boolean tl, boolean tr, boolean bl, boolean br) {
        for (int row = 0; row < h; row++) {
            int leftInset = 0;
            int rightInset = 0;
            int topDist = row;
            int botDist = h - 1 - row;
            if (tl && topDist < r) {
                leftInset = Math.max(leftInset, r - 1 - topDist);
            }
            if (bl && botDist < r) {
                leftInset = Math.max(leftInset, r - 1 - botDist);
            }
            if (tr && topDist < r) {
                rightInset = Math.max(rightInset, r - 1 - topDist);
            }
            if (br && botDist < r) {
                rightInset = Math.max(rightInset, r - 1 - botDist);
            }
            g.fill(x + leftInset, y + row, x + w - rightInset, y + row + 1, argb);
        }
    }

    private void drawTypingFrame(GuiGraphics g, int x, int y, int frame) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, 1f);
        g.blit(TEX_TYPING, x, y, 0, frame * TYPING_H, TYPING_W, TYPING_H, TYPING_W, TYPING_H * TYPING_FRAMES);
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

    /** Contact-name header: ×2 scale, centered, NO drop shadow. */
    private void drawScaledCentered(GuiGraphics g, Component c, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y - 7f, 0);
        g.pose().scale(2f, 2f, 1f);
        int w = this.font.width(c);
        g.drawString(this.font, c, -w / 2, 0, color, false);
        g.pose().popPose();
    }

    /**
     * Slider with a 1px outline and a proportional knob (cf. §8.2). {@code fraction} is the content
     * position: 0 ⇒ knob at the top (top of content shown), 1 ⇒ knob at the bottom.
     */
    private void drawSlider(GuiGraphics g, int sliderId, int sx0, int sx1, int totalH, int areaH, float fraction) {
        int x0 = originX + sx0;
        int x1 = originX + sx1;
        int y0 = originY + CONTACTS_Y0;
        int y1 = originY + CONTACTS_Y1;
        g.fill(x0 + 1, y0, x1 - 1, y0 + 1, COL_WHITE);
        g.fill(x0 + 1, y1 - 1, x1 - 1, y1, COL_WHITE);
        g.fill(x0, y0 + 1, x0 + 1, y1 - 1, COL_WHITE);
        g.fill(x1 - 1, y0 + 1, x1, y1 - 1, COL_WHITE);

        int trackTop = y0 + 2;
        int trackBottom = y1 - 2;
        int trackH = trackBottom - trackTop;
        int knobH = SLIDER_MAX_KNOB;
        if (totalH > areaH && totalH > 0) {
            knobH = clamp((int) ((long) areaH * trackH / totalH), SLIDER_MIN_KNOB, SLIDER_MAX_KNOB);
        }
        int knobY = trackTop + Math.round((trackH - knobH) * Math.max(0f, Math.min(1f, fraction)));
        int knobX0 = (x0 + x1) / 2 - 2;
        g.fill(knobX0, knobY + 1, knobX0 + 4, knobY + knobH - 1, COL_WHITE);
        g.fill(knobX0 + 1, knobY, knobX0 + 3, knobY + knobH, COL_WHITE);

        if (sliderId == 1) {
            contactsKnobY = knobY;
            contactsKnobH = knobH;
        } else {
            chatKnobY = knobY;
            chatKnobH = knobH;
        }
    }

    // ---------------------------------------------------------------- slider drag

    private boolean inSliderColumn(double mouseX, double mouseY, int sx0, int sx1) {
        return mouseX >= originX + sx0 && mouseX < originX + sx1
                && mouseY >= originY + CONTACTS_Y0 && mouseY < originY + CONTACTS_Y1;
    }

    private void startSliderDrag(int sliderId, int mouseY) {
        int knobY = sliderId == 1 ? contactsKnobY : chatKnobY;
        int knobH = sliderId == 1 ? contactsKnobH : chatKnobH;
        draggingSlider = sliderId;
        dragGrabDy = (mouseY >= knobY && mouseY < knobY + knobH) ? (mouseY - knobY) : knobH / 2;
        applySliderDrag(mouseY);
    }

    private void applySliderDrag(int mouseY) {
        if (draggingSlider == 0) {
            return;
        }
        int knobH = draggingSlider == 1 ? contactsKnobH : chatKnobH;
        int trackTop = originY + CONTACTS_Y0 + 2;
        int trackBottom = originY + CONTACTS_Y1 - 2;
        int denom = (trackBottom - trackTop) - knobH;
        float f = denom <= 0 ? 0f : Math.max(0f, Math.min(1f, (mouseY - dragGrabDy - trackTop) / (float) denom));
        if (draggingSlider == 1) {
            scrollContacts = clamp(Math.round(f * lastContactsOverflow), 0, lastContactsOverflow);
        } else {
            // chat: fraction 0 = top (scrollChat = overflow), 1 = bottom (scrollChat = 0)
            scrollChat = clamp(Math.round((1f - f) * lastChatOverflow), 0, lastChatOverflow);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
