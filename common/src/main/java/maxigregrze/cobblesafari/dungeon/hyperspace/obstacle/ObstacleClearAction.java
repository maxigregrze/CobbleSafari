package maxigregrze.cobblesafari.dungeon.hyperspace.obstacle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.scheduling.SchedulingFunctionsKt;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket;
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormEntityParticlePacket;
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import maxigregrze.cobblesafari.block.hyperspace.ObstacleBlock;
import maxigregrze.cobblesafari.advancement.ModCriteria;
import maxigregrze.cobblesafari.init.ModStats;
import maxigregrze.cobblesafari.objectives.ObjectivesManager;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A sent-out Pokémon clears an obstacle with a move (action plan 196, § 8.3 - 8.6): pick the
 * Pokémon and the move, replay the move's pose, particles and sounds, then clear every obstacle
 * of the same kind in a radius-2 diamond. Cleared obstacles come back on their own (§ 8.7).
 */
public final class ObstacleClearAction {

    private static final int COOLDOWN_TICKS = 20;
    private static final double POKEMON_RANGE = 8;
    /** Special moves hit from afar: a Pokémon this far away can still clear with one. */
    private static final double SPECIAL_POKEMON_RANGE = POKEMON_RANGE * 2;
    private static final double MESSAGE_RANGE = 16;
    private static final double PACKET_RANGE = 64;
    private static final int RADIUS = 2;
    private static final int CLEAR_AFTER_TARGET_TICKS = 5;

    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    /** Obstacles inside a running sequence, keyed by dimension and position. */
    private static final Set<String> LOCKED = ConcurrentHashMap.newKeySet();

    private ObstacleClearAction() {}

    public static void tryClear(ServerLevel level, BlockPos pos, ObstacleKind kind, ServerPlayer player) {
        long now = level.getGameTime();
        Long last = COOLDOWNS.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS && now >= last) {
            return;
        }
        COOLDOWNS.put(player.getUUID(), now);
        if (LOCKED.contains(lockKey(level, pos))) {
            return;
        }
        ObstacleRule rule = ObstacleRuleDataLoader.get(kind);
        if (rule == null) {
            return;
        }
        List<PokemonEntity> candidates = sentOutByDistance(level, pos, player);
        if (candidates.isEmpty()) {
            player.displayClientMessage(Component.translatable("cobblesafari.obstacle.no_pokemon"), true);
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        boolean physicalTooFar = false;
        for (PokemonEntity entity : candidates) {
            boolean close = entity.distanceToSqr(center) <= POKEMON_RANGE * POKEMON_RANGE;
            Move move = bestMove(entity.getPokemon(), rule, close);
            if (move != null) {
                play(level, pos, kind, rule, player, entity, move);
                return;
            }
            physicalTooFar |= !close && bestMove(entity.getPokemon(), rule, true) != null;
        }
        player.displayClientMessage(Component.translatable(
                physicalTooFar ? "cobblesafari.obstacle.too_far" : rule.message()), true);
    }

    /** Sent-out party members within special-move range, nearest first. */
    private static List<PokemonEntity> sentOutByDistance(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Vec3 center = Vec3.atCenterOf(pos);
        List<PokemonEntity> out = new ArrayList<>();
        for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
            PokemonEntity entity = pokemon.getEntity();
            if (entity != null && entity.isAlive() && entity.level() == level
                    && entity.distanceToSqr(center) <= SPECIAL_POKEMON_RANGE * SPECIAL_POKEMON_RANGE) {
                out.add(entity);
            }
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return out;
    }

    /**
     * The most powerful compatible non-status move, first in moveset order on a tie (decision § 8.3).
     * Physical moves only count when the Pokémon is within {@link #POKEMON_RANGE}.
     */
    private static Move bestMove(Pokemon pokemon, ObstacleRule rule, boolean allowPhysical) {
        Move best = null;
        for (Move move : pokemon.getMoveSet().getMoves()) {
            MoveTemplate template = move.getTemplate();
            if (template.getDamageCategory() == DamageCategories.INSTANCE.getSTATUS()
                    || (!allowPhysical && template.getDamageCategory() == DamageCategories.INSTANCE.getPHYSICAL())) {
                continue;
            }
            boolean compatible = rule.accepts(template.getEffectiveElementalType(pokemon).getShowdownId(),
                    template.getName());
            if (compatible && (best == null || template.getPower() > best.getTemplate().getPower())) {
                best = move;
            }
        }
        return best;
    }

