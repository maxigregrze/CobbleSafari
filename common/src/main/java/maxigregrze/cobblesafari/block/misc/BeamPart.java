package maxigregrze.cobblesafari.block.misc;

import net.minecraft.util.StringRepresentable;

/**
 * One cell of the belltower beam's 3x3 layout, numbered as authored:
 * <pre>
 *   north
 *   1 2 3
 *   4 5 6
 *   7 8 9
 *   south
 * </pre>
 * The offsets are <em>local</em> - expressed in the structure's own frame, before the block's
 * FACING is applied. That is what makes {@code rotate} a one-liner on FACING alone: turning the
 * structure never renumbers its cells.
 */
public enum BeamPart implements StringRepresentable {
    P1("p1", -1, -1),
    P2("p2",  0, -1),
    P3("p3",  1, -1),
    P4("p4", -1,  0),
    P5("p5",  0,  0),
    P6("p6",  1,  0),
    P7("p7", -1,  1),
    P8("p8",  0,  1),
    P9("p9",  1,  1);

    private final String name;
    private final int dx;
    private final int dz;

    BeamPart(String name, int dx, int dz) {
        this.name = name;
        this.dx = dx;
        this.dz = dz;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    /** The model number this cell draws with: {@code belltower_beam_<number>}. */
    public int modelNumber() {
        return ordinal() + 1;
    }

    /** The cell at a local offset. Both components must be in {@code -1..1}. */
    public static BeamPart at(int dx, int dz) {
        for (BeamPart part : values()) {
            if (part.dx == dx && part.dz == dz) {
                return part;
            }
        }
        throw new IllegalArgumentException("offset outside the 3x3 grid: " + dx + ", " + dz);
    }
}
