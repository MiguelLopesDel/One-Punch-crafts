package com.onepunchcrafts.v3.core.combat;

import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.combat.DamageApplier;
import com.onepunchcrafts.v3.api.combat.DamageContext;
import com.onepunchcrafts.v3.api.combat.DamageSpec;
import com.onepunchcrafts.v3.api.combat.DamageTarget;
import com.onepunchcrafts.v3.api.combat.DamageTier;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamagePipelineTest {
    private static final Id SERIOUS = Id.parse("onepunchcrafts:strike/serious_punch");

    @Test
    void seriousEscalatesWhenNormalApplicationIsDenied() {
        FakeTarget target = new FakeTarget(20);
        DamagePipeline pipeline = new DamagePipeline();
        DamageApplier.ApplyResult result = pipeline.execute(context(target), new FakeApplier(target, true));

        assertEquals(DamageApplier.Method.DIRECT_HEALTH, result.method());
        assertEquals(0, target.health());
    }

    @Test
    void saitamaTargetWinsBeforeUnstoppableVerification() {
        FakeTarget target = new FakeTarget(20);
        target.tags.add(DamagePipeline.SAITAMA_TARGET);
        DamagePipeline pipeline = new DamagePipeline();
        DamageApplier.ApplyResult result = pipeline.execute(context(target), new FakeApplier(target, true));

        assertEquals(DamageApplier.Method.CANON_IMMUNE, result.method());
        assertEquals(20, target.health());
    }

    private static DamageContext context(FakeTarget target) {
        return new DamageContext("attacker", target,
                new DamageSpec(SERIOUS, DamageTier.SERIOUS, 1.0e16, false,
                        DamageSpec.IFramePolicy.IGNORE, Set.of()));
    }

    private static final class FakeTarget implements DamageTarget {
        private final Set<Id> tags = new HashSet<>();
        private double health;
        private FakeTarget(double health) { this.health = health; }
        @Override public String stableId() { return "target"; }
        @Override public double health() { return health; }
        @Override public boolean alive() { return health > 0; }
        @Override public boolean hasTag(Id tag) { return tags.contains(tag); }
    }

    private record FakeApplier(FakeTarget target, boolean denyNormal) implements DamageApplier {
        @Override public ApplyResult apply(DamageContext context) {
            double before = target.health;
            if (!denyNormal) target.health = Math.max(0, before - context.spec().amount());
            return new ApplyResult(before, target.health, target.alive(), denyNormal ? Method.DENIED : Method.NORMAL);
        }
        @Override public ApplyResult force(DamageContext context, ApplyResult deniedResult) {
            target.health = 0;
            return new ApplyResult(deniedResult.healthBefore(), 0, false, Method.DIRECT_HEALTH);
        }
    }
}
