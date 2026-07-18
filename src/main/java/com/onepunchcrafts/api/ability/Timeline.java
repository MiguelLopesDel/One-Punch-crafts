package com.onepunchcrafts.api.ability;

import com.onepunchcrafts.api.Id;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable, serializable schedule shared by gameplay and presentation. */
public record Timeline(Id id, int durationTicks, List<Step> steps) {
    public Timeline {
        Objects.requireNonNull(id);
        if (durationTicks < 0) throw new IllegalArgumentException("Negative timeline duration");
        steps = steps.stream().sorted(Comparator.comparingInt(Step::tick)).toList();
        if (steps.stream().anyMatch(step -> step.tick < 0 || step.tick > durationTicks))
            throw new IllegalArgumentException("Timeline step outside duration");
    }

    public static Builder builder(Id id, int durationTicks) { return new Builder(id, durationTicks); }

    public record Step(int tick, Command command) {}

    public sealed interface Command permits Command.Cue, Command.StrikeTarget, Command.StrikeCone,
            Command.StrikeArea, Command.StrikeCylinder, Command.DestroyCylinder, Command.TeleportToTarget, Command.Dash {
        record Cue(Id cueId, Params params) implements Command {}
        record StrikeTarget(Id strikeId, TargetRef target) implements Command {}
        record StrikeCone(Id strikeId, double range, double halfAngleDegrees) implements Command {}
        record StrikeCylinder(Id strikeId, double radius, double length) implements Command {}
        record StrikeArea(Id strikeId, double horizontalRadius, double verticalRadius) implements Command {}
        record DestroyCylinder(double radius, double length) implements Command {}
        record TeleportToTarget(TargetRef target) implements Command {}
        record Dash(double distance) implements Command {}
    }

    public enum TargetRef { PRIMARY, NEXT_CAPTURED }

    public record Params(double scale, int color, String text) {
        public static final Params EMPTY = new Params(1, 0xFFFFFF, "");
    }

    public static final class Builder {
        private final Id id;
        private final int duration;
        private final List<Step> steps = new ArrayList<>();
        private Builder(Id id, int duration) { this.id = id; this.duration = duration; }
        public Builder at(int tick, Command command) { steps.add(new Step(tick, command)); return this; }
        public Builder cue(int tick, Id cue) { return at(tick, new Command.Cue(cue, Params.EMPTY)); }
        public Timeline build() { return new Timeline(id, duration, steps); }
    }
}
