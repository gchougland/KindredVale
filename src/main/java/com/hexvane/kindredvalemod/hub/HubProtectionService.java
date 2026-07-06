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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Determines whether hub protection applies and whether a player may bypass it.
 */
public final class HubProtectionService {

    @Nonnull
    private final Config<HubProtectionConfig> config;

    /**
     * Players with hub build permission who have enabled build mode for this session.
     */
    @Nonnull
    private final Set<UUID> buildModeEnabled = ConcurrentHashMap.newKeySet();

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
     * @return true if the player has permission to use hub build mode
     */
    public boolean hasHubBuildPermission(@Nonnull final UUID playerUuid) {
        final var permissions = PermissionsModule.get();
        if (permissions == null) {
            return false;
        }
        return permissions.hasPermission(playerUuid, KindredValePermissions.getHubBuildPermission());
    }

    /**
     * @return true if hub block protection should not apply to this player
     */
    public boolean shouldBypassHubProtection(@Nonnull final UUID playerUuid) {
        return hasHubBuildPermission(playerUuid) && buildModeEnabled.contains(playerUuid);
    }

    public boolean isBuildModeEnabled(@Nonnull final UUID playerUuid) {
        return buildModeEnabled.contains(playerUuid);
    }

    public void setBuildModeEnabled(@Nonnull final UUID playerUuid, final boolean enabled) {
        if (enabled) {
            buildModeEnabled.add(playerUuid);
        } else {
            buildModeEnabled.remove(playerUuid);
        }
    }

    /**
     * @return the new build mode state after toggling
     */
    public boolean toggleBuildMode(@Nonnull final UUID playerUuid) {
        if (buildModeEnabled.contains(playerUuid)) {
            buildModeEnabled.remove(playerUuid);
            return false;
        }
        buildModeEnabled.add(playerUuid);
        return true;
    }

    public void clearBuildMode(@Nonnull final UUID playerUuid) {
        buildModeEnabled.remove(playerUuid);
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
