package com.hexvane.kindredvalemod;

import com.hexvane.kindredvalemod.aetherhaven.AetherhavenIntegration;
import com.hexvane.kindredvalemod.command.HubBuildCommand;
import com.hexvane.kindredvalemod.command.HubCommand;
import com.hexvane.kindredvalemod.config.HexvaneNpcConfig;
import com.hexvane.kindredvalemod.config.HubProtectionConfig;
import com.hexvane.kindredvalemod.config.WorldDestinationsConfig;
import com.hexvane.kindredvalemod.hub.HubProtectionService;
import com.hexvane.kindredvalemod.hub.systems.HubBlockBreakProtectionSystem;
import com.hexvane.kindredvalemod.hub.systems.HubBlockDamageProtectionSystem;
import com.hexvane.kindredvalemod.hub.systems.HubBlockPlaceProtectionSystem;
import com.hexvane.kindredvalemod.permissions.KindredValePermissions;
import com.hexvane.kindredvalemod.teleport.WorldTeleportService;
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
    private static final String WORLD_DESTINATIONS_CONFIG_FILE = "WorldDestinations.json";
    private static final String HEXVANE_NPC_CONFIG_FILE = "HexvaneNpc.json";

    private final Config<HubProtectionConfig> hubProtectionConfig =
        withConfig("HubProtection", HubProtectionConfig.CODEC);
    private final Config<WorldDestinationsConfig> worldDestinationsConfig =
        withConfig("WorldDestinations", WorldDestinationsConfig.CODEC);
    private final Config<HexvaneNpcConfig> hexvaneNpcConfig =
        withConfig("HexvaneNpc", HexvaneNpcConfig.CODEC);

    private HubProtectionService hubProtectionService;
    private WorldTeleportService worldTeleportService;
    private AetherhavenIntegration aetherhavenIntegration;

    public KindredValeModPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("KindredValeMod loaded (v%s)", this.getManifest().getVersion());
    }

    @Override
    protected void setup() {
        KindredValePermissions.register(getBasePermission());

        hubProtectionService = new HubProtectionService(hubProtectionConfig);
        worldTeleportService = new WorldTeleportService(worldDestinationsConfig);
        aetherhavenIntegration = new AetherhavenIntegration(worldTeleportService, hexvaneNpcConfig);

        final var entityStoreRegistry = getEntityStoreRegistry();
        entityStoreRegistry.registerSystem(new HubBlockBreakProtectionSystem(hubProtectionService));
        entityStoreRegistry.registerSystem(new HubBlockPlaceProtectionSystem(hubProtectionService));
        entityStoreRegistry.registerSystem(new HubBlockDamageProtectionSystem(hubProtectionService));

        getEventRegistry().registerGlobal(StartWorldEvent.class, this::onWorldStart);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        getCommandRegistry().registerCommand(new HubBuildCommand(hubProtectionService));
        getCommandRegistry().registerCommand(new HubCommand(worldTeleportService));
    }

    @Override
    protected void start() {
        ensureDefaultConfig(hubProtectionConfig, HUB_PROTECTION_CONFIG_FILE);
        ensureDefaultConfig(worldDestinationsConfig, WORLD_DESTINATIONS_CONFIG_FILE);
        ensureDefaultConfig(hexvaneNpcConfig, HEXVANE_NPC_CONFIG_FILE);

        if (aetherhavenIntegration != null) {
            aetherhavenIntegration.register();
        }

        final var permissionsModule = PermissionsModule.get();
        if (permissionsModule != null) {
            permissionsModule.refreshVirtualGroups();
        }

        for (final var world : Universe.get().getWorlds().values()) {
            hubProtectionService.applyPvpSettings(world);
            if (aetherhavenIntegration != null) {
                aetherhavenIntegration.ensureHexvanePlacements(world);
            }
        }

        LOGGER.atInfo().log(
            "Hub protection enabled for worlds: %s",
            String.join(", ", hubProtectionConfig.get().getProtectedWorlds())
        );
    }

    @Override
    protected void shutdown() {
        if (aetherhavenIntegration != null) {
            aetherhavenIntegration.unregister();
        }
    }

    private void onWorldStart(@Nonnull final StartWorldEvent event) {
        hubProtectionService.applyPvpSettings(event.getWorld());
        if (aetherhavenIntegration != null) {
            aetherhavenIntegration.ensureHexvanePlacements(event.getWorld());
        }
    }

    private void onPlayerDisconnect(@Nonnull final PlayerDisconnectEvent event) {
        hubProtectionService.clearBuildMode(event.getPlayerRef().getUuid());
    }

    private void ensureDefaultConfig(@Nonnull Config<?> config, @Nonnull String fileName) {
        final Path configPath = getDataDirectory().resolve(fileName);
        if (Files.exists(configPath)) {
            return;
        }

        config.save().join();
        LOGGER.atInfo().log("Created default config at %s", configPath);
    }
}
