package maxigregrze.cobblesafari.block.misc;

import net.minecraft.world.level.block.state.properties.EnumProperty;

public interface UnionRoomColoredBlock {

    EnumProperty<UnionRoomColor> COLOR = EnumProperty.create("color", UnionRoomColor.class);
}
