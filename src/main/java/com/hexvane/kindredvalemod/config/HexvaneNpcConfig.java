package com.hexvane.kindredvalemod.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;

/**
 * World NPC placements for Hexvane greeters (hub, SMP returner, etc.).
 */
public class HexvaneNpcConfig {

    @Nonnull
    public static final BuilderCodec<HexvaneNpcConfig> CODEC =
        BuilderCodec.builder(HexvaneNpcConfig.class, HexvaneNpcConfig::new)
            .append(
                new KeyedCodec<>("Placements", new ArrayCodec<>(Placement.CODEC, Placement[]::new)),
                (config, placements) -> config.placements = placements != null ? placements : new Placement[0],
                config -> config.placements
            )
            .documentation("Hexvane world NPC placements to ensure on matching worlds.")
            .add()
            .build();

    @Nonnull
    private Placement[] placements = defaultPlacements();

    @Nonnull
    public Placement[] getPlacements() {
        return placements;
    }

    @Nonnull
    private static Placement[] defaultPlacements() {
        Placement hub = new Placement();
        hub.worldName = "KindredVale";
        hub.placementId = "kindredvale_hexvane";
        hub.npcRoleId = "KindredVale_Hexvane";
        hub.displayName = "Hexvane";
        hub.portraitIcon = "KindredVale_Hexvane.png";
        hub.x = 0.5;
        hub.y = 64.0;
        hub.z = 0.5;
        hub.yawDegrees = 0f;

        Placement smp = new Placement();
        smp.worldName = "survival";
        smp.placementId = "smp_hexvane";
        smp.npcRoleId = "KindredVale_Hexvane";
        smp.displayName = "Hexvane";
        smp.portraitIcon = "KindredVale_Hexvane.png";
        smp.x = 0.5;
        smp.y = 64.0;
        smp.z = 0.5;
        smp.yawDegrees = 0f;

        return new Placement[] { hub, smp };
    }

    public static class Placement {
        @Nonnull
        public static final BuilderCodec<Placement> CODEC =
            BuilderCodec.builder(Placement.class, Placement::new)
                .append(
                    new KeyedCodec<>("WorldName", Codec.STRING),
                    (p, v) -> p.worldName = v,
                    p -> p.worldName
                )
                .documentation("World where this Hexvane placement is ensured.")
                .add()
                .append(
                    new KeyedCodec<>("PlacementId", Codec.STRING),
                    (p, v) -> p.placementId = v,
                    p -> p.placementId
                )
                .documentation("Stable Aetherhaven world NPC placement id.")
                .add()
                .append(
                    new KeyedCodec<>("NpcRoleId", Codec.STRING),
                    (p, v) -> p.npcRoleId = v,
                    p -> p.npcRoleId
                )
                .documentation("NPC role id to spawn.")
                .add()
                .append(
                    new KeyedCodec<>("DisplayName", Codec.STRING),
                    (p, v) -> p.displayName = v,
                    p -> p.displayName
                )
                .documentation("Nametag / dialogue speaker override.")
                .add()
                .append(
                    new KeyedCodec<>("PortraitIcon", Codec.STRING),
                    (p, v) -> p.portraitIcon = v,
                    p -> p.portraitIcon
                )
                .documentation("Portrait filename under Icons/ModelsGenerated/.")
                .add()
                .append(
                    new KeyedCodec<>("X", Codec.DOUBLE),
                    (p, v) -> p.x = v,
                    p -> p.x
                )
                .documentation("Default placement X (override with /aetherhaven worldnpc setpose).")
                .add()
                .append(
                    new KeyedCodec<>("Y", Codec.DOUBLE),
                    (p, v) -> p.y = v,
                    p -> p.y
                )
                .documentation("Default placement Y.")
                .add()
                .append(
                    new KeyedCodec<>("Z", Codec.DOUBLE),
                    (p, v) -> p.z = v,
                    p -> p.z
                )
                .documentation("Default placement Z.")
                .add()
                .append(
                    new KeyedCodec<>("YawDegrees", Codec.FLOAT),
                    (p, v) -> p.yawDegrees = v,
                    p -> p.yawDegrees
                )
                .documentation("Default facing yaw in degrees.")
                .add()
                .build();

        @Nonnull
        private String worldName = "KindredVale";

        @Nonnull
        private String placementId = "kindredvale_hexvane";

        @Nonnull
        private String npcRoleId = "KindredVale_Hexvane";

        @Nonnull
        private String displayName = "Hexvane";

        @Nonnull
        private String portraitIcon = "KindredVale_Hexvane.png";

        private double x = 0.5;
        private double y = 64.0;
        private double z = 0.5;
        private float yawDegrees = 0f;

        @Nonnull
        public String getWorldName() {
            return worldName;
        }

        @Nonnull
        public String getPlacementId() {
            return placementId;
        }

        @Nonnull
        public String getNpcRoleId() {
            return npcRoleId;
        }

        @Nonnull
        public String getDisplayName() {
            return displayName;
        }

        @Nonnull
        public String getPortraitIcon() {
            return portraitIcon;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public float getYawDegrees() {
            return yawDegrees;
        }
    }
}
