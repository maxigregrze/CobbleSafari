package maxigregrze.cobblesafari.compat.wthit;

import maxigregrze.cobblesafari.block.basepc.BasePCBlockEntity;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlockEntity;
import maxigregrze.cobblesafari.block.pokestatue.AbstractPokeStatueBlockEntity;
import maxigregrze.cobblesafari.block.ultraspace.UltraCropBlock;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IWailaClientPlugin;
import mcp.mobius.waila.api.WailaConstants;

@SuppressWarnings("unused")
public class CobbleSafariWTHITClientPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(new DungeonPortalWTHITComponent(), DungeonPortalBlockEntity.class);
        registrar.body(new IncubatorWTHITComponent(), IncubatorBlockEntity.class);
        registrar.body(new BasePCWTHITComponent(), BasePCBlockEntity.class);
        PokeStatueWTHITComponent statue = new PokeStatueWTHITComponent();
        registrar.icon(statue, AbstractPokeStatueBlockEntity.class);
        registrar.body(statue, AbstractPokeStatueBlockEntity.class);
        // After the built-in name (default priority): replaces it.
        registrar.head(new UltraCropWTHITComponent(), UltraCropBlock.class, WailaConstants.DEFAULT_PRIORITY + 100);
    }
}
