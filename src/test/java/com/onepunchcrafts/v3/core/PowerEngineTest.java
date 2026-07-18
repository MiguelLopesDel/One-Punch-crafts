package com.onepunchcrafts.v3.core;

import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.ability.AbilityContext;
import com.onepunchcrafts.v3.api.effect.EffectSpec;
import com.onepunchcrafts.v3.content.SaitamaContent;
import com.onepunchcrafts.v3.core.ability.AbilityBook;
import com.onepunchcrafts.v3.core.state.PowerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerEngineTest {
    @Test
    void selectionRoutesPrimaryButNeverBecomesStrikeIdentity() {
        V3Registries registries = new V3Registries();
        SaitamaContent.register(registries);
        registries.freeze();
        PowerEngine engine = new PowerEngine(registries);
        PowerState state = new PowerState();
        engine.assign(state, SaitamaContent.POWER_SET);
        engine.selectRelative(state, 1);
        RecordingSink sink = new RecordingSink();

        assertTrue(engine.primaryAttack(state, context(state), sink));
        assertEquals(List.of(SaitamaContent.NORMAL_STRIKE), sink.strikes);
    }

    @Test
    void togglesAreDeclaredLoadoutControlsNotAnonymousAbilities() {
        V3Registries registries = new V3Registries();
        SaitamaContent.register(registries);
        registries.freeze();
        PowerEngine engine = new PowerEngine(registries);
        PowerState state = new PowerState();
        engine.assign(state, SaitamaContent.POWER_SET);

        engine.activate(state, SaitamaContent.SERIOUS_FART, context(state), new RecordingSink());
        assertTrue(state.tags().contains(SaitamaContent.TAG_SERIOUS_FART));
    }

    private static AbilityContext context(PowerState state) {
        return new AbilityContext("actor", 10, Optional.of("target"), List.of(), state.powerSetId(), Map.of());
    }

    private static final class RecordingSink implements PowerEngine.ExecutionSink {
        private final List<Id> strikes = new ArrayList<>();
        @Override public void strike(Id strikeId, String targetId) { strikes.add(strikeId); }
        @Override public void timeline(AbilityBook.Emission emission) {}
        @Override public void effect(EffectSpec.Operation operation) {}
    }
}
