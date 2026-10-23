package maxigregrze.cobblesafari.item;

import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.pokeball.PokeBall;
import maxigregrze.cobblesafari.block.misc.LostItemBlock;
import maxigregrze.cobblesafari.block.misc.LostItemBlockEntity;
import maxigregrze.cobblesafari.block.misc.LostItemSkin;
import maxigregrze.cobblesafari.init.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Lost Item Template (action plan 193, section 5): sneak + right-click a Lost Item to copy its loot and
 * look settings into the template, right-click another one to paste them. The claim list is never copied.
 */
public class LostItemTemplateItem extends Item {

    /** The skin lives on the block state, so the template carries it next to the block entity config. */
    private static final String NBT_SKIN = "Skin";
    private static final String TOOLTIP = "tooltip.cobblesafari.lost_item_template.";

    public LostItemTemplateItem(Properties properties) {
        super(properties);
    }

    /** Called by the interaction handler for a creative player holding a template on a Lost Item. */
    public static InteractionResult onUseLostItem(Player player, Level level, BlockPos pos, LostItemBlockEntity be, ItemStack stack) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            CompoundTag tag = new CompoundTag();
            be.writeConfig(tag, level.registryAccess());
            tag.putString(NBT_SKIN, be.getSkin().getSerializedName());
            stack.set(ModComponents.LOST_ITEM_TEMPLATE, CustomData.of(tag));
            player.displayClientMessage(Component.translatable("gui.cobblesafari.lost_item.template_saved"), true);
            return InteractionResult.CONSUME;
        }
        CustomData data = stack.get(ModComponents.LOST_ITEM_TEMPLATE);
        if (data == null) {
            player.displayClientMessage(Component.translatable("gui.cobblesafari.lost_item.template_empty"), true);
            return InteractionResult.CONSUME;
        }
        CompoundTag tag = data.copyTag();
        be.readConfig(tag, level.registryAccess());
        LostItemBlock.applySkin(level, pos, LostItemSkin.byName(tag.getString(NBT_SKIN)));
        be.syncConfigToClients();
        player.displayClientMessage(Component.translatable("gui.cobblesafari.lost_item.template_applied"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModComponents.LOST_ITEM_TEMPLATE) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CustomData data = stack.get(ModComponents.LOST_ITEM_TEMPLATE);
        if (data == null) {
            tooltip.add(Component.translatable(TOOLTIP + "empty").withStyle(ChatFormatting.GRAY));
        } else {
            appendConfigLines(data.copyTag(), context.registries(), tooltip);
        }
        tooltip.add(Component.translatable(TOOLTIP + "usage").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void appendConfigLines(CompoundTag tag, HolderLookup.Provider registries, List<Component> tooltip) {
        int mode = tag.getInt(LostItemBlockEntity.NBT_MODE);
        tooltip.add(Component.translatable(TOOLTIP + "mode", mode).withStyle(ChatFormatting.GRAY));
        Component detail = switch (mode) {
            case 1 -> Component.translatable(TOOLTIP + "loot_table", tag.getString(LostItemBlockEntity.NBT_LOST_ITEM_LOOT_TABLE));
            case 2 -> lootItemLine(tag, registries);
            default -> Component.translatable(TOOLTIP + "pools",
                    tag.getInt(LostItemBlockEntity.NBT_MIN_ROLL), tag.getInt(LostItemBlockEntity.NBT_MAX_ROLL));
        };
        tooltip.add(detail.copy().withStyle(ChatFormatting.GRAY));
        LostItemSkin skin = LostItemSkin.byName(tag.getString(NBT_SKIN));
        tooltip.add(Component.translatable(TOOLTIP + "skin",
                Component.translatable("gui.cobblesafari.lost_item.skin." + skin.getSerializedName())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(TOOLTIP + "pokeball", pokeballName(tag.getString(LostItemBlockEntity.NBT_POKEBALL)))
                .withStyle(ChatFormatting.GRAY));
    }

    private static Component lootItemLine(CompoundTag tag, HolderLookup.Provider registries) {
        if (registries != null && tag.contains(LostItemBlockEntity.NBT_LOOT_ITEM_STACK, Tag.TAG_COMPOUND)) {
            ItemStack loot = ItemStack.parse(registries, tag.getCompound(LostItemBlockEntity.NBT_LOOT_ITEM_STACK)).orElse(ItemStack.EMPTY);
            if (!loot.isEmpty()) {
                return Component.translatable(TOOLTIP + "loot_item", loot.getHoverName(), loot.getCount());
            }
        }
        return Component.translatable(TOOLTIP + "loot_item", tag.getString(LostItemBlockEntity.NBT_LOOT_ITEM), 1);
    }

    private static Component pokeballName(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        PokeBall ball = loc != null ? PokeBalls.getPokeBall(loc) : null;
        return ball != null ? ball.item().getDescription() : Component.literal(id);
    }
}
