package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3f;

/**
 * Sludge/mud variant for {@link AttackPileProjectileEntity}:
 * display cube, ephemeral block placed on impact, particle trail.
 */
public enum PileKind {
    SLUDGE(
            ModBlocks.ATTACK_SLUDGE_CUBE_DISPLAY,
            ModBlocks.EPHEMERAL_SLUDGE_PILE,
            new DustParticleOptions(new Vector3f(0.45f, 0.15f, 0.55f), 1.2f),
            true,
            false
    ),
    MUD(
            ModBlocks.ATTACK_MUD_CUBE_DISPLAY,
            ModBlocks.EPHEMERAL_MUD_PILE,
            new DustParticleOptions(new Vector3f(0.545f, 0.416f, 0.310f), 1.2f),
            false,
            true
    );

    private final Block displayBlock;
    private final Block ephemeralBlock;
    private final DustParticleOptions trailDust;
    private final boolean appliesPoison;
    private final boolean appliesDamage;

    PileKind(Block displayBlock, Block ephemeralBlock, DustParticleOptions trailDust,
             boolean appliesPoison, boolean appliesDamage) {
        this.displayBlock = displayBlock;
        this.ephemeralBlock = ephemeralBlock;
        this.trailDust = trailDust;
        this.appliesPoison = appliesPoison;
        this.appliesDamage = appliesDamage;
    }

    public Block displayBlock() {
        return displayBlock;
    }

    public Block ephemeralBlock() {
        return ephemeralBlock;
    }

    public DustParticleOptions trailDust() {
        return trailDust;
    }

    public boolean appliesPoison() {
        return appliesPoison;
    }

    public boolean appliesDamage() {
        return appliesDamage;
    }

    public static PileKind fromOrdinal(int ordinal) {
        PileKind[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SLUDGE;
    }
}
