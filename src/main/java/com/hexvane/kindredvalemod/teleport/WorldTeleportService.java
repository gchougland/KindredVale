package com.hexvane.kindredvalemod.teleport;

import com.hexvane.kindredvalemod.config.WorldDestinationsConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Shared same-instance world teleport used by dialogue actions and {@code /hub}.
 */
public final class WorldTeleportService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Config<WorldDestinationsConfig> destinationsConfig;

    public WorldTeleportService(@Nonnull Config<WorldDestinationsConfig> destinationsConfig) {
        this.destinationsConfig = destinationsConfig;
    }

    /**
     * @return true if the teleport was queued successfully
     */
    public boolean teleportToDestination(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull String destinationId
    ) {
        WorldDestinationsConfig.Destination destination =
            destinationsConfig.get().findById(destinationId).orElse(null);
        if (destination == null) {
            notifyPlayer(store, playerRef, "Unknown travel destination.");
            LOGGER.atWarning().log("Unknown destinationId %s", destinationId);
            return false;
        }

        World targetWorld = Universe.get().getWorld(destination.getWorldName());
        if (targetWorld == null) {
            notifyPlayer(store, playerRef, "That world is not available right now.");
            LOGGER.atWarning().log("World %s not loaded (destination %s)", destination.getWorldName(), destinationId);
            return false;
        }

        World current = store.getExternalData().getWorld();
        if (current != null && current.getName().equalsIgnoreCase(destination.getWorldName())) {
            notifyPlayer(store, playerRef, "You are already there.");
            return false;
        }

        PlayerRef player = resolvePlayerRef(store, playerRef);
        if (player == null || player.getReference() == null || !player.getReference().isValid()) {
            return false;
        }

        Ref<EntityStore> liveRef = player.getReference();
        final String worldName = destination.getWorldName();
        final String destId = destination.getId();

        // A world spawn may have to load its chunk first, so the teleport is applied once the spawn point is known,
        // on the thread of the world the player is in.
        Executor playerWorld = current != null ? current : Runnable::run;
        buildTeleport(
            player.getUuid(),
            targetWorld,
            destination.isUseWorldSpawn(),
            destination.getX(),
            destination.getY(),
            destination.getZ(),
            destination.getYawDegrees()
        )
            .exceptionally(error -> {
                LOGGER.atWarning().withCause(error).log("Could not resolve the spawn of world %s (%s)", worldName, destId);
                return null;
            })
            .thenAcceptAsync(teleport -> {
                if (!liveRef.isValid()) {
                    return;
                }
                Store<EntityStore> liveStore = liveRef.getStore();
                if (teleport == null) {
                    notifyPlayer(liveStore, liveRef, "Could not find a spawn point for that world.");
                    return;
                }
                liveStore.addComponent(liveRef, Teleport.getComponentType(), teleport);
                LOGGER.atInfo().log("Teleported player to %s (%s)", worldName, destId);
            }, playerWorld)
            .whenComplete((ignored, error) -> {
                if (error != null) {
                    LOGGER.atSevere().withCause(error).log("Failed to teleport player to %s (%s)", worldName, destId);
                }
            });
        return true;
    }

    public boolean isDestinationAvailable(@Nullable String destinationId, @Nonnull Store<EntityStore> store) {
        WorldDestinationsConfig.Destination destination =
            destinationsConfig.get().findById(destinationId).orElse(null);
        if (destination == null) {
            return false;
        }
        World targetWorld = Universe.get().getWorld(destination.getWorldName());
        if (targetWorld == null) {
            return false;
        }
        World current = store.getExternalData().getWorld();
        if (current == null) {
            return true;
        }
        return !current.getName().equalsIgnoreCase(destination.getWorldName());
    }

    @Nonnull
    public String getHubDestinationId() {
        return destinationsConfig.get().getHubDestinationId();
    }

    /**
     * @return the teleport into {@code targetWorld}, or a future holding null when that world has no spawn point
     */
    @Nonnull
    private static CompletableFuture<Teleport> buildTeleport(
        @Nonnull UUID playerUuid,
        @Nonnull World targetWorld,
        boolean useWorldSpawn,
        double x,
        double y,
        double z,
        float yawDegrees
    ) {
        if (useWorldSpawn) {
            var spawnProvider = targetWorld.getWorldConfig().getSpawnProvider();
            if (spawnProvider == null) {
                return CompletableFuture.completedFuture(null);
            }
            return spawnProvider.getSpawnPointAsync(targetWorld, playerUuid)
                .thenApply(spawnPoint -> spawnPoint != null ? Teleport.createForPlayer(targetWorld, spawnPoint) : null);
        }
        Vector3d pos = new Vector3d(x, y, z);
        Rotation3f facing = new Rotation3f(0f, (float) Math.toRadians(yawDegrees), 0f);
        return CompletableFuture.completedFuture(Teleport.createForPlayer(targetWorld, pos, facing));
    }

    @Nullable
    private static PlayerRef resolvePlayerRef(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef) {
        return store.getComponent(playerRef, PlayerRef.getComponentType());
    }

    private static void notifyPlayer(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull String text
    ) {
        PlayerRef player = resolvePlayerRef(store, playerRef);
        if (player != null) {
            player.sendMessage(Message.raw(text));
        }
    }
}
