package com.onepunchcrafts.v3.core.state;

import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.effect.EffectSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EffectContainerTest {
    @Test
    void expiresDirectlyAndEmitsItsOwnConsequence() {
        Id explosion = Id.parse("onepunchcrafts:cue/delayed_explosion");
        EffectSpec spec = new EffectSpec(Id.parse("onepunchcrafts:effect/punched"), 4, 0,
                EffectSpec.StackPolicy.REFRESH, List.of(), List.of(),
                List.of(new EffectSpec.Operation.Cue(explosion)));
        EffectContainer effects = new EffectContainer();
        List<EffectSpec.Operation> emitted = new ArrayList<>();

        effects.apply(spec, 20, emitted::add);
        effects.tick(23, emitted::add);
        effects.tick(24, emitted::add);

        assertEquals(List.of(new EffectSpec.Operation.Cue(explosion)), emitted);
        assertEquals(0, effects.count(spec.id()));
    }
}
