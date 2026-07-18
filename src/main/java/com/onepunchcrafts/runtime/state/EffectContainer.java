package com.onepunchcrafts.runtime.state;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.effect.EffectSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Consumer;

/** Active effects indexed by id and expiry; no world-wide scans. */
public final class EffectContainer {
    private final Map<Id, List<ActiveEffect>> active = new HashMap<>();
    private final PriorityQueue<ActiveEffect> expirations = new PriorityQueue<>(Comparator.comparingLong(ActiveEffect::expiresAt));
    private boolean dirty;

    public boolean apply(EffectSpec spec, long now, Consumer<EffectSpec.Operation> sink) {
        List<ActiveEffect> instances = active.computeIfAbsent(spec.id(), ignored -> new ArrayList<>());
        if (!instances.isEmpty()) {
            if (spec.stackPolicy() == EffectSpec.StackPolicy.REJECT) return false;
            if (spec.stackPolicy() == EffectSpec.StackPolicy.REFRESH) {
                ActiveEffect old = instances.remove(0);
                expirations.remove(old);
            }
        }
        ActiveEffect effect = new ActiveEffect(spec, now, now + spec.durationTicks(), now);
        instances.add(effect);
        expirations.add(effect);
        spec.onApply().forEach(sink);
        dirty = true;
        return true;
    }

    public void tick(long now, Consumer<EffectSpec.Operation> sink) {
        for (List<ActiveEffect> values : List.copyOf(active.values())) {
            for (int i = 0; i < values.size(); i++) {
                ActiveEffect effect = values.get(i);
                if (effect.spec.periodTicks() > 0 && now >= effect.nextPeriod + effect.spec.periodTicks() && now < effect.expiresAt) {
                    effect.spec.periodic().forEach(sink);
                    ActiveEffect advanced = effect.withNextPeriod(now);
                    values.set(i, advanced);
                    expirations.remove(effect);
                    expirations.add(advanced);
                }
            }
        }
        while (!expirations.isEmpty() && expirations.peek().expiresAt <= now) {
            ActiveEffect expired = expirations.poll();
            List<ActiveEffect> instances = active.get(expired.spec.id());
            if (instances == null || !instances.remove(expired)) continue;
            expired.spec.onExpire().forEach(sink);
            if (instances.isEmpty()) active.remove(expired.spec.id());
            dirty = true;
        }
    }

    public int count(Id effect) { return active.getOrDefault(effect, List.of()).size(); }
    public void clear() { if (!active.isEmpty()) { active.clear(); expirations.clear(); dirty = true; } }
    public boolean consumeDirty() { boolean value = dirty; dirty = false; return value; }

    private record ActiveEffect(EffectSpec spec, long appliedAt, long expiresAt, long nextPeriod) {
        private ActiveEffect withNextPeriod(long tick) { return new ActiveEffect(spec, appliedAt, expiresAt, tick); }
    }
}
