package maxigregrze.cobblesafari.block.incubator;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.item.PokeBallItem;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.IncubatorConfig;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.incubator.CobbreedingCompat;
import maxigregrze.cobblesafari.incubator.EggIncubatorRecipe;
import maxigregrze.cobblesafari.incubator.EggIncubatorRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IncubatorBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<IncubatorBlock> CODEC = simpleCodec(IncubatorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty STATE = IntegerProperty.create("state", 0, 6);

    public static final int STATE_EMPTY = 0;
    public static final int STATE_DONE = 6;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public IncubatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STATE, STATE_EMPTY));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IncubatorBlockEntity incubator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int currentState = state.getValue(STATE);

        if (currentState >= 1 && currentState <= 5
                && player.isCreative()
                && stack.getItem() == ModItems.EGG_CREATIVE) {
            incubator.finishInstantly();
            CobbleSafari.LOGGER.info("[Incubator] {} used creative egg to instantly finish incubation at {}",
                    player.getName().getString(), pos);
            return ItemInteractionResult.SUCCESS;
        }

        if (currentState == STATE_EMPTY && CobbreedingCompat.isCobbreedingEgg(stack)) {
            int timer = CobbreedingCompat.getTimer(stack);
            incubator.startCobbreedingIncubation(stack, timer);
            stack.shrink(1);
            level.playSound(null, pos, CobblemonSounds.FOSSIL_MACHINE_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            CobbleSafari.LOGGER.info("[Incubator] {} started Cobbreeding egg incubation at {} (timer: {} -> {})",
                    player.getName().getString(), pos, timer,
                    Math.round(timer * IncubatorConfig.getCobbreedingHatchSpeedMultiplier()));
            return ItemInteractionResult.SUCCESS;
        }

        if (currentState == STATE_EMPTY && EggIncubatorRegistry.isValidInput(stack)) {
            incubator.startIncubation(stack);
            stack.shrink(1);
            level.playSound(null, pos, CobblemonSounds.FOSSIL_MACHINE_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            CobbleSafari.LOGGER.info("[Incubator] {} started incubation with {} at {}",
                    player.getName().getString(),
                    BuiltInRegistries.ITEM.getKey(incubator.getInputItem().getItem()),
                    pos);
            return ItemInteractionResult.SUCCESS;
        }

        if (currentState == STATE_DONE && stack.getItem() instanceof PokeBallItem pokeBallItem) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return ItemInteractionResult.SUCCESS;
            }

            PokeBall ballType = pokeBallItem.getPokeBall();
            stack.shrink(1);

            Pokemon pokemon = incubator.isCobbreedingEgg() ? createPokemonFromCobbreedingEgg(incubator) : createPokemonFromIncubator(incubator);
            pokemon.setCaughtBall(ballType);

            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(serverPlayer);
            party.add(pokemon);

            level.playSound(null, pos, CobblemonSounds.FOSSIL_MACHINE_RETRIEVE_POKEMON, SoundSource.BLOCKS, 1.0F, 1.0F);
            CobbleSafari.LOGGER.info("[Incubator] {} retrieved {} from incubator at {}",
                    serverPlayer.getName().getString(),
                    pokemon.getSpecies().getName(),
                    pos);

            incubator.reset();
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private Pokemon createPokemonFromIncubator(IncubatorBlockEntity incubator) {
        EggIncubatorRecipe recipe = EggIncubatorRegistry.getRecipe(incubator.getInputItem());
        if (recipe == null || recipe.outputs().isEmpty()) {
            CobbleSafari.LOGGER.warn("[Incubator] No recipe found for {}, outputting random pokemon",
                    BuiltInRegistries.ITEM.getKey(incubator.getInputItem().getItem()));
            return PokemonProperties.Companion.parse("random").create();
        }

        List<String> outputs = recipe.outputs();
        String chosen = outputs.get(ThreadLocalRandom.current().nextInt(outputs.size()));
        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(incubator.getInputItem().getItem());

        try {
            PokemonProperties props = PokemonProperties.Companion.parse(chosen);
            Pokemon pokemon = props.create();
            if (pokemon.getSpecies().getName().equalsIgnoreCase("MissingNo")) {
                throw new IllegalStateException("Resolved to MissingNo");
            }
            applyShinyBoost(pokemon, recipe.shinyBoost());
            return pokemon;
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("[Incubator] The '{}' species from the {} data file was not found, outputting random pokemon instead",
                    chosen, inputId);
            return PokemonProperties.Companion.parse("random").create();
        }
    }

    private Pokemon createPokemonFromCobbreedingEgg(IncubatorBlockEntity incubator) {
        PokemonProperties properties = CobbreedingCompat.extractProperties(incubator.getInputItem());
        if (properties != null) {
            try {
                Pokemon pokemon = properties.create();
                if (pokemon.getSpecies().getName().equalsIgnoreCase("MissingNo")) {
                    throw new IllegalStateException("Resolved to MissingNo");
                }
                return pokemon;
            } catch (Exception e) {
                CobbleSafari.LOGGER.warn("[Incubator] Failed to create Pokemon from Cobbreeding egg data", e);
            }
        }
        String fallbackName = incubator.getStoredEggSpeciesName();
        if (fallbackName != null && !fallbackName.isEmpty()) {
            try {
                Pokemon pokemon = PokemonProperties.Companion.parse(fallbackName).create();
                if (pokemon != null && !pokemon.getSpecies().getName().equalsIgnoreCase("MissingNo")) {
                    return pokemon;
                }
            } catch (Exception e) {
                CobbleSafari.LOGGER.warn("[Incubator] Failed to create Pokemon from stored species name '{}'", fallbackName, e);
            }
        }
        CobbleSafari.LOGGER.warn("[Incubator] Cobbreeding egg has no recoverable data, outputting random");
        return PokemonProperties.Companion.parse("random").create();
    }

    private void applyShinyBoost(Pokemon pokemon, int shinyBoost) {
        if (shinyBoost <= 1) return;
        int shinyRate = getCobblemonShinyRate();
        int roll = ThreadLocalRandom.current().nextInt(1, shinyRate + 1);
        if (roll <= shinyBoost) {
            pokemon.setShiny(true);
        }
    }

    private static int getCobblemonShinyRate() {
        try {
            Object config = Cobblemon.INSTANCE.getClass().getField("config").get(Cobblemon.INSTANCE);
            Object rate = config.getClass().getField("shinyRate").get(config);
            return rate instanceof Number n ? n.intValue() : 8192;
        } catch (Exception e) {
            return 8192;
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IncubatorBlockEntity incubator) {
                if (!player.isCreative()) {
                    ItemStack drop = incubator.isEmpty()
                            ? new ItemStack(this)
                            : createIncubatorDrop(incubator);
                    Block.popResource(level, pos, drop);
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
                return state;
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private ItemStack createIncubatorDrop(IncubatorBlockEntity incubator) {
        ItemStack drop = new ItemStack(this);
        CompoundTag tag = new CompoundTag();
        if (!incubator.getInputItem().isEmpty()) {
            tag.put("InputItem", incubator.getInputItem().save(incubator.getLevel().registryAccess()));
        }
        tag.putInt("TicksRemaining", incubator.getTicksRemaining());
        tag.putInt("TotalTicks", incubator.getTotalTicks());
        tag.putBoolean("IsCobbreedingEgg", incubator.isCobbreedingEgg());
        String speciesName = incubator.getStoredEggSpeciesName();
        if (!speciesName.isEmpty()) {
            tag.putString("StoredEggSpeciesName", speciesName);
        }
        drop.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return drop;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof IncubatorBlockEntity incubator) {
                    CompoundTag tag = customData.copyTag();
                    incubator.loadAdditional(tag, level.registryAccess());
                    int ticksRem = incubator.getTicksRemaining();
                    int total = incubator.getTotalTicks();
                    int newState;
                    if (incubator.isEmpty()) {
                        newState = STATE_EMPTY;
                    } else if (ticksRem <= 0) {
                        newState = STATE_DONE;
                    } else {
                        int elapsed = total - ticksRem;
                        newState = Math.min(1 + (elapsed * 5 / total), 5);
                    }
                    level.setBlock(pos, state.setValue(STATE, newState), Block.UPDATE_ALL);
                    incubator.setChanged();
                }
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IncubatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() && type == ModBlockEntities.INCUBATOR) {
            return (lvl, pos, st, be) -> ((IncubatorBlockEntity) be).serverTick(lvl, pos, st);
        }
        return null;
    }
}
