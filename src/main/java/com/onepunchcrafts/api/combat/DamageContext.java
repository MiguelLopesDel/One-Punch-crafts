package com.onepunchcrafts.api.combat;

import java.util.Objects;

public record DamageContext(String attackerId, DamageTarget target, DamageSpec spec) {
    public DamageContext {
        Objects.requireNonNull(attackerId);
        Objects.requireNonNull(target);
        Objects.requireNonNull(spec);
    }

    public DamageContext withSpec(DamageSpec changed) { return new DamageContext(attackerId, target, changed); }
}
