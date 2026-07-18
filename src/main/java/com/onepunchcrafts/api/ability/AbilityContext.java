package com.onepunchcrafts.api.ability;

import com.onepunchcrafts.api.Id;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Snapshot supplied by an environment Adapter when an ability starts. */
public record AbilityContext(
        String actorId,
        long gameTick,
        Optional<String> primaryTarget,
        List<String> capturedTargets,
        Id powerSetId,
        Map<String, Double> parameters
) {
    public AbilityContext {
        primaryTarget = primaryTarget == null ? Optional.empty() : primaryTarget;
        capturedTargets = List.copyOf(capturedTargets);
        parameters = Map.copyOf(parameters);
    }
}
