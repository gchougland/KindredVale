package com.hexvane.kindredvalemod;

import com.hexvane.kindredvalemod.command.HubBuildCommand;
import com.hexvane.kindredvalemod.config.HubProtectionConfig;
import com.hexvane.kindredvalemod.hub.HubProtectionService;
import com.hexvane.kindredvalemod.hub.systems.HubBlockBreakProtectionSystem;
import com.hexvane.kindredvalemod.hub.systems.HubBlockDamageProtectionSystem;
import com.hexvane.kindredvalemod.hub.systems.HubBlockPlaceProtectionSystem;
import com.hexvane.kindredvalemod.permissions.KindredValePermissions;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;

public class KindredValeModPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HUB_PROTECTION_CONFIG_FILE = "HubProtection.json";

    private final Config<HubProtectionConfig> hubProtectionConfig =
        withConfig("HubProtection", HubProtectionConfig.CODEC);

    private HubProtectionService hubProtectionService;

    public KindredValeModPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("KindredValeMod loaded (v%s)", this.getManifest().getVersion());
    }

    @Override
    protected void setup() {
        KindredValePermissions.register(getBasePermission());

        hubProtectionService = new HubProtectionService(hubProtectionConfig);

        final var entityStoreRegistry = getEntityStoreRegistry();
        entityStoreRegistry.registerSystem(new HubBlockBreakProtectionSystem(hubProtectionService));
        entityStoreRegistry.registerSystem(new HubBlockPlaceProtectionSystem(hubProtectionService));
        entityStoreRegistry.registerSystem(new HubBlockDamageProtectionSystem(hubProtectionService));

        getEventRegistry().registerGlobal(StartWorldEvent.class, this::onWorldStart);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        getCommandRegistry().registerCommand(new HubBuildCommand(hubProtectionService));
    }

    @Override
    protected void start() {
        ensureDefaultHubProtectionConfig();

        final var permissionsModule = PermissionsModule.get();
        if (permissionsModule != null) {
            permissionsModule.refreshVirtualGroups();
        }

        for (final var world : Universe.get().getWorlds().values()) {
            hubProtectionService.applyPvpSettings(world);
        }

        LOGGER.atInfo().log(
            "Hub protection enabled for worlds: %s",
            String.join(", ", hubProtectionConfig.get().getProtectedWorlds())
        );
    }

    private void onWorldStart(@Nonnull final StartWorldEvent event) {
        hubProtectionService.applyPvpSettings(event.getWorld());
    }

    private void onPlayerDisconnect(@Nonnull final PlayerDisconnectEvent event) {
        hubProtectionService.clearBuildMode(event.getPlayerRef().getUuid());
    }

    /**
     * {@link Config#load()} uses defaults in memory when the file is missing but does not write it.
     * Persist defaults so server ops can edit {@value #HUB_PROTECTION_CONFIG_FILE} directly.
     */
    private void ensureDefaultHubProtectionConfig() {
        final Path configPath = getDataDirectory().resolve(HUB_PROTECTION_CONFIG_FILE);
        if (Files.exists(configPath)) {
            return;
        }

        hubProtectionConfig.save().join();
        LOGGER.atInfo().log("Created default hub protection config at %s", configPath);
    }
}
