package com.onepunchcrafts.api;

import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable namespaced identity used by every public registry and save. */
public record Id(String namespace, String path) implements Comparable<Id> {
    public static final Codec<Id> CODEC = Codec.STRING.xmap(Id::parse, Id::toString);
    private static final Pattern PART = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    public Id {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!PART.matcher(namespace).matches() || !PATH.matcher(path).matches())
            throw new IllegalArgumentException("Invalid id: " + namespace + ':' + path);
    }

    public static Id parse(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1)
            throw new IllegalArgumentException("Expected namespace:path, got " + value);
        return new Id(value.substring(0, separator), value.substring(separator + 1));
    }

    @Override public String toString() { return namespace + ':' + path; }
    @Override public int compareTo(Id other) { return toString().compareTo(other.toString()); }
}
