package maxigregrze.cobblesafari.client.donut;



import maxigregrze.cobblesafari.item.donut.DonutBonus;

import maxigregrze.cobblesafari.item.donut.DonutTooltipPayload;

import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.Items;



public record DonutFlavorClientTooltip(DonutTooltipPayload data) implements ClientTooltipComponent {



    private static final int MOB_EFFECT_ICON = 18;

    private static final int GAP = 2;



    @Override

    public int getHeight() {

        int h = DonutTooltipPayload.flavorLineHeight();

        if (!data.shiftDetail()) {

            return h + (data.bonuses().isEmpty() ? 0 : MOB_EFFECT_ICON);

        }

        h += data.bonuses().size() * (MOB_EFFECT_ICON + GAP);

        h += 11;

        if (!data.berryItemIds().isEmpty()) {

            h += 18;

        }

        return h;

    }



    @Override

    public int getWidth(Font font) {

        int max = font.width(data.flavorDescription());

        if (!data.shiftDetail()) {

            if (data.bonuses().isEmpty()) {

                return max;

            }

            return Math.max(max, data.bonuses().size() * (MOB_EFFECT_ICON + GAP) - GAP);

        }

        max = Math.max(max, font.width(caloriesLine()));

        for (DonutBonus b : data.bonuses()) {

            max = Math.max(max, MOB_EFFECT_ICON + 4 + font.width(DonutTooltipPayload.bonusEffectDescription(b)));

        }

        if (!data.berryItemIds().isEmpty()) {

            max = Math.max(max, data.berryItemIds().size() * 18 - 2);

        }

        return max;

    }



    private Component caloriesLine() {

        String fire = new String(Character.toChars(0x1F525));

        return Component.literal(fire + " ")

                .append(Component.literal(String.valueOf(data.calories())))

                .append(Component.literal(" "))

                .append(Component.translatable("tooltip.cobblesafari.donut.unit_cal"));

    }



    @Override

    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {

        int yo = y;

        graphics.drawString(font, data.flavorDescription(), x, yo, 0xFFFFFF, false);

        yo += DonutTooltipPayload.flavorLineHeight();

        if (!data.shiftDetail()) {

            int xo = x;

            for (DonutBonus b : data.bonuses()) {

                ResourceLocation tex = DonutTooltipPayload.bonusTextureLocation(b.powerId(), b.level(), b.type());

                graphics.blit(tex, xo, yo, 0, 0, MOB_EFFECT_ICON, MOB_EFFECT_ICON, MOB_EFFECT_ICON, MOB_EFFECT_ICON);

                xo += MOB_EFFECT_ICON + GAP;

            }

            return;

        }

        for (DonutBonus b : data.bonuses()) {

            ResourceLocation tex = DonutTooltipPayload.bonusTextureLocation(b.powerId(), b.level(), b.type());

            graphics.blit(tex, x, yo, 0, 0, MOB_EFFECT_ICON, MOB_EFFECT_ICON, MOB_EFFECT_ICON, MOB_EFFECT_ICON);

            int textY = yo + (MOB_EFFECT_ICON - font.lineHeight) / 2;

            graphics.drawString(font, DonutTooltipPayload.bonusEffectDescription(b), x + MOB_EFFECT_ICON + 4, textY, 0xA0A0A0, false);

            yo += MOB_EFFECT_ICON + GAP;

        }

        graphics.drawString(font, caloriesLine(), x, yo, 0xA0A0A0, false);

        yo += 11;

        int bx = x;

        for (ResourceLocation id : data.berryItemIds()) {

            var item = BuiltInRegistries.ITEM.get(id);

            if (item != Items.AIR) {

                graphics.renderItem(new ItemStack(item), bx, yo);

                bx += 18;

            }

        }

    }

}


