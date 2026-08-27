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

    public static final SoundEvent MACHINE_UNION = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "block.machine_union"));

    public static final SoundEvent MACHINE_GTS = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "block.machine_gts"));

    public static final SoundEvent MACHINE_WONDER = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "block.machine_wonder"));

    /** Belltower bell, rung by right- or left-clicking one of its sides (plan 170). */
    public static final SoundEvent BELL_CHIME = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "block.bell_chime"));

    public static final SoundEvent FART = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "entity.generic.fart"));

    public static final SoundEvent FART_REVERB = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "entity.generic.fart_reverb"));

    /** Played on every client that can see a Rainbow Feather save (plan 178, § 3.5). */
    public static final SoundEvent RAINBOW_FEATHER_REVIVE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "entity.generic.rainbow_feather_revive"));

    /** Intentionally silent sound (empty sounds.json entry) - used to mute the fart trap's explosion boom. */
    public static final SoundEvent SILENT = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "entity.generic.silent"));

    // --- Ultra Warp Ride. Call sites are final; the .ogg files are placeholders. ---

    public static final SoundEvent WARPRIDE_POSITIVE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "warpride.positive"));

    public static final SoundEvent WARPRIDE_NEGATIVE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "warpride.negative"));

    public static final SoundEvent WARPRIDE_JUMP = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "warpride.jump"));

    public static final SoundEvent WARPRIDE_PORTAL = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "warpride.portal"));

    public static final SoundEvent WARPRIDE_WARNING = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "warpride.warning"));

    public static final SoundEvent WARPRIDE_COUNTDOWN = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "warpride.countdown"));

    private ModSounds() {}
}
