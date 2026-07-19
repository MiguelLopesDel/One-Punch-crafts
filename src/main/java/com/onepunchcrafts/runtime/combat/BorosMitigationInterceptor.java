package com.onepunchcrafts.runtime.combat;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.combat.DamageContext;
import com.onepunchcrafts.api.combat.DamageInterceptor;
import com.onepunchcrafts.api.combat.DamageStage;
import com.onepunchcrafts.api.combat.DamageTier;

/** Absolute Boros balance values; AttributeFix permits the conceptual health scale in Minecraft. */
public final class BorosMitigationInterceptor implements DamageInterceptor {
    public static final Id BOROS_TARGET = Id.parse("onepunchcrafts:identity/boros");
    public static final Id RELEASED = Id.parse("onepunchcrafts:form/boros_released");
    public static final Id METEORIC = Id.parse("onepunchcrafts:form/boros_meteoric_burst");
    public static final double MAX_HEALTH = 150_000_000.0;
    public static final double MAX_DAMAGE_PER_HIT = MAX_HEALTH * 0.08;
    private static final Id ID = Id.parse("onepunchcrafts:interceptor/boros_mitigation");

    @Override public Id id() { return ID; }
    @Override public DamageStage stage() { return DamageStage.INCOMING; }
    @Override public int order() { return 100; }

    @Override
    public DamageContext intercept(DamageContext context) {
        if (!context.target().hasTag(BOROS_TARGET) || context.spec().tier() == DamageTier.SERIOUS)
            return context;
        double taken = context.target().hasTag(METEORIC) ? 0.65
                : context.target().hasTag(RELEASED) ? 0.75 : 0.55;
        return context.withSpec(context.spec().withAmount(
                Math.min(context.spec().amount() * taken, MAX_DAMAGE_PER_HIT)));
    }
}
