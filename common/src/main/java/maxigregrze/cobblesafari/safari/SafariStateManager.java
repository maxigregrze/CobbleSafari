package maxigregrze.cobblesafari.safari;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.init.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SafariStateManager {

    private static final Map<UUID, SafariPokemonState> STATES = new ConcurrentHashMap<>();
    private static final List<ScheduledTask> SCHEDULED_TASKS = Collections.synchronizedList(new ArrayList<>());
    private static long currentServerTick = 0;

    private SafariStateManager() {}

    public static void onServerTick(MinecraftServer server) {
        currentServerTick++;
        
        List<ScheduledTask> tasksToExecute = new ArrayList<>();
        synchronized (SCHEDULED_TASKS) {
            Iterator<ScheduledTask> iterator = SCHEDULED_TASKS.iterator();
            while (iterator.hasNext()) {
                ScheduledTask task = iterator.next();
                if (currentServerTick >= task.executionTick) {
                    tasksToExecute.add(task);
                    iterator.remove();
                }
            }
        }
        
        for (ScheduledTask task : tasksToExecute) {
            task.action.run();
        }
    }

    private static class ScheduledTask {
        final long executionTick;
        final Runnable action;

        ScheduledTask(long executionTick, Runnable action) {
            this.executionTick = executionTick;
            this.action = action;
        }
    }

    public static SafariPokemonState getOrCreate(UUID entityId) {
        return STATES.computeIfAbsent(entityId, SafariPokemonState::new);
    }

    public static SafariPokemonState getState(UUID entityId) {
        return STATES.get(entityId);
    }

    public static void remove(UUID entityId) {
        STATES.remove(entityId);
    }

    public static boolean hasState(UUID entityId) {
        return STATES.containsKey(entityId);
    }

    public static boolean isInSafariDimension(PokemonEntity pokemonEntity) {
        if (!(pokemonEntity.level() instanceof ServerLevel serverLevel)) return false;
        ResourceLocation safariDimension = ResourceLocation.parse(SafariTimerConfig.getSafariDimensionId());
        ResourceLocation currentDimension = serverLevel.dimension().location();
        return currentDimension.equals(safariDimension);
    }

    public static void applyMudBall(PokemonEntity pokemonEntity) {
        SafariPokemonState state = getOrCreate(pokemonEntity.getUUID());
        int applied = state.applyMudBall();
        if (applied > 0 && pokemonEntity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    pokemonEntity.getX(), pokemonEntity.getY() + pokemonEntity.getBbHeight() + 0.5,
                    pokemonEntity.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }
    }

    public static void applyBait(PokemonEntity pokemonEntity) {
        SafariPokemonState state = getOrCreate(pokemonEntity.getUUID());
        int applied = state.applyBait();
        if (applied > 0 && pokemonEntity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    pokemonEntity.getX(), pokemonEntity.getY() + pokemonEntity.getBbHeight() + 0.5,
                    pokemonEntity.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }
    }

    public static void tryFlee(PokemonEntity pokemonEntity) {
        if (!isInSafariDimension(pokemonEntity)) return;
        
        UUID entityId = pokemonEntity.getUUID();
        SafariPokemonState state = getOrCreate(entityId);
        if (state.isFleeing()) return;

        if (pokemonEntity.getPokemon().getShiny() && !SafariConfig.canShinyFlee()) return;

        int actualFleeRate = state.getActualFleeRate();
        int roll = pokemonEntity.getRandom().nextInt(255);
        
        if (roll > actualFleeRate) return;

        startFleeing(pokemonEntity, state);
    }

    public static void triggerImmediateDespawn(PokemonEntity pokemonEntity) {
        if (!(pokemonEntity.level() instanceof ServerLevel serverLevel)) return;

        SafariPokemonState state = getState(pokemonEntity.getUUID());
        int token = state != null ? state.getFleeToken() : 0;

        pokemonEntity.getPokemon().getCustomProperties().add(
                UncatchableProperty.INSTANCE.uncatchable()
        );

        Component fledMessage = Component.translatable(
                "cobblesafari.safari.pokemon_fled",
                pokemonEntity.getExposedSpecies().getTranslatedName()
        );
        sendSubtitleToNearbyPlayers(pokemonEntity, fledMessage);
        serverLevel.playSound(null, pokemonEntity.getX(), pokemonEntity.getY(), pokemonEntity.getZ(),
                ModSounds.FLEE_SOUND, SoundSource.NEUTRAL, 1.0f, 1.0f);

        startFadeAndDespawn(pokemonEntity, token);
    }

    public static void pauseFleeTimer(UUID entityId) {
        SafariPokemonState state = getState(entityId);
        if (state != null) {
            state.incrementFleeToken();
        }
    }

    private static void startFleeing(PokemonEntity pokemonEntity, SafariPokemonState state) {
        state.setFleeing(true);

        if (pokemonEntity.level() instanceof ServerLevel serverLevel) {
            state.setFleeStartTick(serverLevel.getServer().getTickCount());

            Component fleeMessage = Component.translatable(
                    "cobblesafari.safari.flee_warning",
                    pokemonEntity.getExposedSpecies().getTranslatedName()
            );
            sendSubtitleToNearbyPlayers(pokemonEntity, fleeMessage);

            int graceTicks = SafariConfig.getFleeGracePeriodTicks();
            scheduleFleeSequence(pokemonEntity, graceTicks);
        }
    }

    private static void scheduleFleeSequence(PokemonEntity pokemonEntity, int graceTicks) {
        UUID entityId = pokemonEntity.getUUID();
        SafariPokemonState state = getState(entityId);
        if (state == null) return;
        int token = state.getFleeToken();
        long executionTick = currentServerTick + graceTicks;

        SCHEDULED_TASKS.add(new ScheduledTask(executionTick, () -> {
            SafariPokemonState current = getState(entityId);
            if (current == null || !current.isFleeing() || current.getFleeToken() != token) return;
            if (!pokemonEntity.isAlive()) {
                remove(entityId);
                return;
            }

            pokemonEntity.getPokemon().getCustomProperties().add(
                    UncatchableProperty.INSTANCE.uncatchable()
            );

            Component fledMessage = Component.translatable(
                    "cobblesafari.safari.pokemon_fled",
                    pokemonEntity.getExposedSpecies().getTranslatedName()
            );
            sendSubtitleToNearbyPlayers(pokemonEntity, fledMessage);

            if (pokemonEntity.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, pokemonEntity.getX(), pokemonEntity.getY(), pokemonEntity.getZ(),
                        ModSounds.FLEE_SOUND, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }

            startFadeAndDespawn(pokemonEntity, token);
        }));
    }

    private static void startFadeAndDespawn(PokemonEntity pokemonEntity, int token) {
        UUID entityId = pokemonEntity.getUUID();

        int[][] fadePattern = {
            {3, 0},   {4, 1},
            {6, 0},   {8, 1},
            {9, 0},   {10, 1},
            {11, 0},  {12, 1},
            {13, 0},  {14, 1},
            {15, 0}
        };

        for (int[] step : fadePattern) {
            boolean invisible = (step[1] == 1);
            long executionTick = currentServerTick + step[0];
            SCHEDULED_TASKS.add(new ScheduledTask(executionTick, () -> {
                SafariPokemonState current = getState(entityId);
                if (current != null && current.getFleeToken() != token) return;
                if (pokemonEntity.isAlive()) {
                    pokemonEntity.setInvisible(invisible);
                }
            }));
        }

        long despawnTick = currentServerTick + 20;
        SCHEDULED_TASKS.add(new ScheduledTask(despawnTick, () -> {
            SafariPokemonState current = getState(entityId);
            if (current != null && current.getFleeToken() != token) return;
            if (pokemonEntity.isAlive()) {
                pokemonEntity.setInvisible(true);
                pokemonEntity.discard();
            }
            remove(entityId);
        }));
    }

    public static void scheduleTickDelay(int ticks, Runnable action) {
        if (ticks <= 0) {
            action.run();
            return;
        }
        long executionTick = currentServerTick + ticks;
        SCHEDULED_TASKS.add(new ScheduledTask(executionTick, action));
    }

    public static void sendSubtitleToPlayer(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(2, 20, 5));
        player.connection.send(new ClientboundSetSubtitleTextPacket(message));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
    }

    private static void sendSubtitleToNearbyPlayers(PokemonEntity pokemon, Component message) {
        if (!(pokemon.level() instanceof ServerLevel serverLevel)) return;
        AABB area = pokemon.getBoundingBox().inflate(48.0);
        List<ServerPlayer> players = serverLevel.getPlayers(p -> area.contains(p.getX(), p.getY(), p.getZ()));
        for (ServerPlayer player : players) {
            sendSubtitleToPlayer(player, message);
        }
    }

    public static void cleanup() {
        STATES.clear();
    }
}
