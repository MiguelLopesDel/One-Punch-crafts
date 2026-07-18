package com.onepunchcrafts.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PowerSetDefinition(
        Id id,
        Map<Id, Double> baseAttributes,
        Map<Id, ResourceDefinition> resources,
        Set<Id> identityTags,
        List<TechniquePage> techniquePages
) {
    public static final int MAX_TECHNIQUES_PER_PAGE = 8;

    public PowerSetDefinition {
        baseAttributes = Map.copyOf(baseAttributes);
        resources = Map.copyOf(resources);
        identityTags = Set.copyOf(identityTags);
        techniquePages = List.copyOf(techniquePages);
        if (techniquePages.isEmpty()) throw new IllegalArgumentException("PowerSet needs a technique page");
        Set<Id> unique = new HashSet<>();
        for (TechniquePage page : techniquePages) {
            if (page.techniques().isEmpty() || page.techniques().size() > MAX_TECHNIQUES_PER_PAGE)
                throw new IllegalArgumentException("Technique pages need 1-" + MAX_TECHNIQUES_PER_PAGE + " entries");
            for (Id technique : page.techniques()) {
                if (!unique.add(technique)) throw new IllegalArgumentException("Duplicate technique " + technique);
            }
        }
    }

    public Id requireTechnique(Id id) {
        return techniquePages.stream().flatMap(page -> page.techniques().stream())
                .filter(id::equals).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Technique is not owned by " + this.id + ": " + id));
    }

    public int pageOf(Id technique) {
        for (int index = 0; index < techniquePages.size(); index++)
            if (techniquePages.get(index).techniques().contains(technique)) return index;
        return -1;
    }

    public record TechniquePage(String titleKey, List<Id> techniques) {
        public TechniquePage { techniques = List.copyOf(techniques); }
    }

    public record ResourceDefinition(double maximum, double initial, double regenerationPerTick) {}
}
