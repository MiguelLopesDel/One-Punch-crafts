package com.onepunchcrafts.v3.minecraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.core.state.PowerState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Map;

/** Versioned Minecraft persistence Adapter for the pure PowerState aggregate. */
public final class PowerStateCodec {
    private static final int VERSION = 1;
    private static final Codec<ResourceSnapshot> RESOURCE = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("current").forGetter(ResourceSnapshot::current),
            Codec.DOUBLE.fieldOf("maximum").forGetter(ResourceSnapshot::maximum),
            Codec.DOUBLE.fieldOf("regen").forGetter(ResourceSnapshot::regeneration)
    ).apply(instance, ResourceSnapshot::new));
    private static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("version").forGetter(Snapshot::version),
            Id.CODEC.fieldOf("power_set").forGetter(Snapshot::powerSet),
            Id.CODEC.optionalFieldOf("selected", PowerState.NONE).forGetter(Snapshot::selected),
            Codec.INT.optionalFieldOf("selected_group", 0).forGetter(Snapshot::selectedGroup),
            Codec.unboundedMap(Id.CODEC, Codec.DOUBLE).fieldOf("attributes").forGetter(Snapshot::attributes),
            Codec.unboundedMap(Id.CODEC, RESOURCE).fieldOf("resources").forGetter(Snapshot::resources),
            Id.CODEC.listOf().fieldOf("tags").forGetter(Snapshot::tags)
    ).apply(instance, Snapshot::new));

    private PowerStateCodec() {}

    public static CompoundTag encode(PowerState state) {
        Map<Id, ResourceSnapshot> resources = state.resources().values().entrySet().stream().collect(
                java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> new ResourceSnapshot(
                        entry.getValue().current(), entry.getValue().maximum(), entry.getValue().regenerationPerTick())));
        Snapshot snapshot = new Snapshot(VERSION, state.powerSetId(),
                state.abilities().selectedAbility() == null ? PowerState.NONE : state.abilities().selectedAbility(),
                state.abilities().selectedGroup(),
                state.attributes().bases(), resources, List.copyOf(state.tags().values()));
        DataResult<Tag> encoded = CODEC.encodeStart(NbtOps.INSTANCE, snapshot);
        return (CompoundTag) encoded.getOrThrow(false, message -> {
            throw new IllegalStateException("Cannot encode v3 power state: " + message);
        });
    }

    public static void decodeInto(CompoundTag tag, PowerState state) {
        Snapshot snapshot = CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(false, message -> {
            throw new IllegalArgumentException("Cannot decode v3 power state: " + message);
        });
        if (snapshot.version > VERSION) throw new IllegalArgumentException("Unsupported v3 state version " + snapshot.version);
        state.reset();
        state.powerSetId(snapshot.powerSet);
        snapshot.attributes.forEach(state.attributes()::setBase);
        snapshot.resources.forEach((id, resource) -> state.resources().define(id,
                resource.current, resource.maximum, resource.regeneration));
        snapshot.tags.forEach(state.tags()::add);
        state.abilities().selectedGroup(snapshot.selectedGroup);
        if (!snapshot.selected.equals(PowerState.NONE)) state.abilities().select(snapshot.selected);
        state.consumeDirty();
    }

    private record ResourceSnapshot(double current, double maximum, double regeneration) {}
    private record Snapshot(int version, Id powerSet, Id selected, int selectedGroup, Map<Id, Double> attributes,
                            Map<Id, ResourceSnapshot> resources, List<Id> tags) {}
}
