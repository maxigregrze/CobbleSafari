package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {

    public static final SoundEvent FLEE_SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "safari.flee"));

    public static final SoundEvent SPRAY_SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "safari.spray"));

    public static final SoundEvent GIRATINA_TRADE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "giratina.trade"));

    public static final SoundEvent MUSIC_DISTORTION_INTRO = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "music.distortion.intro"));

    public static final SoundEvent MUSIC_DISTORTION_LOOP = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "music.distortion.loop"));

    public static final SoundEvent MUSIC_UNDERGROUND_INTRO = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "music.underground.intro"));

    public static final SoundEvent MUSIC_UNDERGROUND_LOOP = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "music.underground.loop"));

    private ModSounds() {}
}
