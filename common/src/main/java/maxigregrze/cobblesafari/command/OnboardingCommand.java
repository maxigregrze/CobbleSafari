package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import maxigregrze.cobblesafari.onboarding.OnboardingService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /cobblesafari onboarding}: opens the admin onboarding screen (action plan 198, section 5). */
public final class OnboardingCommand {

    private OnboardingCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        // Repeats the root's level so the command keeps its own gate if the root's ever changes.
        return Commands.literal("onboarding")
                .requires(source -> source.hasPermission(OnboardingService.PERMISSION_LEVEL))
                .executes(OnboardingCommand::execute);
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
        OnboardingService.open(player);
        return 1;
    }
}
