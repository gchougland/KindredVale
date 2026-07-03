package com.hexvane.kindredvalemod.hub.systems;

import com.hexvane.kindredvalemod.hub.HubProtectionService;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;

import javax.annotation.Nonnull;

public final class HubBlockDamageProtectionSystem extends HubPlayerBlockProtectionSystem<DamageBlockEvent> {

    public HubBlockDamageProtectionSystem(@Nonnull final HubProtectionService hubProtection) {
        super(DamageBlockEvent.class, hubProtection);
    }
}
