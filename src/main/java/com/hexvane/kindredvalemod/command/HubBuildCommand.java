package com.hexvane.kindredvalemod.command;

import com.hexvane.kindredvalemod.hub.HubProtectionService;
import com.hexvane.kindredvalemod.permissions.KindredValePermissions;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Toggles per-session hub build mode for players with {@code hub.build} permission.
 */
public final class HubBuildCommand extends AbstractPlayerCommand {

    @Nonnull
    private final HubProtectionService hubProtection;

    @Nonnull
    private final OptionalArg<String> stateArg = withOptionalArg(
        "state",
        "on, off, or omit to toggle",
        ArgTypes.STRING
    );

    public HubBuildCommand(@Nonnull final HubProtectionService hubProtection) {
        super("hubbuild", "Toggle hub build mode for protected worlds");
        requirePermission(KindredValePermissions.getHubBuildPermission());
        this.hubProtection = hubProtection;
    }

    @Override
    protected void execute(
        @Nonnull final CommandContext context,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final Ref<EntityStore> ref,
        @Nonnull final PlayerRef playerRef,
        @Nonnull final World world
    ) {
        if (!hubProtection.isProtectedWorld(world)) {
            playerRef.sendMessage(Message.raw("Hub build mode only applies in protected hub worlds."));
            return;
        }

        final boolean enabled;
        if (context.provided(stateArg)) {
            final Boolean parsed = parseState(context.get(stateArg));
            if (parsed == null) {
                playerRef.sendMessage(Message.raw("Use on or off, or run /hubbuild with no arguments to toggle."));
                return;
            }
            enabled = parsed;
            hubProtection.setBuildModeEnabled(playerRef.getUuid(), enabled);
        } else {
            enabled = hubProtection.toggleBuildMode(playerRef.getUuid());
        }

        if (enabled) {
            playerRef.sendMessage(Message.raw("Hub build mode enabled. You can edit blocks in this world."));
        } else {
            playerRef.sendMessage(Message.raw("Hub build mode disabled. Block protection is active."));
        }
    }

    @Nullable
    private static Boolean parseState(@Nonnull final String state) {
        return switch (state.toLowerCase(Locale.ROOT)) {
            case "on", "true", "enable", "enabled" -> true;
            case "off", "false", "disable", "disabled" -> false;
            default -> null;
        };
    }
}
