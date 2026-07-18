package com.onepunchcrafts.v3.api.effect;

import com.onepunchcrafts.v3.api.Id;

import java.util.List;
import java.util.Objects;

/** Data-only effect; persistence never serializes code or lambdas. */
public record EffectSpec(
        Id id,
        int durationTicks,
        int periodTicks,
        StackPolicy stackPolicy,
        List<Operation> onApply,
        List<Operation> periodic,
        List<Operation> onExpire
) {
    public EffectSpec {
        Objects.requireNonNull(id);
        Objects.requireNonNull(stackPolicy);
        onApply = List.copyOf(onApply);
        periodic = List.copyOf(periodic);
        onExpire = List.copyOf(onExpire);
        if (durationTicks < 0 || periodTicks < 0) throw new IllegalArgumentException("Negative effect timing");
        if (!periodic.isEmpty() && periodTicks == 0) throw new IllegalArgumentException("Periodic effect needs a period");
    }

    public enum StackPolicy { REJECT, REFRESH, STACK }

    public sealed interface Operation permits Operation.AttributeDelta, Operation.ResourceDelta,
            Operation.AddTag, Operation.RemoveTag, Operation.Cue {
        record AttributeDelta(Id attribute, double amount) implements Operation {}
        record ResourceDelta(Id resource, double amount) implements Operation {}
        record AddTag(Id tag) implements Operation {}
        record RemoveTag(Id tag) implements Operation {}
        record Cue(Id cue) implements Operation {}
    }
}
