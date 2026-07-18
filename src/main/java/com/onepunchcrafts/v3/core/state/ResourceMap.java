package com.onepunchcrafts.v3.core.state;

import com.onepunchcrafts.v3.api.Id;

import java.util.HashMap;
import java.util.Map;

public final class ResourceMap {
    private final Map<Id, Pool> pools = new HashMap<>();
    private boolean dirty;

    public void define(Id id, double current, double maximum, double regenerationPerTick) {
        pools.put(id, Pool.validated(current, maximum, regenerationPerTick));
        dirty = true;
    }

    public double current(Id id) { return require(id).current; }
    public double maximum(Id id) { return require(id).maximum; }

    public boolean consume(Id id, double amount) {
        Pool pool = require(id);
        if (amount < 0 || pool.current < amount) return false;
        pools.put(id, pool.withCurrent(pool.current - amount));
        dirty = amount != 0 || dirty;
        return true;
    }

    public void add(Id id, double amount) {
        Pool pool = require(id);
        double next = Math.max(0, Math.min(pool.maximum, pool.current + amount));
        if (next != pool.current) { pools.put(id, pool.withCurrent(next)); dirty = true; }
    }

    public void tick() { pools.forEach((id, pool) -> add(id, pool.regenerationPerTick)); }
    public Map<Id, Pool> values() { return Map.copyOf(pools); }
    public void clear() { if (!pools.isEmpty()) { pools.clear(); dirty = true; } }
    public boolean consumeDirty() { boolean value = dirty; dirty = false; return value; }

    private Pool require(Id id) {
        Pool pool = pools.get(id);
        if (pool == null) throw new IllegalArgumentException("Unknown resource " + id);
        return pool;
    }

    public record Pool(double current, double maximum, double regenerationPerTick) {
        private static Pool validated(double current, double maximum, double regen) {
            if (!Double.isFinite(current) || !Double.isFinite(maximum) || !Double.isFinite(regen) || maximum < 0)
                throw new IllegalArgumentException("Invalid resource pool");
            return new Pool(Math.max(0, Math.min(maximum, current)), maximum, regen);
        }
        private Pool withCurrent(double value) { return new Pool(value, maximum, regenerationPerTick); }
    }
}
