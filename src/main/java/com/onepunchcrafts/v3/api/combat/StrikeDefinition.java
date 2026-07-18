package com.onepunchcrafts.v3.api.combat;

import com.onepunchcrafts.v3.api.Id;

public record StrikeDefinition(Id id, DamageSpec damage) {
    public StrikeDefinition {
        if (!id.equals(damage.strikeId())) throw new IllegalArgumentException("Strike and DamageSpec ids differ");
    }
}
