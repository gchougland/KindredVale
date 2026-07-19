package com.hexvane.kindredvalemod.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

/**
 * Named destinations for dialogue {@code teleport_world} actions.
 */
public class WorldDestinationsConfig {

    @Nonnull
    public static final BuilderCodec<WorldDestinationsConfig> CODEC =
        BuilderCodec.builder(WorldDestinationsConfig.class, WorldDestinationsConfig::new)
            .append(
                new KeyedCodec<>("HubDestinationId", Codec.STRING),
                (config, id) -> config.hubDestinationId = id,
                config -> config.hubDestinationId
            )
            .documentation("Destination id used by /hub (default kindredvale).")
            .add()
            .append(
                new KeyedCodec<>("Destinations", new ArrayCodec<>(Destination.CODEC, Destination[]::new)),
                (config, destinations) -> config.destinations = destinations != null ? destinations : new Destination[0],
                config -> config.destinations
            )
            .documentation("World teleport destinations keyed by destinationId in dialogue actions.")
            .add()
            .build();

    @Nonnull
    private Destination[] destinations = defaultDestinations();

    @Nonnull
    private String hubDestinationId = "kindredvale";

    @Nonnull
    public Destination[] getDestinations() {
        return destinations;
    }

    @Nonnull
    public String getHubDestinationId() {
        return hubDestinationId;
    }

    @Nonnull
    public Optional<Destination> findById(@Nullable String destinationId) {
        if (destinationId == null || destinationId.isBlank()) {
            return Optional.empty();
        }
        String id = destinationId.trim();
        return Arrays.stream(destinations)
            .filter(d -> d != null && id.equalsIgnoreCase(d.getId()))
            .findFirst();
    }

    @Nonnull
    private static Destination[] defaultDestinations() {
        Destination hub = new Destination();
        hub.id = "kindredvale";
        hub.worldName = "KindredVale";
        hub.useWorldSpawn = true;

        Destination smp = new Destination();
        smp.id = "smp";
        smp.worldName = "survival";
        smp.useWorldSpawn = true;

        return new Destination[] { hub, smp };
    }

    public static class Destination {
        @Nonnull
        public static final BuilderCodec<Destination> CODEC =
            BuilderCodec.builder(Destination.class, Destination::new)
                .append(
                    new KeyedCodec<>("Id", Codec.STRING),
                    (d, v) -> d.id = v,
                    d -> d.id
                )
                .documentation("Stable id referenced by dialogue teleport_world.destinationId.")
                .add()
                .append(
                    new KeyedCodec<>("WorldName", Codec.STRING),
                    (d, v) -> d.worldName = v,
                    d -> d.worldName
                )
                .documentation("Hytale world name to teleport into.")
                .add()
                .append(
                    new KeyedCodec<>("UseWorldSpawn", Codec.BOOLEAN),
                    (d, v) -> d.useWorldSpawn = v,
                    d -> d.useWorldSpawn
                )
                .documentation("When true, use the world spawn provider; otherwise use X/Y/Z/YawDegrees.")
                .add()
                .append(
                    new KeyedCodec<>("X", Codec.DOUBLE),
                    (d, v) -> d.x = v,
                    d -> d.x
                )
                .documentation("Fixed spawn X when UseWorldSpawn is false.")
                .add()
                .append(
                    new KeyedCodec<>("Y", Codec.DOUBLE),
                    (d, v) -> d.y = v,
                    d -> d.y
                )
                .documentation("Fixed spawn Y when UseWorldSpawn is false.")
                .add()
                .append(
                    new KeyedCodec<>("Z", Codec.DOUBLE),
                    (d, v) -> d.z = v,
                    d -> d.z
                )
                .documentation("Fixed spawn Z when UseWorldSpawn is false.")
                .add()
                .append(
                    new KeyedCodec<>("YawDegrees", Codec.FLOAT),
                    (d, v) -> d.yawDegrees = v,
                    d -> d.yawDegrees
                )
                .documentation("Fixed facing yaw in degrees when UseWorldSpawn is false.")
                .add()
                .build();

        @Nonnull
        private String id = "";

        @Nonnull
        private String worldName = "";

        private boolean useWorldSpawn = true;

        private double x;
        private double y;
        private double z;
        private float yawDegrees;

        @Nonnull
        public String getId() {
            return id;
        }

        @Nonnull
        public String getWorldName() {
            return worldName;
        }

        public boolean isUseWorldSpawn() {
            return useWorldSpawn;
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
