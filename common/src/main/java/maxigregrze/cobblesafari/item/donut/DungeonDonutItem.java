package maxigregrze.cobblesafari.item.donut;

import com.cobblemon.mod.common.api.storage.StoreCoordinates;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DungeonDonutItem extends Item {

    private static final ResourceLocation HOOPA_SPECIES = ResourceLocation.fromNamespaceAndPath("cobblemon", "hoopa");

    @Nullable
    private final String portalDungeonId;

    public DungeonDonutItem(Properties properties, @Nullable String portalDungeonId) {
        super(properties);
        this.portalDungeonId = portalDungeonId;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = key.getPath();
        if (!path.startsWith("donut_")) {
            tooltip.add(Component.translatable("tooltip.cobblesafari." + path).withStyle(ChatFormatting.GRAY));
            return;
        }
        String dimensionPath = path.substring("donut_".length());
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip.cobblesafari." + path).withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("tooltip.cobblesafari.portal." + dimensionPath).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand usedHand) {
        if (player.level().isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof PokemonEntity pokemonEntity)) {
            return InteractionResult.PASS;
        }

        Pokemon pokemon = pokemonEntity.getPokemon();
        StoreCoordinates storeCoordinates = pokemon.getStoreCoordinates().get();
        if (storeCoordinates == null || storeCoordinates.getStore() == null
                || !storeCoordinates.getStore().getUuid().equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.not_owner"));
            return InteractionResult.FAIL;
        }

        if (pokemon.isFull()) {
            return InteractionResult.FAIL;
        }

        if (!HOOPA_SPECIES.equals(pokemon.getSpecies().getResourceIdentifier())) {
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon_donut.not_hoopa"));
            return InteractionResult.FAIL;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        String resolvedId = resolveDungeonIdForSpawn(portalDungeonId);
        boolean success = PortalSpawnManager.spawnPortalNearPlayer(serverPlayer, false, resolvedId);
        if (!success) {
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon_donut.portal_error"));
            return InteractionResult.FAIL;
        }

        if (resolvedId != null) {
            player.sendSystemMessage(Component.translatable("cobblesafari.command.dungeon.spawn.success_destination",
                    player.getName().getString(), resolvedId));
        } else {
            player.sendSystemMessage(Component.translatable("cobblesafari.command.dungeon.spawn.success",
                    player.getName().getString()));
        }

        
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        pokemon.feedPokemon(1, true);
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static String resolveDungeonIdForSpawn(@Nullable String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        String trimmed = configured.trim();
        if (!trimmed.contains(":")) {
            return trimmed;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(trimmed);
        if (parsed != null && CobbleSafari.MOD_ID.equals(parsed.getNamespace())) {
            return parsed.getPath();
        }
        return trimmed;
    }
}
