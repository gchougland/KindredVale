package com.hexvane.kindredvalemod.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Configuration for hub world spawn protection.
 */
public class HubProtectionConfig {

    @Nonnull
    public static final BuilderCodec<HubProtectionConfig> CODEC = BuilderCodec.builder(HubProtectionConfig.class, HubProtectionConfig::new)
        .append(
            new KeyedCodec<>("ProtectedWorlds", Codec.STRING_ARRAY),
            (config, worlds) -> config.protectedWorlds = worlds,
            config -> config.protectedWorlds
        )
        .documentation("World names where block breaking, placement, and punching are restricted.")
        .add()
        .append(
            new KeyedCodec<>("DisablePvpInProtectedWorlds", Codec.BOOLEAN),
            (config, disable) -> config.disablePvpInProtectedWorlds = disable,
            config -> config.disablePvpInProtectedWorlds
        )
        .documentation("When true, PvP is disabled on protected worlds when they start.")
        .add()
        .build();

    @Nonnull
    private String[] protectedWorlds = new String[] { "KindredVale" };

    private boolean disablePvpInProtectedWorlds = true;

    @Nonnull
    public String[] getProtectedWorlds() {
        return protectedWorlds;
    }

    public boolean isDisablePvpInProtectedWorlds() {
        return disablePvpInProtectedWorlds;
    }
}
