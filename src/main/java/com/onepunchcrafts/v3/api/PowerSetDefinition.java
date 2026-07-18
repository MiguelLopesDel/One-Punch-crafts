package com.onepunchcrafts.v3.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record PowerSetDefinition(
        Id id,
        Map<Id, Double> baseAttributes,
        Map<Id, ResourceDefinition> resources,
        Set<Id> identityTags,
        List<List<LoadoutEntry>> loadout
) {
    public PowerSetDefinition {
        baseAttributes = Map.copyOf(baseAttributes);
        resources = Map.copyOf(resources);
        identityTags = Set.copyOf(identityTags);
        loadout = loadout.stream().map(List::copyOf).toList();
        if (loadout.isEmpty() || loadout.stream().anyMatch(List::isEmpty))
            throw new IllegalArgumentException("PowerSet needs non-empty loadout groups");
    }

    public LoadoutEntry requireEntry(Id id) {
        return loadout.stream().flatMap(List::stream).filter(entry -> entry.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entry is not owned by " + this.id + ": " + id));
    }

    public sealed interface LoadoutEntry permits AbilityEntry, ToggleEntry, AdjustableEntry { Id id(); }
    public record AbilityEntry(Id id) implements LoadoutEntry {}
    public record ToggleEntry(Id id, Id tag) implements LoadoutEntry {}
    public record AdjustableEntry(Id id, Id attribute, double minimum, double maximum, double step) implements LoadoutEntry {
        public AdjustableEntry {
            if (maximum < minimum || step <= 0) throw new IllegalArgumentException("Invalid adjustable " + id);
        }
    }

    public record ResourceDefinition(double maximum, double initial, double regenerationPerTick) {}
}
