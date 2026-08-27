package com.hexvane.kindredvalemod.command;

import com.hexvane.kindredvalemod.teleport.WorldTeleportService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Teleports the player to the Kindred Vale hub world.
 */
public final class HubCommand extends AbstractPlayerCommand {

    @Nonnull
    private final WorldTeleportService teleportService;

    public HubCommand(@Nonnull final WorldTeleportService teleportService) {
        super("hub", "Teleport to the Kindred Vale hub");
        requireNoPermission();
        this.teleportService = teleportService;
    }

    @Override
    protected void execute(
        @Nonnull final CommandContext context,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final Ref<EntityStore> ref,
        @Nonnull final PlayerRef playerRef,
        @Nonnull final World world
    ) {
        String hubId = teleportService.getHubDestinationId();
        if (!teleportService.isDestinationAvailable(hubId, store)) {
            playerRef.sendMessage(Message.raw("You are already at the hub, or the hub world is unavailable."));
            return;
        }
        if (teleportService.teleportToDestination(ref, store, hubId)) {
            playerRef.sendMessage(Message.raw("Returning to Kindred Vale..."));
        }
    }
}
