package com.onepunchcrafts.v3.core;

import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.ability.Ability;
import com.onepunchcrafts.v3.api.ability.AbilityContext;
import com.onepunchcrafts.v3.api.ability.Timeline;
import com.onepunchcrafts.v3.api.combat.DamageSpec;
import com.onepunchcrafts.v3.api.effect.EffectSpec;
import com.onepunchcrafts.v3.content.ConsecutiveNormalPunches;
import com.onepunchcrafts.v3.content.SaitamaContent;
import com.onepunchcrafts.v3.core.ability.AbilityBook;
import com.onepunchcrafts.v3.core.state.PowerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    @Test
    void consecutiveNormalPunchesAccelerateAcrossTheWholeAoeCone() {
        V3Registries registries = new V3Registries();
        SaitamaContent.register(registries);
        registries.freeze();
        PowerEngine engine = new PowerEngine(registries);
        PowerState state = new PowerState();
        engine.assign(state, SaitamaContent.POWER_SET);
        RecordingSink sink = new RecordingSink();

        Ability.Activation activation = engine.activate(state, SaitamaContent.NORMAL_PUNCH,
                context(state), sink);
        assertInstanceOf(Ability.Activation.Scheduled.class, activation);
        for (long tick = 11; tick <= 110; tick++) engine.tick(state, tick, sink);

        List<Timeline.Command.StrikeCone> waves = sink.emissions.stream()
                .map(emission -> emission.step().command())
                .filter(Timeline.Command.StrikeCone.class::isInstance)
                .map(Timeline.Command.StrikeCone.class::cast)
                .toList();
        List<Integer> waveTicks = sink.emissions.stream()
                .filter(emission -> emission.step().command() instanceof Timeline.Command.StrikeCone)
                .map(emission -> emission.step().tick())
                .toList();

        assertEquals(ConsecutiveNormalPunches.WAVE_TICKS, waveTicks);
        assertTrue(waves.stream().allMatch(wave -> wave.strikeId().equals(SaitamaContent.NORMAL_BARRAGE_STRIKE)));
        assertTrue(waves.stream().allMatch(wave -> wave.range() == ConsecutiveNormalPunches.RANGE
                && wave.halfAngleDegrees() == ConsecutiveNormalPunches.HALF_ANGLE_DEGREES));
        assertTrue(waveTicks.get(1) - waveTicks.get(0)
                > waveTicks.get(waveTicks.size() - 1) - waveTicks.get(waveTicks.size() - 2));

        List<Id> cues = sink.emissions.stream()
                .map(emission -> emission.step().command())
                .filter(Timeline.Command.Cue.class::isInstance)
                .map(Timeline.Command.Cue.class::cast)
                .map(Timeline.Command.Cue::cueId)
                .toList();
        assertTrue(cues.contains(SaitamaContent.CUE_BARRAGE_FINISH));
        assertTrue(cues.contains(SaitamaContent.CUE_BARRAGE_END));

        var damage = registries.strikes.require(SaitamaContent.NORMAL_BARRAGE_STRIKE).damage();
        assertEquals(DamageSpec.IFramePolicy.IGNORE, damage.iFramePolicy());
        assertTrue(damage.tags().contains(SaitamaContent.TAG_BARRAGE_HIT));
    }

    private static AbilityContext context(PowerState state) {
        return new AbilityContext("actor", 10, Optional.of("target"), List.of(), state.powerSetId(), Map.of());
    }

    private static final class RecordingSink implements PowerEngine.ExecutionSink {
        private final List<Id> strikes = new ArrayList<>();
        private final List<AbilityBook.Emission> emissions = new ArrayList<>();
        @Override public void strike(Id strikeId, String targetId) { strikes.add(strikeId); }
        @Override public void timeline(AbilityBook.Emission emission) { emissions.add(emission); }
        @Override public void effect(EffectSpec.Operation operation) {}
    }
}
