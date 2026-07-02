package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client chat app result. Polymorphic by {@code kind}:
 * a full per-player transcript snapshot for one conversation, or an error key.
 *
 * <p>The snapshot carries <em>every</em> step from 0 up to the current step (so the whole history
 * stays visible on screen, scrollable); only the current step is partial.
 */
public record ChatAppResultPayload(int kind, StateData state, String errorKey) implements CustomPacketPayload {

    public static final int KIND_STATE = 0;
    public static final int KIND_ERROR = 1;

    public record StateData(String convId, int currentStepIndex, int phase, boolean claimed,
                            List<StepView> steps) {}

    /**
     * One step's render data. For steps before the current one everything is fully shown and done.
     * For the current step, {@code beforeShown}/{@code afterShown}/{@code taskVisible} reflect the
     * persisted position and {@code progressNum/Den/done} the live task progress.
     * {@code progressDen == 0} = binary bar, no counter text (mono-criterion advancement).
     */
    public record StepView(List<String> before, List<String> after, String taskTitleKey,
                           int progressNum, int progressDen, boolean done,
                           boolean rewardItems, boolean rewardTrade,
                           int beforeShown, int afterShown, boolean taskVisible, boolean current,
                           boolean failed) {}

    public static final CustomPacketPayload.Type<ChatAppResultPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "chat_app_result"));

    public static final StreamCodec<FriendlyByteBuf, ChatAppResultPayload> STREAM_CODEC =
            StreamCodec.of(ChatAppResultPayload::write, ChatAppResultPayload::read);

    public static ChatAppResultPayload error(String errorKey) {
        return new ChatAppResultPayload(KIND_ERROR, null, errorKey == null ? "" : errorKey);
    }

    public static ChatAppResultPayload state(StateData state) {
        return new ChatAppResultPayload(KIND_STATE, state, "");
    }

    private static void write(FriendlyByteBuf buf, ChatAppResultPayload p) {
        buf.writeVarInt(p.kind);
        if (p.kind == KIND_STATE) {
            StateData s = p.state;
            buf.writeUtf(s.convId());
            buf.writeVarInt(s.currentStepIndex());
            buf.writeVarInt(s.phase());
            buf.writeBoolean(s.claimed());
            buf.writeVarInt(s.steps().size());
            for (StepView v : s.steps()) {
                writeStrings(buf, v.before());
                writeStrings(buf, v.after());
                buf.writeUtf(v.taskTitleKey() == null ? "" : v.taskTitleKey());
                buf.writeVarInt(v.progressNum());
                buf.writeVarInt(v.progressDen());
                buf.writeBoolean(v.done());
                buf.writeBoolean(v.rewardItems());
                buf.writeBoolean(v.rewardTrade());
                buf.writeVarInt(v.beforeShown());
                buf.writeVarInt(v.afterShown());
                buf.writeBoolean(v.taskVisible());
                buf.writeBoolean(v.current());
                buf.writeBoolean(v.failed());
            }
        } else {
            buf.writeUtf(p.errorKey == null ? "" : p.errorKey);
        }
    }

    private static ChatAppResultPayload read(FriendlyByteBuf buf) {
        int kind = buf.readVarInt();
        if (kind == KIND_STATE) {
            String convId = buf.readUtf();
            int currentStep = buf.readVarInt();
            int phase = buf.readVarInt();
            boolean claimed = buf.readBoolean();
            int n = buf.readVarInt();
            List<StepView> steps = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                List<String> before = readStrings(buf);
                List<String> after = readStrings(buf);
                String title = buf.readUtf();
                int num = buf.readVarInt();
                int den = buf.readVarInt();
                boolean done = buf.readBoolean();
                boolean rItems = buf.readBoolean();
                boolean rTrade = buf.readBoolean();
                int beforeShown = buf.readVarInt();
                int afterShown = buf.readVarInt();
                boolean taskVisible = buf.readBoolean();
                boolean current = buf.readBoolean();
                boolean failed = buf.readBoolean();
                steps.add(new StepView(before, after, title, num, den, done, rItems, rTrade,
                        beforeShown, afterShown, taskVisible, current, failed));
            }
            return state(new StateData(convId, currentStep, phase, claimed, steps));
        }
        return error(buf.readUtf());
    }

    private static void writeStrings(FriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) {
            buf.writeUtf(s);
        }
    }

    private static List<String> readStrings(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(buf.readUtf());
        }
        return out;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
