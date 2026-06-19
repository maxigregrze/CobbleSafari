package maxigregrze.cobblesafari.csmusic;

import net.minecraft.core.BlockPos;

public record CsMusicBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public static CsMusicBox of(BlockPos a, BlockPos b) {
        return new CsMusicBox(
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
