package maxigregrze.cobblesafari.client.screen.warpride;

import maxigregrze.cobblesafari.item.ultraloot.ReconArmorLogic;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * A never-ticked copy of the local player, posed in elytra flight for the warp ride screen (action
 * plan 189, section 8). Same game profile, hence same player info, hence same skin and cape.
 *
 * <p>Never added to the level: nothing ticks it, nothing syncs it, and it is dropped with the
 * screen. The real {@code LocalPlayer} is left alone - its flags and equipment are owned by the
 * server, and bending them for a frame would fight the next sync.</p>
 */
public final class WarpRideRiderProxy extends RemotePlayer {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** Far above the camera: a name tag only shows within 64 blocks, and this is all it changes. */
    private static final double NAME_TAG_DODGE = 4096.0;

    /** Fully spread wings, as {@code ElytraModel} would reach them after ~30 frames of easing. */
    private static final float SPREAD_ROT_X = (float) (Math.PI / 9.0);
    private static final float SPREAD_ROT_Z = (float) (-Math.PI / 2.0);

    private static final ItemStack PLAIN_ELYTRA = new ItemStack(Items.ELYTRA);

    public WarpRideRiderProxy(ClientLevel level, LocalPlayer source) {
        super(level, source.getGameProfile());
    }

    /** Armour (only when it changed), skin layers and the frozen elytra-flight pose. */
    public void syncFrom(LocalPlayer source, Vec3 cameraPosition) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack worn = source.getItemBySlot(slot);
            if (!ItemStack.matches(getItemBySlot(slot), worn)) {
                setItemSlot(slot, worn.copy());
            }
        }
        // Protected statics are reachable from a subclass: no accessor mixin is needed.
        entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, source.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
        setSharedFlag(FLAG_FALL_FLYING, true);
        fallFlyTicks = 20;                 // > 4: HumanoidModel switches to the flight pose
        elytraRotX = SPREAD_ROT_X;
        elytraRotY = 0.0f;
        elytraRotZ = SPREAD_ROT_Z;
        // Facing north (-Z), i.e. into the screen; xRot 0 lays the body flat under the flight pitch.
        setYRot(180.0f);
        yRotO = 180.0f;
        setXRot(0.0f);
        xRotO = 0.0f;
        yBodyRot = 180.0f;
        yBodyRotO = 180.0f;
        yHeadRot = 180.0f;
        yHeadRotO = 180.0f;
        setPos(cameraPosition.x, cameraPosition.y + NAME_TAG_DODGE, cameraPosition.z);
        xo = getX();
        yo = getY();
        zo = getZ();
    }

    /**
     * What the elytra layer draws: nothing in the full Ultra Recon set, whose own wings come from
     * {@code ReconElytraLayer} (action plan 200, section 4.2); else the worn elytra (keeping its
     * glint), else a plain one.
     */
    public ItemStack elytraStack() {
        if (ReconArmorLogic.isFullSet(this)) {
            return ItemStack.EMPTY;
        }
        ItemStack chest = getItemBySlot(EquipmentSlot.CHEST);
        return chest.is(Items.ELYTRA) ? chest : PLAIN_ELYTRA;
    }
}
