package com.onepunchcrafts.runtime;

import com.onepunchcrafts.api.PowerSetDefinition;
import com.onepunchcrafts.api.Registry;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.api.ability.Ability;
import com.onepunchcrafts.api.combat.StrikeDefinition;
import com.onepunchcrafts.api.effect.EffectSpec;

public final class PowerRegistries {
    public final Registry<PowerSetDefinition> powerSets = new Registry<>("power set");
    public final Registry<Technique> techniques = new Registry<>("technique");
    public final Registry<Ability> abilities = new Registry<>("ability");
    public final Registry<StrikeDefinition> strikes = new Registry<>("strike");
    public final Registry<EffectSpec> effects = new Registry<>("effect");

    public void freeze() {
        for (var powerSetId : powerSets.ids()) {
            PowerSetDefinition powerSet = powerSets.require(powerSetId);
            for (var page : powerSet.techniquePages()) {
                for (var techniqueId : page.techniques()) {
                    Technique technique = techniques.require(techniqueId);
                    technique.primaryAbility().ifPresent(abilities::require);
                    if (technique.activeAction() instanceof Technique.ActiveAction.Cast cast)
                        abilities.require(cast.ability());
                    if (technique.activeAction() instanceof Technique.ActiveAction.Adjust adjust
                            && !powerSet.baseAttributes().containsKey(adjust.attribute()))
                        throw new IllegalArgumentException("Technique " + techniqueId
                                + " adjusts an attribute not owned by " + powerSetId + ": " + adjust.attribute());
                }
            }
        }
        powerSets.freeze();
        techniques.freeze();
        abilities.freeze();
        strikes.freeze();
        effects.freeze();
    }
}
