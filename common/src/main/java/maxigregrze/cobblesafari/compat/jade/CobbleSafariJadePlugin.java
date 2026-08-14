package maxigregrze.cobblesafari.compat.jade;

import maxigregrze.cobblesafari.block.basepc.BasePCBlock;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlock;
import maxigregrze.cobblesafari.block.pokestatue.CreativeStatueBlock;
import maxigregrze.cobblesafari.block.pokestatue.PokeStatueBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class CobbleSafariJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DungeonPortalJadeComponent.INSTANCE, DungeonPortalBlock.class);
        registration.registerBlockDataProvider(IncubatorJadeComponent.INSTANCE, IncubatorBlock.class);
        registration.registerBlockDataProvider(BasePCJadeComponent.INSTANCE, BasePCBlock.class);
        registration.registerBlockDataProvider(PokeStatueJadeComponent.INSTANCE, PokeStatueBlock.class);
        registration.registerBlockDataProvider(PokeStatueJadeComponent.INSTANCE, CreativeStatueBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DungeonPortalJadeComponent.INSTANCE, DungeonPortalBlock.class);
        registration.registerBlockComponent(IncubatorJadeComponent.INSTANCE, IncubatorBlock.class);
        registration.registerBlockComponent(BasePCJadeComponent.INSTANCE, BasePCBlock.class);
        registration.registerBlockComponent(PokeStatueJadeComponent.INSTANCE, PokeStatueBlock.class);
        registration.registerBlockComponent(PokeStatueJadeComponent.INSTANCE, CreativeStatueBlock.class);
    }
}
