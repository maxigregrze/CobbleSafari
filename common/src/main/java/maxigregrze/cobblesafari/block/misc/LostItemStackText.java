package maxigregrze.cobblesafari.block.misc;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The mode 2 stack of a Lost Item as editable text (action plan 193, section 8.2): its components are
 * written as the bracket part of a {@code /give} argument and read back with vanilla's own item parser.
 */
public final class LostItemStackText {

    private LostItemStackText() {
    }

    /** Components of {@code stack} as {@code [key=value,!removed]}; an empty string when it has none. */
    public static String writeComponents(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return "";
        }
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        List<String> entries = new ArrayList<>();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : stack.getComponentsPatch().entrySet()) {
            ResourceLocation key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            if (key == null) {
                continue;
            }
            if (entry.getValue().isEmpty()) {
                entries.add("!" + key);
            } else {
                // Transient components have no codec and cannot be written; they are dropped here.
                encode(entry.getKey(), entry.getValue().get(), ops).ifPresent(tag -> entries.add(key + "=" + tag));
            }
        }
        return entries.isEmpty() ? "" : "[" + String.join(",", entries) + "]";
    }

    /**
     * Rebuilds a stack from the three GUI fields with the {@code /give} parser. The brackets around
     * {@code components} are optional. Throws with the parser's own message on any syntax error.
     */
    public static ItemStack parse(String itemId, String components, int count, HolderLookup.Provider registries)
            throws CommandSyntaxException {
        String data = components.trim();
        if (!data.isEmpty() && !data.startsWith("[")) {
            data = "[" + data + "]";
        }
        ItemParser.ItemResult result = new ItemParser(registries).parse(new StringReader(itemId.trim() + data));
        return new ItemStack(result.item(), count, result.components());
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<Tag> encode(DataComponentType<T> type, Object value, DynamicOps<Tag> ops) {
        Codec<T> codec = type.codec();
        if (codec == null) {
            return Optional.empty();
        }
        return codec.encodeStart(ops, (T) value).result();
    }
}
