package com.hexvane.kindredvalemod.hub.systems;

import com.hexvane.kindredvalemod.hub.HubProtectionService;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Cancels player block events in protected hub worlds unless the player has hub build permission.
 */
public abstract class HubPlayerBlockProtectionSystem<EventType extends CancellableEcsEvent>
    extends EntityEventSystem<EntityStore, EventType> {

    @Nonnull
    private static final Query<EntityStore> QUERY = Query.and(
        Player.getComponentType(),
        UUIDComponent.getComponentType()
    );

    @Nonnull
    private final HubProtectionService hubProtection;

    protected HubPlayerBlockProtectionSystem(
        @Nonnull final Class<EventType> eventType,
        @Nonnull final HubProtectionService hubProtection
    ) {
        super(eventType);
        this.hubProtection = hubProtection;
    }

    @Override
    public void handle(
        final int index,
        @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final CommandBuffer<EntityStore> commandBuffer,
        @Nonnull final EventType event
    ) {
        final var world = store.getExternalData().getWorld();
        if (!hubProtection.isProtectedWorld(world)) {
            return;
        }

        final var entityRef = archetypeChunk.getReferenceTo(index);
        final var uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }

        if (hubProtection.canBuildInHub(uuidComponent.getUuid())) {
            return;
        }

        event.setCancelled(true);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }
}
