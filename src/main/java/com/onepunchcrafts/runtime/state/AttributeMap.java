package com.onepunchcrafts.runtime.state;

import com.onepunchcrafts.api.Id;

import java.util.HashMap;
import java.util.Map;

public final class AttributeMap {
    private final Map<Id, Double> bases = new HashMap<>();
    private final Map<Id, Map<Id, Modifier>> modifiers = new HashMap<>();
    private boolean dirty;

    public void setBase(Id attribute, double value) {
        requireFinite(value);
        if (!Double.valueOf(value).equals(bases.put(attribute, value))) dirty = true;
    }

    public double base(Id attribute) { return bases.getOrDefault(attribute, 0.0); }

    public void putModifier(Id attribute, Id source, double value, Operation operation) {
        requireFinite(value);
        Modifier replacement = new Modifier(value, operation);
        Modifier old = modifiers.computeIfAbsent(attribute, ignored -> new HashMap<>()).put(source, replacement);
        if (!replacement.equals(old)) dirty = true;
    }

    public void removeModifier(Id attribute, Id source) {
        Map<Id, Modifier> values = modifiers.get(attribute);
        if (values != null && values.remove(source) != null) dirty = true;
    }

    public double value(Id attribute) {
        double base = base(attribute);
        Map<Id, Modifier> values = modifiers.getOrDefault(attribute, Map.of());
        double add = values.values().stream().filter(v -> v.operation == Operation.ADD).mapToDouble(Modifier::value).sum();
        double multiplyBase = values.values().stream().filter(v -> v.operation == Operation.MULTIPLY_BASE).mapToDouble(Modifier::value).sum();
        double multiplyTotal = values.values().stream().filter(v -> v.operation == Operation.MULTIPLY_TOTAL)
                .mapToDouble(v -> 1.0 + v.value).reduce(1.0, (a, b) -> a * b);
        return (base + add + base * multiplyBase) * multiplyTotal;
    }

    public Map<Id, Double> bases() { return Map.copyOf(bases); }
    public void clear() { if (!bases.isEmpty() || !modifiers.isEmpty()) { bases.clear(); modifiers.clear(); dirty = true; } }
    public boolean consumeDirty() { boolean value = dirty; dirty = false; return value; }

    public enum Operation { ADD, MULTIPLY_BASE, MULTIPLY_TOTAL }
    public record Modifier(double value, Operation operation) {}

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Attribute value must be finite");
    }
}
