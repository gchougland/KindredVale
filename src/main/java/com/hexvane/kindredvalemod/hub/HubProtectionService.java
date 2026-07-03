package com.hexvane.kindredvalemod.hub;

import com.hexvane.kindredvalemod.config.HubProtectionConfig;
import com.hexvane.kindredvalemod.permissions.KindredValePermissions;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Determines whether hub protection applies and whether a player may bypass it.
 */
public final class HubProtectionService {

    @Nonnull
    private final Config<HubProtectionConfig> config;

    public HubProtectionService(@Nonnull final Config<HubProtectionConfig> config) {
        this.config = config;
    }

    public boolean isProtectedWorld(@Nonnull final World world) {
        final var worldName = world.getName().toLowerCase(Locale.ROOT);
        return getProtectedWorldNames().contains(worldName);
    }

    public boolean isDisablePvpInProtectedWorlds() {
        return config.get().isDisablePvpInProtectedWorlds();
    }

    /**
     * @return true if the player may break or place blocks in a protected hub world
     */
    public boolean canBuildInHub(@Nonnull final UUID playerUuid) {
        final var permissions = PermissionsModule.get();
        if (permissions == null) {
            return false;
        }
        return permissions.hasPermission(playerUuid, KindredValePermissions.getHubBuildPermission());
    }

    public void applyPvpSettings(@Nonnull final World world) {
        if (!isProtectedWorld(world) || !isDisablePvpInProtectedWorlds()) {
            return;
        }
        world.getWorldConfig().setPvpEnabled(false);
    }

    @Nonnull
    private Set<String> getProtectedWorldNames() {
        return Arrays.stream(config.get().getProtectedWorlds())
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }
}
