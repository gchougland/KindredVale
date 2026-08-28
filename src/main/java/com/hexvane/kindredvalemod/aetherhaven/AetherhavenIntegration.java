package com.hexvane.kindredvalemod.aetherhaven;

import com.hexvane.kindredvalemod.config.HexvaneNpcConfig;
import com.hexvane.kindredvalemod.teleport.WorldTeleportService;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Wires Kindred Vale into Aetherhaven without linking Aetherhaven classes into this plugin's
 * classloader. Hytale classpath plugins often resolve on the application loader, which cannot see
 * sibling mod jars, so all Aetherhaven API access goes through {@link PluginManager} + reflection
 * on Aetherhaven's own classloader.
 */
public final class AetherhavenIntegration {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final PluginIdentifier AETHERHAVEN_ID = new PluginIdentifier("Hexvane", "Aetherhaven");
    private static final String ACTION_TELEPORT_WORLD = "teleport_world";
    private static final String CONDITION_DESTINATION_AVAILABLE = "kindred_destination_available";

    private final WorldTeleportService teleportService;
    private final Config<HexvaneNpcConfig> hexvaneNpcConfig;
    private boolean registered;

    public AetherhavenIntegration(
        @Nonnull WorldTeleportService teleportService,
        @Nonnull Config<HexvaneNpcConfig> hexvaneNpcConfig
    ) {
        this.teleportService = teleportService;
        this.hexvaneNpcConfig = hexvaneNpcConfig;
    }

    public void register() {
        Object ah = resolveAetherhavenPlugin();
        if (ah == null) {
            LOGGER.atWarning().log("Aetherhaven not present; Hexvane world NPC integration skipped");
            return;
        }

        try {
            ClassLoader ahLoader = ah.getClass().getClassLoader();
            Object actionRegistry = invoke(ah, "getDialogueActionRegistry");
            Object conditionRegistry = invoke(ah, "getDialogueConditionRegistry");

            Class<?> actionHandlerType =
                Class.forName("com.hexvane.aetherhaven.plugin.DialogueActionRegistry$Handler", true, ahLoader);
            Class<?> conditionHandlerType =
                Class.forName("com.hexvane.aetherhaven.plugin.DialogueConditionRegistry$Handler", true, ahLoader);

            Object actionHandler = Proxy.newProxyInstance(
                ahLoader,
                new Class<?>[] { actionHandlerType },
                (proxy, method, args) -> {
                    if ("run".equals(method.getName()) && args != null && args.length >= 4) {
                        handleTeleportWorld(args[0], args[1], args[2], args[3]);
                        return null;
                    }
                    return defaultProxyReturn(method);
                }
            );

            Object conditionHandler = Proxy.newProxyInstance(
                ahLoader,
                new Class<?>[] { conditionHandlerType },
                (proxy, method, args) -> {
                    if ("test".equals(method.getName()) && args != null && args.length >= 3) {
                        return isDestinationAvailable(args[0], args[2]);
                    }
                    return defaultProxyReturn(method);
                }
            );

            invoke(actionRegistry, "register", new Class<?>[] { String.class, actionHandlerType }, ACTION_TELEPORT_WORLD, actionHandler);
            invoke(
                conditionRegistry,
                "register",
                new Class<?>[] { String.class, conditionHandlerType },
                CONDITION_DESTINATION_AVAILABLE,
                conditionHandler
            );
            registered = true;
            LOGGER.atInfo().log(
                "Registered Aetherhaven dialogue action %s and condition %s",
                ACTION_TELEPORT_WORLD,
                CONDITION_DESTINATION_AVAILABLE
            );
        } catch (ReflectiveOperationException e) {
            LOGGER.atSevere().withCause(e).log("Failed to register Aetherhaven dialogue hooks");
        }
    }

