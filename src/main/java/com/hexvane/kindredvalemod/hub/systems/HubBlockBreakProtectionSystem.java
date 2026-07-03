package com.hexvane.kindredvalemod.hub.systems;

import com.hexvane.kindredvalemod.hub.HubProtectionService;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;

import javax.annotation.Nonnull;

public final class HubBlockBreakProtectionSystem extends HubPlayerBlockProtectionSystem<BreakBlockEvent> {

    public HubBlockBreakProtectionSystem(@Nonnull final HubProtectionService hubProtection) {
        super(BreakBlockEvent.class, hubProtection);
    }
}
