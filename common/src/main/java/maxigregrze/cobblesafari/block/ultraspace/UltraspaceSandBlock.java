package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HoeConversion;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Strange Sand: vanilla sand behaviour, gravity included, plus the first step of the hoe cascade
 * (sand to path to patterned sand). The three texture variants are a blockstate model array, not a
 * block state property.
 */
public class UltraspaceSandBlock extends FallingBlock {

    /**
     * Held here rather than inherited from {@code ColoredFallingBlock}: that class declares an
     * invariant {@code MapCodec<ColoredFallingBlock>} which a subclass cannot narrow, so the
     * family extends {@link FallingBlock} and carries its own dust colour.
     */
    private final ColorRGBA dustColor;

    public UltraspaceSandBlock(ColorRGBA dustColor, Properties properties) {
        super(properties);
        this.dustColor = dustColor;
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return simpleCodec(props -> new UltraspaceSandBlock(this.dustColor, props));
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return dustColor.rgba();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return HoeConversion.tryConvert(stack, level, pos, player, hand, ModBlocks.ULTRASPACE_SAND_PATH);
    }
}
