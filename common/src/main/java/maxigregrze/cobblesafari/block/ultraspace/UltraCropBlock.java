package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Ultra Beast fruit crop (action plan 192, section 12): vanilla {@link CropBlock} growth on
 * farmland, eight stages = ages 0-7.
 * <ul>
 *   <li>Stages 1-4 (ages 0-3): a young shoot - no collision, a growing selection box, gives back its
 *   one seed when broken. The seed is <b>not renewable</b>: nothing ever gives a second one.</li>
 *   <li>Stages 5-8 (ages 4-7): the grown plant's box, solid, and stinging for Pheromosa. Broken, it
 *   drops the grown plant (plus the fruit at stage 8: see the block loot table).</li>
 *   <li>Stage 8, right-click: the fruit pops out and the crop becomes its grown, decorative plant.</li>
 * </ul>
 */
public class UltraCropBlock extends CropBlock {

    /** Last young age: stages 1-4 are ages 0-3. */
    public static final int LAST_YOUNG_AGE = 3;
    private static final VoxelShape[] YOUNG_SHAPES = {
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(0, 0, 0, 16, 6, 16),
            Block.box(0, 0, 0, 16, 8, 16)
    };

    private final Supplier<? extends ItemLike> seeds;
    private final Supplier<? extends ItemLike> fruit;
    private final Supplier<? extends Block> grownPlant;
    private final VoxelShape matureShape;
    private final boolean stinging;

    public UltraCropBlock(Properties properties, Supplier<? extends ItemLike> seeds, Supplier<? extends ItemLike> fruit,
                          Supplier<? extends Block> grownPlant, VoxelShape matureShape, boolean stinging) {
        super(properties);
        this.seeds = seeds;
        this.fruit = fruit;
        this.grownPlant = grownPlant;
        this.matureShape = matureShape;
        this.stinging = stinging;
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return simpleCodec(props -> new UltraCropBlock(props, seeds, fruit, grownPlant, matureShape, stinging));
    }

    public static boolean isYoung(BlockState state) {
        return state.getValue(AGE) <= LAST_YOUNG_AGE;
    }

    /** Name shown by Jade / WTHIT: the young shoot reads as a "sprout", from stage 5 as the plant. */
    public Component displayName(BlockState state) {
        return isYoung(state) ? Component.translatable(getDescriptionId() + ".young") : getName();
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seeds.get();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(isYoung(state) ? seeds.get() : grownPlant.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isYoung(state) ? YOUNG_SHAPES[state.getValue(AGE)] : matureShape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isYoung(state) ? Shapes.empty() : matureShape;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (stinging && !isYoung(state) && entity instanceof LivingEntity) {
            entity.hurt(level.damageSources().cactus(), 1.0f);
        }
    }

    /** Stage 8: the fruit pops out and the crop becomes the decorative grown plant, sunk in its farmland. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!isMaxAge(state)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            popResource(level, pos, new ItemStack(fruit.get()));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f,
                    0.8f + level.getRandom().nextFloat() * 0.4f);
            BlockState grown = grownPlant.get().defaultBlockState()
                    .setValue(UltraGrownPlantBlock.FACE, AttachFace.FLOOR)
                    .setValue(UltraGrownPlantBlock.FACING, player.getDirection().getOpposite());
            level.setBlock(pos, UltraGrownPlantBlock.withSink(grown, level, pos), Block.UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
