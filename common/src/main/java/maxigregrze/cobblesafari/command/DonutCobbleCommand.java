package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import maxigregrze.cobblesafari.init.ModComponents;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.item.donut.DonutBonus;
import maxigregrze.cobblesafari.item.donut.DonutFlavorComponent;
import maxigregrze.cobblesafari.item.donut.DonutFlavorLogic;
import maxigregrze.cobblesafari.item.donut.DonutMainFlavor;
import maxigregrze.cobblesafari.item.donut.DonutPower;
import maxigregrze.cobblesafari.item.donut.DonutPowerRegistry;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public final class DonutCobbleCommand {

    private static final String ARG_FLAVOR = "flavor";
    private static final String ARG_TIER = "tier";
    private static final String ARG_B1 = "bonus1";
    private static final String ARG_B2 = "bonus2";
    private static final String ARG_B3 = "bonus3";
    private static final String ARG_COUNT = "count";

    private static final String MSG_CUSTOM_INVALID_BONUS = "cobblesafari.command.donut.custom.invalid_bonus";

    private static List<String> allBonusKeys;

    private DonutCobbleCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> donutLiteral() {
        LiteralArgumentBuilder<CommandSourceStack> randomBranch =
                Commands.literal("random")
                        .then(Commands.argument(ARG_FLAVOR, StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        Stream.of(DonutMainFlavor.values())
                                                .filter(f -> f != DonutMainFlavor.MIX)
                                                .map(DonutMainFlavor::getSerializedName),
                                        b))
                                .then(Commands.argument(ARG_TIER, IntegerArgumentType.integer(0, DonutFlavorComponent.MAX_TIER))
                                        .then(Commands.argument(ARG_COUNT, IntegerArgumentType.integer(1, 64))
                                                .executes(DonutCobbleCommand::executeRandom))));
        LiteralArgumentBuilder<CommandSourceStack> customBranch =
                Commands.literal("custom")
                        .then(Commands.argument(ARG_B1, StringArgumentType.word())
                                .suggests(bonusSuggestions(false))
                                .then(Commands.argument(ARG_B2, StringArgumentType.word())
                                        .suggests(bonusSuggestions(true))
                                        .then(Commands.argument(ARG_B3, StringArgumentType.word())
                                                .suggests(bonusSuggestions(true))
                                                .then(Commands.argument(ARG_COUNT, IntegerArgumentType.integer(1, 64))
                                                        .executes(DonutCobbleCommand::executeCustom)))));
        return Commands.literal("donut")
                .requires(source -> source.hasPermission(4))
                .then(randomBranch)
                .then(customBranch);
    }

    private static SuggestionProvider<CommandSourceStack> bonusSuggestions(boolean includeNone) {
        return (context, builder) -> {
            if (allBonusKeys == null) {
                allBonusKeys = buildAllBonusKeys();
            }
            if (includeNone) {
                List<String> merged = new ArrayList<>(allBonusKeys.size() + 1);
                merged.add("none");
                merged.addAll(allBonusKeys);
                return SharedSuggestionProvider.suggest(merged, builder);
            }
            return SharedSuggestionProvider.suggest(allBonusKeys, builder);
        };
    }

    private static List<String> buildAllBonusKeys() {
        List<String> keys = new ArrayList<>();
        for (DonutPower p : DonutPowerRegistry.all()) {
            int typeLimit = p.typeNbr() <= 1 ? 1 : Math.min(p.typeNbr(), PowerVariantRegistry.VARIANT_COUNT);
            for (int lv = 1; lv <= 3; lv++) {
                for (int ti = 0; ti < typeLimit; ti++) {
                    int typeIdx = p.typeNbr() <= 1 ? 0 : ti;
                    DonutBonus b = new DonutBonus(p.id(), lv, typeIdx);
                    if (ModPowerEffects.resolveBonus(b) != null) {
                        keys.add(p.id() + "_" + lv + "_" + typeIdx);
                    }
                }
            }
        }
        Collections.sort(keys);
        return keys;
    }

    private static int executeRandom(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.donut.player_only"));
            return 0;
        }
        String flavorStr = StringArgumentType.getString(ctx, ARG_FLAVOR);
        int tier = IntegerArgumentType.getInteger(ctx, ARG_TIER);
        int count = IntegerArgumentType.getInteger(ctx, ARG_COUNT);
        DonutMainFlavor flavor = parseFlavor(flavorStr);
        if (flavor == null || flavor == DonutMainFlavor.MIX) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.donut.invalid_flavor"));
            return 0;
        }
        ItemStack stack = DonutFlavorLogic.createGeneratedStack(flavor, tier);
        givePlayerDonuts(player, stack, count);
        int givenCount = count;
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.donut.random.success", givenCount), true);
        return 1;
    }

    private static DonutMainFlavor parseFlavor(String s) {
        for (DonutMainFlavor f : DonutMainFlavor.values()) {
            if (f.getSerializedName().equalsIgnoreCase(s)) {
                return f;
            }
        }
        return null;
    }

    private static int executeCustom(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.donut.player_only"));
            return 0;
        }
        String s1 = StringArgumentType.getString(ctx, ARG_B1);
        String s2 = StringArgumentType.getString(ctx, ARG_B2);
        String s3 = StringArgumentType.getString(ctx, ARG_B3);
        int count = IntegerArgumentType.getInteger(ctx, ARG_COUNT);

        Optional<DonutBonus> o1 = DonutFlavorLogic.parseBonusSpec(s1);
        if (o1.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable(MSG_CUSTOM_INVALID_BONUS, 1));
            return 0;
        }
        List<DonutBonus> bonuses = new ArrayList<>();
        bonuses.add(o1.get());

        if (!DonutFlavorLogic.isNoneBonusSlot(s2)) {
            Optional<DonutBonus> o2 = DonutFlavorLogic.parseBonusSpec(s2);
            if (o2.isEmpty()) {
                ctx.getSource().sendFailure(Component.translatable(MSG_CUSTOM_INVALID_BONUS, 2));
                return 0;
            }
            bonuses.add(o2.get());
        }

        if (!DonutFlavorLogic.isNoneBonusSlot(s3)) {
            Optional<DonutBonus> o3 = DonutFlavorLogic.parseBonusSpec(s3);
            if (o3.isEmpty()) {
                ctx.getSource().sendFailure(Component.translatable(MSG_CUSTOM_INVALID_BONUS, 3));
                return 0;
            }
            bonuses.add(o3.get());
        }

        DonutFlavorComponent comp;
        try {
            comp = DonutFlavorLogic.fromCustomBonuses(bonuses, new Random());
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            if ("duplicate_flavor_category".equals(msg)) {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.donut.custom.duplicate_category"));
            } else if ("invalid_level_sum".equals(msg)) {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.donut.custom.invalid_sum"));
            } else {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.donut.custom.failed"));
            }
            return 0;
        }

        ItemStack stack = new ItemStack(ModItems.DONUT);
        stack.set(ModComponents.DONUT_FLAVOR, comp);
        givePlayerDonuts(player, stack, count);
        int givenCount = count;
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.donut.custom.success", givenCount), true);
        return 1;
    }

    private static void givePlayerDonuts(ServerPlayer player, ItemStack template, int count) {
        int max = template.getMaxStackSize();
        int left = count;
        while (left > 0) {
            int n = Math.min(left, max);
            ItemStack s = template.copy();
            s.setCount(n);
            if (!player.getInventory().add(s)) {
                player.drop(s, false);
            }
            left -= n;
        }
    }
}
