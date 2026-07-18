package com.onepunchcrafts.runtime.state;

import com.onepunchcrafts.api.Id;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Reference-counted tags allow independent effects to contribute the same state. */
public final class TagSet {
    private final Map<Id, Integer> counts = new HashMap<>();
    private boolean dirty;

    public void add(Id tag) { counts.merge(tag, 1, Integer::sum); dirty = true; }
    public void remove(Id tag) {
        counts.computeIfPresent(tag, (ignored, count) -> count <= 1 ? null : count - 1);
        dirty = true;
    }
    public boolean contains(Id tag) { return counts.containsKey(tag); }
    public Set<Id> values() { return Set.copyOf(counts.keySet()); }
    public void clear() { if (!counts.isEmpty()) { counts.clear(); dirty = true; } }
    public boolean consumeDirty() { boolean value = dirty; dirty = false; return value; }
}
