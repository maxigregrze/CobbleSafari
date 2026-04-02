package maxigregrze.cobblesafari.item.redchainrandom;

import com.cobblemon.mod.common.api.storage.StoreCoordinates;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public abstract class PokemonModifierItem extends Item {

    private static final ResourceLocation UNOWN_FONT = ResourceLocation.fromNamespaceAndPath("cobblesafari", "unown");

    private final String itemId;
    private final boolean redName;
    private final char suffixChar;

    protected PokemonModifierItem(Properties properties, String itemId, boolean redName) {
        this(properties, itemId, redName, '\0');
    }

    protected PokemonModifierItem(Properties properties, String itemId, boolean redName, char suffixChar) {
        super(properties);
        this.itemId = itemId;
        this.redName = redName;
        this.suffixChar = suffixChar;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        if (!redName) {
            return base;
        }
        Component styled = base.copy().withStyle(ChatFormatting.RED);
        if (suffixChar != '\0') {
            Component suffix = Component.literal(String.valueOf(suffixChar))
                    .withStyle(Style.EMPTY.withFont(UNOWN_FONT).withColor(ChatFormatting.RED));
            styled = Component.empty().append(styled).append(suffix);
        }
        return styled;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cobblesafari." + itemId + ".line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cobblesafari." + itemId + ".line2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand usedHand) {
        if (player.level().isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof PokemonEntity pokemonEntity)) {
            return InteractionResult.PASS;
        }

        Pokemon pokemon = pokemonEntity.getPokemon();
        StoreCoordinates storeCoordinates = pokemon.getStoreCoordinates().get();
        if (storeCoordinates == null || storeCoordinates.getStore() == null || !storeCoordinates.getStore().getUuid().equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.not_owner"));
            return InteractionResult.FAIL;
        }

        boolean success = applyToPokemon(player, pokemonEntity, pokemon);
        if (!success) {
            return InteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    protected abstract boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon);
}
