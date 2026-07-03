package com.hexvane.kindredvalemod.permissions;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import javax.annotation.Nonnull;

/**
 * Permission nodes and virtual groups for Kindred Vale server infrastructure.
 */
public final class KindredValePermissions {

    /**
     * Virtual group for staff who may build in protected hub worlds.
     * Assign players via {@code permissions.json} or {@code /setgroup kindredvale:HubBuilder}.
     */
    @Nonnull
    public static final String GROUP_HUB_BUILDER = "kindredvale:HubBuilder";

    @Nonnull
    private static String hubBuildPermission = "hexvane.kindredvalemod.hub.build";

    private KindredValePermissions() {
    }

    /**
     * Registers permission nodes with {@link PermissionsModule}.
     * Call once from plugin {@code setup()}.
     *
     * @param basePermission the plugin base permission (from {@code getBasePermission()})
     */
    public static void register(@Nonnull final String basePermission) {
        hubBuildPermission = basePermission + ".hub.build";
        PermissionsModule.registerPermission(hubBuildPermission, GROUP_HUB_BUILDER);
    }

    /**
     * @return permission node allowing block changes in protected hub worlds
     */
    @Nonnull
    public static String getHubBuildPermission() {
        return hubBuildPermission;
    }
}
