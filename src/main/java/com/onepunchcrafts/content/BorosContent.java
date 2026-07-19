package com.onepunchcrafts.content;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.PowerSetDefinition;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.api.ability.Ability;
import com.onepunchcrafts.api.ability.Timeline;
import com.onepunchcrafts.runtime.PowerRegistries;
import com.onepunchcrafts.runtime.ability.DeclarativeAbility;
import com.onepunchcrafts.runtime.combat.BorosMitigationInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Boros' radial-wheel projection while his mature gameplay remains behind the legacy adapter. */
public final class BorosContent {
    public static final Id POWER_SET = id("powerset/boros");
    public static final Id REGENERATION = id("ability/boros_regeneration");
    public static final Id ENERGY_PROJECTION = id("ability/boros_energy_projection");
    public static final Id FLIGHT = id("ability/boros_flight");
    public static final Id DESTRUCTIVE_MOVEMENT = id("ability/boros_destructive_movement");
    public static final Id DASH = id("ability/boros_dash");
    public static final Id FORM = id("ability/boros_form");
    public static final Id METEORIC_BURST = id("ability/boros_meteoric_burst");
    public static final Id ROARING_CANNON = id("ability/boros_roaring_cannon");
    public static final Id CSRC = id("ability/boros_csrc");
    public static final List<Id> TECHNIQUES = List.of(REGENERATION, ENERGY_PROJECTION, DASH,
            ROARING_CANNON, CSRC, FLIGHT, DESTRUCTIVE_MOVEMENT, FORM, METEORIC_BURST);

    private BorosContent() {}

    public static void register(PowerRegistries registries) {
        for (Id id : TECHNIQUES) registries.abilities.register(id, cueAbility(id));
        registerTechnique(registries, REGENERATION, "technique.boros.regeneration");
        registerTechnique(registries, ENERGY_PROJECTION, "technique.boros.energy_projection");
        registerTechnique(registries, DASH, "technique.boros.dash");
        registerTechnique(registries, ROARING_CANNON, "technique.boros.roaring_cannon");
        registerTechnique(registries, CSRC, "technique.boros.csrc");
        registerTechnique(registries, FLIGHT, "technique.boros.flight");
        registerTechnique(registries, DESTRUCTIVE_MOVEMENT, "technique.boros.destructive_movement");
        registerTechnique(registries, FORM, "technique.boros.form");
        registerTechnique(registries, METEORIC_BURST, "technique.boros.meteoric_burst");

        registries.powerSets.register(POWER_SET, new PowerSetDefinition(POWER_SET, Map.of(), Map.of(),
                Set.of(BorosMitigationInterceptor.BOROS_TARGET), List.of(
                new PowerSetDefinition.TechniquePage("technique.page.boros_combat",
                        List.of(REGENERATION, ENERGY_PROJECTION, DASH, ROARING_CANNON, CSRC)),
                new PowerSetDefinition.TechniquePage("technique.page.boros_forms",
                        List.of(FLIGHT, DESTRUCTIVE_MOVEMENT, FORM, METEORIC_BURST)))));
    }

    private static void registerTechnique(PowerRegistries registries, Id id, String name) {
        registries.techniques.register(id, Technique.cast(id, name, id, "technique.action.activate"));
    }

    private static Ability cueAbility(Id id) {
        return new DeclarativeAbility(id, ignored -> Optional.empty(), ignored ->
                new Ability.Activation.Scheduled(Timeline.builder(
                        new Id(id.namespace(), "timeline/" + id.path().substring("ability/".length())), 1)
                        .cue(0, id).build()));
    }

    private static Id id(String path) { return new Id("onepunchcrafts", path); }
}
