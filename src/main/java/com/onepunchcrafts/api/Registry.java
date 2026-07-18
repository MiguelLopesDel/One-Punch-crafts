package com.onepunchcrafts.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Small deterministic registry. Registration is open; mutation ends at freeze. */
public final class Registry<T> {
    private final String name;
    private final Map<Id, T> entries = new LinkedHashMap<>();
    private boolean frozen;

    public Registry(String name) { this.name = Objects.requireNonNull(name); }

    public synchronized T register(Id id, T value) {
        if (frozen) throw new IllegalStateException(name + " registry is frozen");
        if (entries.putIfAbsent(id, Objects.requireNonNull(value)) != null)
            throw new IllegalArgumentException("Duplicate " + name + " id " + id);
        return value;
    }

    public T require(Id id) {
        T value = entries.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown " + name + " id " + id);
        return value;
    }

    public Collection<Id> ids() { return Collections.unmodifiableSet(entries.keySet()); }
    public synchronized void freeze() { frozen = true; }
    public boolean isFrozen() { return frozen; }
}
