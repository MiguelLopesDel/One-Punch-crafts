package com.onepunchcrafts.v3.core.ability;

import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.ability.Timeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Owns cooldown and resumable timeline state for one player. */
public final class AbilityBook {
    private Id selectedAbility;
    private int selectedGroup;
    private final Map<Id, Long> cooldownUntil = new HashMap<>();
    private final List<RunningTimeline> running = new ArrayList<>();
    private boolean dirty;

    public Id selectedAbility() { return selectedAbility; }
    public int selectedGroup() { return selectedGroup; }
    public void selectedGroup(int group) { if (selectedGroup != group) { selectedGroup = group; dirty = true; } }
    public void select(Id ability) { if (!ability.equals(selectedAbility)) { selectedAbility = ability; dirty = true; } }
    public boolean isReady(Id ability, long now) { return cooldownUntil.getOrDefault(ability, 0L) <= now; }
    public void cooldown(Id ability, long until) { cooldownUntil.put(ability, until); dirty = true; }

    public void start(Timeline timeline, long now, String primaryTarget, List<String> targets, Map<String, Double> parameters) {
        running.add(new RunningTimeline(timeline, now, 0, primaryTarget, List.copyOf(targets), Map.copyOf(parameters)));
        dirty = true;
    }

    public void tick(long now, Consumer<Emission> sink) {
        running.sort(Comparator.comparingLong(RunningTimeline::startedAt));
        for (int index = running.size() - 1; index >= 0; index--) {
            RunningTimeline active = running.get(index);
            long elapsed = now - active.startedAt;
            int cursor = active.cursor;
            List<Timeline.Step> steps = active.timeline.steps();
            while (cursor < steps.size() && steps.get(cursor).tick() <= elapsed) {
                sink.accept(new Emission(active.timeline.id(), active.startedAt, steps.get(cursor), active.primaryTarget,
                        active.capturedTargets, active.parameters));
                cursor++;
            }
            if (elapsed >= active.timeline.durationTicks()) running.remove(index);
            else if (cursor != active.cursor) running.set(index, active.withCursor(cursor));
            dirty = true;
        }
    }

    public List<RunningTimeline> running() { return List.copyOf(running); }
    public void clear() {
        if (selectedAbility != null || !cooldownUntil.isEmpty() || !running.isEmpty()) {
            selectedAbility = null;
            selectedGroup = 0;
            cooldownUntil.clear();
            running.clear();
            dirty = true;
        }
    }
    public boolean consumeDirty() { boolean value = dirty; dirty = false; return value; }

    public record RunningTimeline(Timeline timeline, long startedAt, int cursor, String primaryTarget,
                                  List<String> capturedTargets, Map<String, Double> parameters) {
        private RunningTimeline withCursor(int next) {
            return new RunningTimeline(timeline, startedAt, next, primaryTarget, capturedTargets, parameters);
        }
    }

    public record Emission(Id timelineId, long startTick, Timeline.Step step, String primaryTarget,
                           List<String> capturedTargets, Map<String, Double> parameters) {}
}
