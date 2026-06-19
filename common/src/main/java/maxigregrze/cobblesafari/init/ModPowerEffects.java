package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.effect.BasicStatusEffect;
import maxigregrze.cobblesafari.effect.LuckPowerEffect;
import maxigregrze.cobblesafari.item.donut.DonutBonus;
import maxigregrze.cobblesafari.item.donut.DonutPower;
import maxigregrze.cobblesafari.item.donut.DonutPowerRegistry;
import maxigregrze.cobblesafari.power.ItemCategoryVariantRegistry;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class ModPowerEffects {

    private static final Holder<MobEffect>[] ALPHA = createHolderArray(3);
    private static final Holder<MobEffect>[] FRIENDSHIP = createHolderArray(3);
    private static final Holder<MobEffect>[][] HIDDEN = createTypedMatrix();
    private static final Holder<MobEffect>[][] ATYPICAL = createTypedMatrix();
    private static final Holder<MobEffect>[][] SPARKLING = createTypedMatrix();
    private static final Holder<MobEffect>[][] CAPTURE = createTypedMatrix();
    private static final Holder<MobEffect>[][] ENCOUNTER = createTypedMatrix();
    private static final Holder<MobEffect>[][] SALVAGE = createTypedMatrix();
    private static final Holder<MobEffect>[] HUMONGO = createHolderArray(3);
    private static final Holder<MobEffect>[] LUCK = createHolderArray(3);
    private static final Holder<MobEffect>[] TEENSY = createHolderArray(3);
    private static final Holder<MobEffect>[] ATTACK = createHolderArray(3);
    private static final Holder<MobEffect>[] DEFENSE = createHolderArray(3);
    private static final Holder<MobEffect>[] SP_ATK = createHolderArray(3);
    private static final Holder<MobEffect>[] SP_DEF = createHolderArray(3);
    private static final Holder<MobEffect>[] SPEED = createHolderArray(3);
    private static final Holder<MobEffect>[][] MOVE = createTypedMatrix();
    private static final Holder<MobEffect>[][] RESISTANCE = createTypedMatrix();
    private static final Holder<MobEffect>[][] SELF_ATTACK = createTypedMatrix();
    private static final Holder<MobEffect>[][] SELF_DEFENSE = createTypedMatrix();
    private static final Holder<MobEffect>[] BIG_HAUL = createHolderArray(3);
    private static final Holder<MobEffect>[][] ITEM = createItemMatrix();

    private static List<Holder<MobEffect>> donutPowerEffectBuffer;
    private static Set<Holder<MobEffect>> donutPowerEffects = Set.of();

    private ModPowerEffects() {}

    @SuppressWarnings("unchecked")
    private static Holder<MobEffect>[] createHolderArray(int size) {
        return (Holder<MobEffect>[]) new Holder<?>[size];
    }

    @SuppressWarnings("unchecked")
    private static Holder<MobEffect>[][] createTypedMatrix() {
        Holder<MobEffect>[][] m = (Holder<MobEffect>[][]) new Holder<?>[PowerVariantRegistry.VARIANT_COUNT][];
        for (int i = 0; i < PowerVariantRegistry.VARIANT_COUNT; i++) {
            m[i] = (Holder<MobEffect>[]) new Holder<?>[3];
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Holder<MobEffect>[][] createItemMatrix() {
        Holder<MobEffect>[][] m = (Holder<MobEffect>[][]) new Holder<?>[ItemCategoryVariantRegistry.COUNT][];
        for (int i = 0; i < ItemCategoryVariantRegistry.COUNT; i++) {
            m[i] = (Holder<MobEffect>[]) new Holder<?>[3];
        }
        return m;
    }

    public static void register() {
        donutPowerEffectBuffer = new ArrayList<>();
        for (int lv = 1; lv <= 3; lv++) {
            ALPHA[lv - 1] = registerOne("alpha_" + lv, 0xE8D0A8);
            FRIENDSHIP[lv - 1] = registerOne("friendship_" + lv, 0xFFB7C5);
        }

        registerTypedFamily("hidden", 0x7B68EE, HIDDEN);
        registerTypedFamily("atypical", 0x98FB98, ATYPICAL);
        registerTypedFamily("sparkling", 0xFFE066, SPARKLING);
        registerTypedFamily("capture", 0x87CEEB, CAPTURE);
        registerTypedFamily("encounter", 0x90EE90, ENCOUNTER);
        registerTypedFamily("salvage", 0xCD853F, SALVAGE);

        for (int lv = 1; lv <= 3; lv++) {
            HUMONGO[lv - 1] = registerOne("humongo_" + lv, 0xFF6B6B);
            TEENSY[lv - 1] = registerOne("teensy_" + lv, 0xB0E0E6);
            ATTACK[lv - 1] = registerOne("attack_" + lv, 0xCC4444);
            DEFENSE[lv - 1] = registerOne("defense_" + lv, 0x4466CC);
            SP_ATK[lv - 1] = registerOne("sp_atk_" + lv, 0xE06633);
            SP_DEF[lv - 1] = registerOne("sp_def_" + lv, 0x6644AA);
            SPEED[lv - 1] = registerOne("speed_" + lv, 0x44AA66);
            BIG_HAUL[lv - 1] = registerOne("big_haul_" + lv, 0xC9A227);
        }

        for (int ci = 0; ci < ItemCategoryVariantRegistry.COUNT; ci++) {
            String suf = ItemCategoryVariantRegistry.suffix(ci);
            for (int lv = 1; lv <= 3; lv++) {
                ITEM[ci][lv - 1] = registerOne("item_" + suf + "_" + lv, 0x66BB6A);
            }
        }

        registerTypedFamily("move", 0xFFAA44, MOVE);
        registerTypedFamily("resistance", 0x6699CC, RESISTANCE);
        registerTypedFamily("self_attack", 0xCC5533, SELF_ATTACK);
        registerTypedFamily("self_defense", 0x557799, SELF_DEFENSE);

        var es = SpawnBoostConfig.data.effectSettings;
        LUCK[0] = registerLuck("luck_1", 0x3CB371, es.luckPowerLevel1LuckBonus, "luck_power_1");
        LUCK[1] = registerLuck("luck_2", 0x2E8B57, es.luckPowerLevel2LuckBonus, "luck_power_2");
        LUCK[2] = registerLuck("luck_3", 0x228B22, es.luckPowerLevel3LuckBonus, "luck_power_3");
        donutPowerEffects = Set.copyOf(donutPowerEffectBuffer);
        donutPowerEffectBuffer = null;
    }

    public static boolean isDonutPowerEffect(Holder<MobEffect> holder) {
        return donutPowerEffects.contains(holder);
    }

    public static void removeAllDonutPowerEffects(LivingEntity entity) {
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance inst : entity.getActiveEffects()) {
            Holder<MobEffect> h = inst.getEffect();
            if (isDonutPowerEffect(h)) {
                toRemove.add(h);
            }
        }
        for (Holder<MobEffect> h : toRemove) {
            entity.removeEffect(h);
        }
    }

    private static void registerTypedFamily(String family, int color, Holder<MobEffect>[][] matrix) {
        for (int vi = 0; vi < PowerVariantRegistry.VARIANT_COUNT; vi++) {
            String suf = PowerVariantRegistry.suffix(vi);
            for (int lv = 1; lv <= 3; lv++) {
                matrix[vi][lv - 1] = registerOne(family + "_" + suf + "_" + lv, color);
            }
        }
    }

    private static Holder<MobEffect> registerOne(String id, int color) {
        MobEffect effect = Registry.register(
                BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, id),
                new BasicStatusEffect(MobEffectCategory.BENEFICIAL, color));
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        Collections.addAll(donutPowerEffectBuffer, holder);
        return holder;
    }

    private static Holder<MobEffect> registerLuck(String id, int color, double luckBonus, String modifierPath) {
        MobEffect effect = Registry.register(
                BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, id),
                new LuckPowerEffect(color, luckBonus, modifierPath));
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        Collections.addAll(donutPowerEffectBuffer, holder);
        return holder;
    }

    public static Holder<MobEffect> alpha(int level) {
        return ALPHA[level - 1];
    }

    public static Holder<MobEffect> friendship(int level) {
        return FRIENDSHIP[level - 1];
    }

    public static Holder<MobEffect> hidden(int variantIndex, int level) {
        return HIDDEN[variantIndex][level - 1];
    }

    public static Holder<MobEffect> atypical(int variantIndex, int level) {
        return ATYPICAL[variantIndex][level - 1];
    }

    public static Holder<MobEffect> sparkling(int variantIndex, int level) {
        return SPARKLING[variantIndex][level - 1];
    }

    public static Holder<MobEffect> capture(int variantIndex, int level) {
        return CAPTURE[variantIndex][level - 1];
    }

    public static Holder<MobEffect> encounter(int variantIndex, int level) {
        return ENCOUNTER[variantIndex][level - 1];
    }

    public static Holder<MobEffect> salvage(int variantIndex, int level) {
        return SALVAGE[variantIndex][level - 1];
    }

    public static Holder<MobEffect> humongo(int level) {
        return HUMONGO[level - 1];
    }

    public static Holder<MobEffect> luck(int level) {
        return LUCK[level - 1];
    }

    public static Holder<MobEffect> teensy(int level) {
        return TEENSY[level - 1];
    }

    public static Holder<MobEffect> attack(int level) {
        return ATTACK[level - 1];
    }

    public static Holder<MobEffect> defense(int level) {
        return DEFENSE[level - 1];
    }

    public static Holder<MobEffect> spAtk(int level) {
        return SP_ATK[level - 1];
    }

    public static Holder<MobEffect> spDef(int level) {
        return SP_DEF[level - 1];
    }

    public static Holder<MobEffect> speed(int level) {
        return SPEED[level - 1];
    }

    public static Holder<MobEffect> move(int variantIndex, int level) {
        return MOVE[variantIndex][level - 1];
    }

    public static Holder<MobEffect> resistance(int variantIndex, int level) {
        return RESISTANCE[variantIndex][level - 1];
    }

    public static Holder<MobEffect> selfAttack(int variantIndex, int level) {
        return SELF_ATTACK[variantIndex][level - 1];
    }

    public static Holder<MobEffect> selfDefense(int variantIndex, int level) {
        return SELF_DEFENSE[variantIndex][level - 1];
    }

    public static Holder<MobEffect> bigHaul(int level) {
        return BIG_HAUL[level - 1];
    }

    public static Holder<MobEffect> item(int categoryIndex, int level) {
        return ITEM[categoryIndex][level - 1];
    }

    public static Holder<MobEffect> resolveBonus(DonutBonus bonus) {
        if (bonus == null) {
            return null;
        }
        int lv = bonus.level();
        if (lv < 1 || lv > 3) {
            return null;
        }
        DonutPower power = DonutPowerRegistry.get(bonus.powerId());
        if (power == null) {
            return null;
        }
        int typeNbr = power.typeNbr();
        int typeIdx = bonus.type();
        if (typeNbr <= 1) {
            typeIdx = 0;
        } else if (typeIdx < 0 || typeIdx >= typeNbr) {
            return null;
        }
        return switch (bonus.powerId()) {
            case "alpha" -> alpha(lv);
            case "friendship" -> friendship(lv);
            case "hidden" -> hidden(typeIdx, lv);
            case "atypical" -> atypical(typeIdx, lv);
            case "sparkling" -> sparkling(typeIdx, lv);
            case "capture" -> capture(typeIdx, lv);
            case "encounter" -> encounter(typeIdx, lv);
            case "humongo" -> humongo(lv);
            case "luck" -> luck(lv);
            case "salvage" -> salvage(typeIdx, lv);
            case "teensy" -> teensy(lv);
            case "attack" -> attack(lv);
            case "defense" -> defense(lv);
            case "sp_atk" -> spAtk(lv);
            case "sp_def" -> spDef(lv);
            case "speed" -> speed(lv);
            case "move" -> move(typeIdx, lv);
            case "resistance" -> resistance(typeIdx, lv);
            case "self_attack" -> selfAttack(typeIdx, lv);
            case "self_defense" -> selfDefense(typeIdx, lv);
            case "big_haul" -> bigHaul(lv);
            case "item" -> item(typeIdx, lv);
            default -> null;
        };
    }
}
