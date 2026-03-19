package maxigregrze.cobblesafari.item;

import com.cobblemon.mod.common.api.storage.StoreCoordinates;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public abstract class PokemonModifierItem extends Item {
    private final String itemId;
    private final boolean redName;

    protected PokemonModifierItem(Properties properties, String itemId, boolean redName) {
        super(properties);
        this.itemId = itemId;
        this.redName = redName;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        if (!redName) {
            return base;
        }
        return base.copy().withStyle(ChatFormatting.RED);
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
            player.sendSystemMessage(Component.translatable("message.cobblesafari.randomizer.not_owner"));
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