    public void unregister() {
        if (!registered) {
            return;
        }
        Object ah = resolveAetherhavenPlugin();
        if (ah == null) {
            registered = false;
            return;
        }
        try {
            invoke(invoke(ah, "getDialogueActionRegistry"), "unregister", new Class<?>[] { String.class }, ACTION_TELEPORT_WORLD);
            invoke(
                invoke(ah, "getDialogueConditionRegistry"),
                "unregister",
                new Class<?>[] { String.class },
                CONDITION_DESTINATION_AVAILABLE
            );
        } catch (ReflectiveOperationException e) {
            LOGGER.atWarning().withCause(e).log("Failed to unregister Aetherhaven dialogue hooks");
        }
        registered = false;
    }

    public void ensureHexvanePlacements(@Nonnull World world) {
        if (!registered) {
            return;
        }
        Object ah = resolveAetherhavenPlugin();
        if (ah == null) {
            return;
        }

        for (HexvaneNpcConfig.Placement placement : hexvaneNpcConfig.get().getPlacements()) {
            if (placement == null || !placement.getWorldName().equalsIgnoreCase(world.getName())) {
                continue;
            }
            ensurePlacement(ah, world, placement);
        }
    }

    private void ensurePlacement(
        @Nonnull Object ah,
        @Nonnull World world,
        @Nonnull HexvaneNpcConfig.Placement cfg
    ) {
        try {
            Object npcs = invoke(ah, "getWorldNpcService");
            Object existing = invoke(
                npcs,
                "getPlacement",
                new Class<?>[] { World.class, String.class },
                world,
                cfg.getPlacementId()
            );
            if (existing != null) {
                invoke(existing, "setPortraitIcon", new Class<?>[] { String.class }, cfg.getPortraitIcon());
                invoke(existing, "setDisplayName", new Class<?>[] { String.class }, cfg.getDisplayName());
                Object registry = invoke(npcs, "registry", new Class<?>[] { World.class }, world);
                Method upsert = findMethod(registry.getClass(), "upsertPlacement", 1);
                if (upsert != null) {
                    upsert.invoke(registry, existing);
                    invoke(npcs, "save", new Class<?>[] { World.class }, world);
                }
                // respawn clears a stale entityUuid that ensure() treats as "still in an unloaded chunk"
                // and therefore never re-spawns.
                invoke(npcs, "respawn", new Class<?>[] { World.class, String.class }, world, cfg.getPlacementId());
                LOGGER.atInfo().log("Respawned Hexvane world NPC placement %s in %s", cfg.getPlacementId(), world.getName());
                return;
            }

            Object placed = invoke(
                npcs,
                "place",
                new Class<?>[] {
                    World.class,
                    String.class,
                    String.class,
                    double.class,
                    double.class,
                    double.class,
                    float.class
                },
                world,
                cfg.getPlacementId(),
                cfg.getNpcRoleId(),
                cfg.getX(),
                cfg.getY(),
                cfg.getZ(),
                cfg.getYawDegrees()
            );

            invoke(placed, "setDisplayName", new Class<?>[] { String.class }, cfg.getDisplayName());
            invoke(placed, "setPortraitIcon", new Class<?>[] { String.class }, cfg.getPortraitIcon());

            ClassLoader ahLoader = ah.getClass().getClassLoader();
            Class<?> scheduleModeClass =
                Class.forName("com.hexvane.aetherhaven.worldnpc.WorldNpcScheduleMode", true, ahLoader);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object staticMode = Enum.valueOf((Class) scheduleModeClass, "STATIC");
            invoke(placed, "setScheduleMode", new Class<?>[] { scheduleModeClass }, staticMode);
            invoke(placed, "setShopEnabled", new Class<?>[] { boolean.class }, false);

            Object registry = invoke(npcs, "registry", new Class<?>[] { World.class }, world);
            Method upsert = findMethod(registry.getClass(), "upsertPlacement", 1);
            if (upsert == null) {
                throw new NoSuchMethodException("upsertPlacement(1) on " + registry.getClass().getName());
            }
            upsert.invoke(registry, placed);

            invoke(npcs, "save", new Class<?>[] { World.class }, world);
            invoke(npcs, "respawn", new Class<?>[] { World.class, String.class }, world, cfg.getPlacementId());
            LOGGER.atInfo().log(
                "Placed Hexvane world NPC %s (%s) at %.1f %.1f %.1f in %s",
                cfg.getPlacementId(),
                cfg.getNpcRoleId(),
                cfg.getX(),
                cfg.getY(),
                cfg.getZ(),
                world.getName()
            );
        } catch (ReflectiveOperationException e) {
            LOGGER.atSevere().withCause(e).log(
                "Failed to place Hexvane world NPC %s in %s",
                cfg.getPlacementId(),
                world.getName()
            );
        }
    }

