package com.hexvane.kindredvalemod.hub.systems;

import com.hexvane.kindredvalemod.hub.HubProtectionService;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;

import javax.annotation.Nonnull;

public final class HubBlockPlaceProtectionSystem extends HubPlayerBlockProtectionSystem<PlaceBlockEvent> {

    public HubBlockPlaceProtectionSystem(@Nonnull final HubProtectionService hubProtection) {
        super(PlaceBlockEvent.class, hubProtection);
    }
}