    private static void play(ServerLevel level, BlockPos origin, ObstacleKind kind, ObstacleRule rule,
                             ServerPlayer player, PokemonEntity entity, Move move) {
        List<BlockPos> targets = diamond(level, origin, kind);
        targets.forEach(p -> LOCKED.add(lockKey(level, p)));
        BlockState originState = level.getBlockState(origin);
        Vec3 center = Vec3.atCenterOf(origin);

        entity.getNavigation().stop();
        entity.lookAt(EntityAnchorArgument.Anchor.EYES, center);
        entity.setYBodyRot(entity.getYRot());
        entity.setYHeadRot(entity.getYRot());

        Component used = Component.translatable("cobblemon.battle.used_move",
                entity.getPokemon().getDisplayName(false), move.getDisplayName());
        for (ServerPlayer nearby : level.players()) {
            if (nearby.distanceToSqr(center) <= MESSAGE_RANGE * MESSAGE_RANGE) {
                nearby.displayClientMessage(used, true);
            }
        }

        MoveFieldEffect effect = MoveFieldEffect.get(level.getServer(), move.getTemplate().getName()).orElse(null);
        int delay;
        if (effect != null && !effect.animations().isEmpty()) {
            send(new PlayPosableAnimationPacket(entity.getId(), effect.animations(), List.of()), level, center);
        } else {
            boolean physical = move.getTemplate().getDamageCategory() == DamageCategories.INSTANCE.getPHYSICAL();
            send(new PlayPosableAnimationPacket(entity.getId(), Set.of(physical ? "physical" : "special"), List.of()),
                    level, center);
        }
        if (effect != null) {
            for (MoveFieldEffect.ActorParticle particle : effect.actorParticles()) {
                send(new SpawnSnowstormEntityParticlePacket(particle.effect(), entity.getId(), particle.locators(),
                        null, List.of()), level, center);
            }
            effect.actorSounds().forEach(s -> playSound(level, entity.position(), s));
            delay = effect.targetDelayTicks();
        } else {
            delay = 10;
        }

        String typeId = move.getTemplate().getEffectiveElementalType(entity.getPokemon()).getShowdownId();
        SchedulingFunctionsKt.afterOnServer(delay, level, () -> {
            for (BlockPos p : targets) {
                impact(level, p, effect, originState, typeId);
            }
            SchedulingFunctionsKt.afterOnServer(CLEAR_AFTER_TARGET_TICKS, level, () -> {
                clear(level, kind, rule, player, targets);
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
    }

    /** The clicked obstacle and every same-kind obstacle with |dx| + |dz| <= 2 on its layer (decision § 8.5). */
    private static List<BlockPos> diamond(ServerLevel level, BlockPos origin, ObstacleKind kind) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos p = origin.offset(dx, 0, dz);
                if (Math.abs(dx) + Math.abs(dz) <= RADIUS && isObstacle(level.getBlockState(p), kind)) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    private static void impact(ServerLevel level, BlockPos pos, MoveFieldEffect effect, BlockState state, String typeId) {
        Vec3 c = Vec3.atCenterOf(pos);
        if (effect != null && !effect.targetParticles().isEmpty()) {
            for (ResourceLocation particle : effect.targetParticles()) {
                send(new SpawnSnowstormParticlePacket(particle, c), level, c);
            }
            effect.targetSounds().forEach(s -> playSound(level, c, s));
            return;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), c.x, c.y, c.z, 24, 0.3, 0.3, 0.3, 0.1);
        ParticleOptions typed = switch (typeId) {
            case "fire" -> ParticleTypes.FLAME;
            case "water" -> ParticleTypes.SPLASH;
            case "flying" -> ParticleTypes.CLOUD;
            default -> null;
        };
        if (typed != null) {
            level.sendParticles(typed, c.x, c.y, c.z, 16, 0.4, 0.4, 0.4, 0.05);
        }
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void clear(ServerLevel level, ObstacleKind kind, ObstacleRule rule,
                              ServerPlayer player, List<BlockPos> targets) {
        try {
            // The clear lands after the move's animation; the onboarding may have switched clusters
            // off in between (action plan 198, F4).
            if (kind == ObstacleKind.SHARDCLUSTER && !ShardClusterSettings.enabled(level)) {
                return;
            }
            int cleared = 0;
            for (BlockPos p : targets) {
                BlockState state = level.getBlockState(p);
                if (isObstacle(state, kind)) {
                    ((ObstacleBlock) state.getBlock()).clear(level, p, state);
                    dropLoot(level, p, rule, player);
                    cleared++;
                }
            }
            if (cleared > 0 && kind.task() != null) {
                ObjectivesManager.onObstacleCleared(player, kind.task(), cleared);
                // Boulders, brambles and sludge only: shard clusters are resource nodes, not obstacles.
                int total = ModStats.addAndGet(player, ModStats.OBSTACLES_CLEARED, cleared);
                ModCriteria.OBSTACLE_CLEARED.trigger(player, total);
            }
        } finally {
            targets.forEach(p -> LOCKED.remove(lockKey(level, p)));
        }
    }

    /** One roll per obstacle cleared, dropped where it stood (decision § 8.6). */
    private static void dropLoot(ServerLevel level, BlockPos pos, ObstacleRule rule, ServerPlayer player) {
        if (rule.lootTable() == null) {
            return;
        }
        LootTable table = level.getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, rule.lootTable()));
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);
        for (ItemStack stack : table.getRandomItems(params)) {
            Block.popResource(level, pos, stack);
        }
    }

    /** A standing (not cleared) obstacle of {@code kind}. */
    private static boolean isObstacle(BlockState state, ObstacleKind kind) {
        return state.getBlock() instanceof ObstacleBlock block && block.kind() == kind
                && !state.getValue(ObstacleBlock.CLEARED);
    }

    private static void playSound(ServerLevel level, Vec3 at, ResourceLocation sound) {
        level.playSound(null, at.x, at.y, at.z, SoundEvent.createVariableRangeEvent(sound),
                SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private static void send(PlayPosableAnimationPacket packet, ServerLevel level, Vec3 at) {
        packet.sendToPlayersAround(at.x, at.y, at.z, PACKET_RANGE, level.dimension(), p -> false);
    }

    private static void send(SpawnSnowstormEntityParticlePacket packet, ServerLevel level, Vec3 at) {
        packet.sendToPlayersAround(at.x, at.y, at.z, PACKET_RANGE, level.dimension(), p -> false);
    }

    private static void send(SpawnSnowstormParticlePacket packet, ServerLevel level, Vec3 at) {
        packet.sendToPlayersAround(at.x, at.y, at.z, PACKET_RANGE, level.dimension(), p -> false);
    }

    private static String lockKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.asLong();
    }
}
