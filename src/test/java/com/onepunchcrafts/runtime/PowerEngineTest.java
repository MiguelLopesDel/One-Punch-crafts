package com.onepunchcrafts.runtime;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.api.ability.Ability;
import com.onepunchcrafts.api.ability.AbilityContext;
import com.onepunchcrafts.api.ability.Timeline;
import com.onepunchcrafts.api.combat.DamageSpec;
import com.onepunchcrafts.api.effect.EffectSpec;
import com.onepunchcrafts.content.ConsecutiveNormalPunches;
import com.onepunchcrafts.content.SaitamaContent;
import com.onepunchcrafts.runtime.ability.AbilityBook;
import com.onepunchcrafts.runtime.state.PowerState;
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
        PowerRegistries registries = new PowerRegistries();
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
    void togglesAreDeclaredTechniquesNotAnonymousAbilities() {
        PowerRegistries registries = new PowerRegistries();
        SaitamaContent.register(registries);
        registries.freeze();
        PowerEngine engine = new PowerEngine(registries);
        PowerState state = new PowerState();
        engine.assign(state, SaitamaContent.POWER_SET);

        engine.activate(state, SaitamaContent.SERIOUS_FART, context(state), new RecordingSink());
        assertTrue(state.tags().contains(SaitamaContent.TAG_SERIOUS_FART));
    }

    @Test
    void techniqueKeepsPrimaryAndActiveInputsDistinct() {
        PowerRegistries registries = new PowerRegistries();
        SaitamaContent.register(registries);

        Technique normal = registries.techniques.require(SaitamaContent.NORMAL_PUNCH);

        assertEquals(Optional.of(SaitamaContent.NORMAL_PUNCH), normal.primaryAbility());
        assertEquals(new Technique.ActiveAction.Cast(SaitamaContent.NORMAL_PUNCH), normal.activeAction());
        assertEquals("technique.saitama.normal_punch.primary",
                normal.presentation().primaryKey().orElseThrow());
        assertEquals("technique.saitama.normal_punch.active",
                normal.presentation().activeKey().orElseThrow());
    }

    @Test
    void quickSwapAlternatesTheTwoMostRecentTechniques() {
        PowerRegistries registries = new PowerRegistries();
        SaitamaContent.register(registries);
        registries.freeze();
        PowerEngine engine = new PowerEngine(registries);
        PowerState state = new PowerState();
        engine.assign(state, SaitamaContent.POWER_SET);
        engine.select(state, SaitamaContent.NORMAL_PUNCH);
        engine.select(state, SaitamaContent.SERIOUS_PUNCH);

        assertEquals(SaitamaContent.NORMAL_PUNCH, engine.swapPrevious(state));
        assertEquals(SaitamaContent.SERIOUS_PUNCH, state.abilities().previousTechnique());
        assertEquals(SaitamaContent.SERIOUS_PUNCH, engine.swapPrevious(state));
    }

    @Test
    void powerSetExposesWheelSizedTechniquePages() {
        PowerRegistries registries = new PowerRegistries();
        SaitamaContent.register(registries);

        var pages = registries.powerSets.require(SaitamaContent.POWER_SET).techniquePages();

        assertEquals(2, pages.size());
        assertTrue(pages.stream().allMatch(page -> page.techniques().size() <= 8));
        assertEquals(List.of(SaitamaContent.WEAK_PUNCH, SaitamaContent.NORMAL_PUNCH,
                SaitamaContent.SERIOUS_PUNCH), pages.get(0).techniques().subList(0, 3));
    }

    @Test
    void absoluteWheelAdjustmentIsClampedAndSnappedByTheDomain() {
        PowerRegistries registries = new PowerRegistries();
        SaitamaContent.register(registries);
        registries.freeze();
        PowerEngine engine = new PowerEngine(registries);
        PowerState state = new PowerState();
        engine.assign(state, SaitamaContent.POWER_SET);

        assertEquals(237, engine.setAdjustment(state, SaitamaContent.SPEED, 237.4));
        assertEquals(500, engine.setAdjustment(state, SaitamaContent.SPEED, 900));
        assertEquals(0, engine.setAdjustment(state, SaitamaContent.SPEED, -20));
    }

    @Test
    void consecutiveNormalPunchesAccelerateAcrossTheWholeAoeCone() {
        PowerRegistries registries = new PowerRegistries();
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