    @SuppressWarnings("unchecked")
    private void handleTeleportWorld(
        @Nonnull Object actionJson,
        @Nonnull Object playerRefObj,
        @Nonnull Object storeObj,
        @Nonnull Object outObj
    ) throws ReflectiveOperationException {
        Ref<EntityStore> playerRef = (Ref<EntityStore>) playerRefObj;
        Store<EntityStore> store = (Store<EntityStore>) storeObj;

        String destinationId = readJsonString(actionJson, "destinationId");
        invoke(outObj, "setCloseDialogue", new Class<?>[] { boolean.class }, true);
        invoke(outObj, "setAfterClose", new Class<?>[] { Runnable.class }, (Runnable) () ->
            teleportService.teleportToDestination(playerRef, store, destinationId != null ? destinationId : "")
        );
    }

    @SuppressWarnings("unchecked")
    private boolean isDestinationAvailable(@Nonnull Object conditionJson, @Nonnull Object storeObj) {
        Store<EntityStore> store = (Store<EntityStore>) storeObj;
        String destinationId = readJsonString(conditionJson, "destinationId");
        return teleportService.isDestinationAvailable(destinationId, store);
    }

    @Nullable
    private static Object resolveAetherhavenPlugin() {
        PluginManager manager = PluginManager.get();
        if (manager == null) {
            return null;
        }
        PluginBase base = manager.getPlugin(AETHERHAVEN_ID);
        if (!(base instanceof JavaPlugin)) {
            return null;
        }
        try {
            ClassLoader ahLoader = base.getClass().getClassLoader();
            Class<?> ahClass = Class.forName("com.hexvane.aetherhaven.AetherhavenPlugin", true, ahLoader);
            Object instance = ahClass.getMethod("get").invoke(null);
            return instance != null ? instance : base;
        } catch (ReflectiveOperationException e) {
            LOGGER.atWarning().withCause(e).log("Could not resolve AetherhavenPlugin via reflection");
            return null;
        }
    }

    @Nullable
    private static String readJsonString(@Nonnull Object json, @Nonnull String key) {
        try {
            Boolean has = (Boolean) json.getClass().getMethod("has", String.class).invoke(json, key);
            if (has == null || !has) {
                return null;
            }
            Object element = json.getClass().getMethod("get", String.class).invoke(json, key);
            if (element == null) {
                return null;
            }
            Boolean isNull = (Boolean) element.getClass().getMethod("isJsonNull").invoke(element);
            if (Boolean.TRUE.equals(isNull)) {
                return null;
            }
            return (String) element.getClass().getMethod("getAsString").invoke(element);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nonnull
    private static Object invoke(@Nonnull Object target, @Nonnull String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }

    @Nullable
    private static Object invoke(
        @Nonnull Object target,
        @Nonnull String method,
        @Nonnull Class<?>[] paramTypes,
        @Nullable Object... args
    ) throws ReflectiveOperationException {
        Method m = target.getClass().getMethod(method, paramTypes);
        return m.invoke(target, args);
    }

    @Nullable
    private static Method findMethod(@Nonnull Class<?> type, @Nonnull String name, int paramCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        return null;
    }

    @Nullable
    private static Object defaultProxyReturn(@Nonnull Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }
}
