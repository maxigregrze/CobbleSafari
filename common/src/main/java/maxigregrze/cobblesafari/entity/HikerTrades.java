package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class HikerTrades {

    private HikerTrades() {}

    public static List<MerchantOffer> getTradesForType(String tradeType, RandomSource random) {
        return switch (tradeType.toLowerCase()) {
            case "large" -> generateLargeTrades(random);
            case "treasure" -> generateTreasureTrades(random);
            default -> generateSmallTrades(random);
        };
    }
    
    private static Item getCobblemonItem(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("cobblemon", name);
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || item == Items.AIR) {
            CobbleSafari.LOGGER.warn("Cobblemon item not found: {}", name);
            return null;
        }
        return item;
    }

    private static final ResourceLocation AIR_KEY = BuiltInRegistries.ITEM.getDefaultKey();

    private static boolean isInvalidItem(Item item) {
        if (item == null || item == Items.AIR) return true;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return AIR_KEY.equals(key);
    }

    private static boolean isValidOffer(MerchantOffer offer) {
        if (offer == null) return false;
        try {
            ItemStack result = offer.getResult();
            if (result == null || result.isEmpty() || result.is(Items.AIR)) return false;
            ResourceLocation resultKey = BuiltInRegistries.ITEM.getKey(result.getItem());
            return !AIR_KEY.equals(resultKey);
        } catch (Exception e) {
            return false;
        }
    }

    private static List<MerchantOffer> generateSmallTrades(RandomSource random) {
        List<MerchantOffer> allPossibleTrades = new ArrayList<>();
        
        allPossibleTrades.add(createTrade(
                ModItems.SPHERE_GREEN_L, 1, 16,
                ModItems.SPHERE_GREEN_S, 1, ModItems.SPHERE_PALE_S, 1,
                random, 12, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                ModItems.PERFUME_UNCOMMON, 1, 1,
                ModItems.SPHERE_GREEN_S, 16, ModItems.SPHERE_PRISM_L, 16,
                random, 4, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("relic_coin"), 1, 1,
                ModItems.SPHERE_RED_S, 4, ModItems.SPHERE_BLUE_S, 4,
                random, 8, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                ModItems.HEART_SCALE, 1, 1,
                ModItems.SPHERE_RED_S, 16, ModItems.SPHERE_BLUE_S, 16,
                random, 8, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                ModItems.STAR_PIECE, 1, 1,
                ModItems.SPHERE_PALE_S, 16, ModItems.SPHERE_PRISM_S, 16,
                random, 8, 0, 0.0f));
        
        addCobblemonTrade(allPossibleTrades, "tumblestone",
                ModItems.SPHERE_RED_S, 16, 12);

        addCobblemonTrade(allPossibleTrades, "black_tumblestone",
                ModItems.SPHERE_GREEN_S, 16, 12);

        addCobblemonTrade(allPossibleTrades, "sky_tumblestone",
                ModItems.SPHERE_BLUE_S, 16, 12);

        addCobblemonTrade(allPossibleTrades, "smooth_rock",
                ModItems.SPHERE_GREEN_S, 32, 8);

        addCobblemonTrade(allPossibleTrades, "damp_rock",
                ModItems.SPHERE_BLUE_S, 32, 8);

        addCobblemonTrade(allPossibleTrades, "heat_rock",
                ModItems.SPHERE_GREEN_S, 32, 8);
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("icy_rock"), 1, 1,
                ModItems.SPHERE_BLUE_S, 16, ModItems.SPHERE_RED_S, 16,
                random, 8, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("chipped_pot"), 1, 1,
                ModItems.SPHERE_RED_L, 10, ModItems.SPHERE_GREEN_L, 6,
                random, 4, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("masterpiece_teacup"), 1, 1,
                ModItems.SPHERE_GREEN_S, 6, ModItems.SPHERE_BLUE_S, 10,
                random, 4, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_RED_L, 1),
                new ItemStack(ModItems.SPHERE_RED_S, 60),
                8, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_GREEN_L, 1),
                new ItemStack(ModItems.SPHERE_GREEN_S, 60),
                8, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_BLUE_L, 1),
                new ItemStack(ModItems.SPHERE_BLUE_S, 60),
                8, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_PRISM_L, 1),
                new ItemStack(ModItems.SPHERE_PRISM_S, 60),
                8, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_PALE_L, 1),
                new ItemStack(ModItems.SPHERE_PALE_S, 60),
                8, 0, 0.0f));

        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_PRISM_S, 8),
                Optional.of(new ItemCost(ModItems.SPHERE_PALE_S, 8)),
                new ItemStack(ModItems.LUCKY_MINING_HELMET, 1),
                4, 0, 0.0f));
        
        allPossibleTrades.removeIf(offer -> offer == null || !isValidOffer(offer));
        Collections.shuffle(allPossibleTrades, new Random(random.nextLong()));
        return new ArrayList<>(allPossibleTrades.subList(0, Math.min(5 + random.nextInt(4), allPossibleTrades.size())));
    }

    private static void addCobblemonTrade(List<MerchantOffer> trades, String cobblemonItemName,
                                          Item costItem, int costCount, int maxUses) {
        Item result = getCobblemonItem(cobblemonItemName);
        if (!isInvalidItem(result)) {
            trades.add(new MerchantOffer(
                    new ItemCost(costItem, costCount),
                    new ItemStack(result, 1),
                    maxUses, 0, 0.0f));
        }
    }

    private static List<MerchantOffer> generateLargeTrades(RandomSource random) {
        List<MerchantOffer> allPossibleTrades = new ArrayList<>();
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("cherish_ball"), 1, 1,
                ModItems.SPHERE_PALE_L, 50, ModItems.SPHERE_PRISM_L, 50,
                random, 2, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                ModItems.SUPER_SHINY_INCENSE, 1, 1,
                ModItems.SPHERE_PALE_L, 20, ModItems.SPHERE_BLUE_L, 20,
                random, 4, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                ModItems.ULTRA_SHINY_INCENSE, 1, 1,
                ModItems.SPHERE_PALE_L, 40, ModItems.SPHERE_BLUE_L, 40,
                random, 2, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                ModItems.PERFUME_RARE, 1, 1,
                ModItems.SPHERE_RED_L, 20, ModItems.SPHERE_PRISM_L, 20,
                random, 4, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                ModItems.PERFUME_ULTRARARE, 1, 1,
                ModItems.SPHERE_RED_L, 40, ModItems.SPHERE_PRISM_L, 40,
                random, 2, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("relic_coin_pouch"), 1, 1,
                ModItems.SPHERE_RED_L, 4, ModItems.SPHERE_BLUE_L, 4,
                random, 8, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("max_revive"), 1, 1,
                ModItems.SPHERE_GREEN_L, 9, ModItems.SPHERE_PRISM_L, 7,
                random, 6, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("rare_candy"), 1, 1,
                ModItems.SPHERE_GREEN_L, 9, ModItems.SPHERE_RED_L, 7,
                random, 6, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("ability_capsule"), 1, 1,
                ModItems.SPHERE_GREEN_L, 9, ModItems.SPHERE_BLUE_L, 7,
                random, 4, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("ability_patch"), 1, 1,
                ModItems.SPHERE_GREEN_L, 9, ModItems.SPHERE_PALE_L, 7,
                random, 4, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("chipped_pot"), 1, 1,
                ModItems.SPHERE_RED_L, 10, ModItems.SPHERE_GREEN_L, 6,
                random, 4, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("masterpiece_teacup"), 1, 1,
                ModItems.SPHERE_GREEN_L, 6, ModItems.SPHERE_BLUE_L, 10,
                random, 4, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("auspicious_armor"), 1, 1,
                ModItems.SPHERE_RED_L, 8, ModItems.SPHERE_GREEN_L, 8,
                random, 2, 0, 0.0f));
        
        allPossibleTrades.add(createTrade(
                getCobblemonItem("malicious_armor"), 1, 1,
                ModItems.SPHERE_GREEN_L, 8, ModItems.SPHERE_BLUE_L, 8,
                random, 2, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_RED_S, 32),
                new ItemStack(ModItems.SPHERE_RED_L, 1),
                12, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_GREEN_S, 32),
                new ItemStack(ModItems.SPHERE_GREEN_L, 1),
                12, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_BLUE_S, 32),
                new ItemStack(ModItems.SPHERE_BLUE_L, 1),
                12, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_PRISM_S, 32),
                new ItemStack(ModItems.SPHERE_PRISM_L, 1),
                12, 0, 0.0f));
        
        allPossibleTrades.add(new MerchantOffer(
                new ItemCost(ModItems.SPHERE_PALE_S, 32),
                new ItemStack(ModItems.SPHERE_PALE_L, 1),
                12, 0, 0.0f));
        
        allPossibleTrades.removeIf(offer -> offer == null || !isValidOffer(offer));
        Collections.shuffle(allPossibleTrades, new Random(random.nextLong()));
        return new ArrayList<>(allPossibleTrades.subList(0, Math.min(5 + random.nextInt(4), allPossibleTrades.size())));
    }

    private static List<MerchantOffer> generateTreasureTrades(RandomSource random) {
        List<TradeOption> tradeOptions = new ArrayList<>();
        
        tradeOptions.add(new TradeOption(getCobblemonItem("helix_fossil"), 
                ModItems.SPHERE_BLUE_L, 16, ModItems.SPHERE_PRISM_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("dome_fossil"), 
                ModItems.SPHERE_RED_L, 16, ModItems.SPHERE_PRISM_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("old_amber"), 
                ModItems.SPHERE_GREEN_L, 16, ModItems.SPHERE_PALE_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("root_fossil"), 
                ModItems.SPHERE_BLUE_L, 16, ModItems.SPHERE_PRISM_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("claw_fossil"), 
                ModItems.SPHERE_RED_L, 16, ModItems.SPHERE_PRISM_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("skull_fossil"), 
                ModItems.SPHERE_BLUE_L, 16, ModItems.SPHERE_PALE_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("armor_fossil"), 
                ModItems.SPHERE_RED_L, 16, ModItems.SPHERE_PALE_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("cover_fossil"), 
                ModItems.SPHERE_BLUE_L, 16, ModItems.SPHERE_PRISM_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("plume_fossil"), 
                ModItems.SPHERE_RED_L, 16, ModItems.SPHERE_PRISM_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("jaw_fossil"), 
                ModItems.SPHERE_BLUE_L, 16, ModItems.SPHERE_PALE_L, 8));
        tradeOptions.add(new TradeOption(getCobblemonItem("sail_fossil"), 
                ModItems.SPHERE_RED_L, 16, ModItems.SPHERE_PALE_L, 8));
        
        tradeOptions.add(new TradeOption(ModItems.FOSSIL_RANDOM, 
                ModItems.SPHERE_PALE_S, 64, ModItems.SPHERE_PRISM_S, 64));
        tradeOptions.add(new TradeOption(ModItems.FOSSIL_HIDDEN_ABILITY, 
                ModItems.SPHERE_PALE_L, 10, ModItems.SPHERE_PRISM_L, 10));
        tradeOptions.add(new TradeOption(ModItems.FOSSIL_IV_MAX, 
                ModItems.SPHERE_PALE_L, 10, ModItems.SPHERE_PRISM_L, 10));
        tradeOptions.add(new TradeOption(ModItems.FOSSIL_SHINY, 
                ModItems.SPHERE_PALE_L, 10, ModItems.SPHERE_PRISM_L, 10));
        tradeOptions.add(new TradeOption(ModItems.FOSSIL_PERFECT, 
                ModItems.SPHERE_PALE_L, 32, ModItems.SPHERE_PRISM_L, 32));
        
        tradeOptions.add(new TradeOption(ModItems.STAR_PIECE, 
                ModItems.SPHERE_PALE_S, 8, ModItems.SPHERE_PRISM_S, 8));
        tradeOptions.add(new TradeOption(ModItems.HEART_SCALE, 
                ModItems.SPHERE_BLUE_S, 8, ModItems.SPHERE_RED_S, 8));
        
        tradeOptions.add(new TradeOption(ModItems.FLAG_REGULAR, 
                ModItems.SPHERE_BLUE_S, 8, ModItems.SPHERE_RED_S, 8));
        tradeOptions.add(new TradeOption(ModItems.FLAG_BRONZE, 
                ModItems.SPHERE_GREEN_S, 4, ModItems.SPHERE_GREEN_L, 4));
        tradeOptions.add(new TradeOption(ModItems.FLAG_SILVER, 
                ModItems.SPHERE_BLUE_L, 8, ModItems.SPHERE_RED_L, 8));
        tradeOptions.add(new TradeOption(ModItems.FLAG_GOLD, 
                ModItems.SPHERE_PALE_S, 4, ModItems.SPHERE_PRISM_S, 4));
        tradeOptions.add(new TradeOption(ModItems.FLAG_PLATINUM, 
                ModItems.SPHERE_PALE_L, 4, ModItems.SPHERE_PRISM_L, 4));
        
        tradeOptions.add(new TradeOption(getCobblemonItem("relic_coin"), 
                ModItems.SPHERE_BLUE_S, 2, ModItems.SPHERE_RED_S, 2));
        tradeOptions.add(new TradeOption(getCobblemonItem("revive"), 
                ModItems.SPHERE_PALE_S, 16, ModItems.SPHERE_PRISM_S, 16));
        tradeOptions.add(new TradeOption(getCobblemonItem("max_revive"), 
                ModItems.SPHERE_PALE_S, 32, ModItems.SPHERE_PRISM_S, 32));
        
        tradeOptions.removeIf(option ->
                isInvalidItem(option.itemToSell) || isInvalidItem(option.priceItem1) || isInvalidItem(option.priceItem2));
        Collections.shuffle(tradeOptions, new Random(random.nextLong()));
        
        List<MerchantOffer> trades = new ArrayList<>();
        int numTrades = 9;
        for (int i = 0; i < Math.min(numTrades, tradeOptions.size()); i++) {
            TradeOption option = tradeOptions.get(i);
            boolean useFirstPrice = random.nextBoolean();
            
            if (useFirstPrice) {
                trades.add(new MerchantOffer(
                        new ItemCost(option.itemToSell, 1),
                        new ItemStack(option.priceItem1, option.priceCount1),
                        8, 0, 0.0f));
            } else {
                trades.add(new MerchantOffer(
                        new ItemCost(option.itemToSell, 1),
                        new ItemStack(option.priceItem2, option.priceCount2),
                        8, 0, 0.0f));
            }
        }
        
        trades.removeIf(offer -> !isValidOffer(offer));
        return trades;
    }
    
    private static MerchantOffer createTrade(Item result, int resultMin, int resultMax,
                                            Item price1, int price1Count, Item price2, int price2Count,
                                            RandomSource random, int maxUses, int xp, float priceMultiplier) {
        if (isInvalidItem(result) || isInvalidItem(price1) || isInvalidItem(price2)) {
            return null;
        }

        int resultCount = resultMin + (resultMax > resultMin ? random.nextInt(resultMax - resultMin + 1) : 0);
        
        int adjustedPrice1 = price1Count;
        int adjustedPrice2 = price2Count;
        
        if (random.nextBoolean()) {
            int shift = random.nextInt(3);
            adjustedPrice1 -= shift;
            adjustedPrice2 += shift;
        }
        
        return new MerchantOffer(
                new ItemCost(price1, Math.max(1, adjustedPrice1)),
                Optional.of(new ItemCost(price2, Math.max(1, adjustedPrice2))),
                new ItemStack(result, resultCount),
                maxUses, xp, priceMultiplier);
    }
    
    private static class TradeOption {
        final Item itemToSell;
        final Item priceItem1;
        final int priceCount1;
        final Item priceItem2;
        final int priceCount2;
        
        TradeOption(Item itemToSell, Item priceItem1, int priceCount1, Item priceItem2, int priceCount2) {
            this.itemToSell = itemToSell;
            this.priceItem1 = priceItem1;
            this.priceCount1 = priceCount1;
            this.priceItem2 = priceItem2;
            this.priceCount2 = priceCount2;
        }
    }
}
