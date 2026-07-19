package com.hexvane.kindredvalemod.teleport;

import com.hexvane.kindredvalemod.config.WorldDestinationsConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
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
        final boolean useSpawn = destination.isUseWorldSpawn();
        final double x = destination.getX();
        final double y = destination.getY();
        final double z = destination.getZ();
        final float yawDegrees = destination.getYawDegrees();

        Runnable apply = () -> {
            Store<EntityStore> liveStore = liveRef.getStore();
            if (liveStore == null) {
                return;
            }
            World world = Universe.get().getWorld(worldName);
            if (world == null) {
                notifyPlayer(liveStore, liveRef, "That world is not available right now.");
                return;
            }
            Teleport teleport = buildTeleport(liveRef, liveStore, world, useSpawn, x, y, z, yawDegrees);
            if (teleport == null) {
                notifyPlayer(liveStore, liveRef, "Could not find a spawn point for that world.");
                return;
            }
            liveStore.addComponent(liveRef, Teleport.getComponentType(), teleport);
            LOGGER.atInfo().log("Teleported player to %s (%s)", worldName, destId);
        };

        if (current != null) {
            current.execute(apply);
        } else {
            apply.run();
        }
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

    @Nullable
    private static Teleport buildTeleport(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
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
                return null;
            }
            Transform spawnPoint = spawnProvider.getSpawnPoint(playerRef, store);
            if (spawnPoint == null) {
                return null;
            }
            return Teleport.createForPlayer(targetWorld, spawnPoint);
        }
        Vector3d pos = new Vector3d(x, y, z);
        Rotation3f facing = new Rotation3f(0f, (float) Math.toRadians(yawDegrees), 0f);
        return Teleport.createForPlayer(targetWorld, pos, facing);
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
