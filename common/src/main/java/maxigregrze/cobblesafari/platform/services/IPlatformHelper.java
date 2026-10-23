package maxigregrze.cobblesafari.platform.services;

import maxigregrze.cobblesafari.item.ultraloot.ReconArmorItem;
import maxigregrze.cobblesafari.item.ultraloot.StakatakaShieldItem;
import maxigregrze.cobblesafari.underground.MiningSession;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

import java.nio.file.Path;

public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    Path getConfigDir();

    /**
     * Resolves a directory inside the mod's own jar/resources (e.g. {@code "data/cobblesafari/..."})
     * to a {@link Path} that can be walked at mod-init time, or {@code null} if it does not exist.
     * Used to enumerate bundled data-driven content before the registries freeze (external datapacks
     * load too late for that).
     */
    Path getBundledResourceDir(String path);

    void sendPayloadToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /**
     * Sends the payload to every player tracking the entity. A player never tracks itself, so a
     * {@link ServerPlayer} entity does not receive its own payload.
     */
    void sendPayloadToTrackingPlayers(Entity entity, CustomPacketPayload payload);

    void sendPayloadToServer(CustomPacketPayload payload);

    void openUndergroundMenu(ServerPlayer player, MiningSession session);

    /**
     * Stacked Shield item. NeoForge only counts an item as a shield when it declares the
     * {@code shield_block} item ability, so it substitutes a subclass (action plan 192, section 8.1).
     */
    default Item createStakatakaShield(Item.Properties properties) {
        return new StakatakaShieldItem(properties);
    }

    /**
     * Ultra Recon chestplate. NeoForge drives elytra flight through item methods, so it substitutes
     * a subclass; Fabric uses its elytra event instead (action plan 192, section 2.1).
     */
    default Item createReconChestplate(Holder<ArmorMaterial> material, Item.Properties properties) {
        return new ReconArmorItem(material, ArmorItem.Type.CHESTPLATE, properties);
    }

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
