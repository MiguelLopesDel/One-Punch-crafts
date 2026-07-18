package com.onepunchcrafts.v3.core;

import com.onepunchcrafts.v3.api.PowerSetDefinition;
import com.onepunchcrafts.v3.api.Registry;
import com.onepunchcrafts.v3.api.ability.Ability;
import com.onepunchcrafts.v3.api.combat.StrikeDefinition;
import com.onepunchcrafts.v3.api.effect.EffectSpec;

public final class V3Registries {
    public final Registry<PowerSetDefinition> powerSets = new Registry<>("power set");
    public final Registry<Ability> abilities = new Registry<>("ability");
    public final Registry<StrikeDefinition> strikes = new Registry<>("strike");
    public final Registry<EffectSpec> effects = new Registry<>("effect");

    public void freeze() {
        powerSets.freeze();
        abilities.freeze();
        strikes.freeze();
        effects.freeze();
    }
}
