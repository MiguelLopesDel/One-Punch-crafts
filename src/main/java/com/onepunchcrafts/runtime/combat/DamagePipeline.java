package com.onepunchcrafts.runtime.combat;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.combat.DamageApplier;
import com.onepunchcrafts.api.combat.DamageContext;
import com.onepunchcrafts.api.combat.DamageInterceptor;
import com.onepunchcrafts.api.combat.DamageObserver;
import com.onepunchcrafts.api.combat.DamageStage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Owns combat ordering. Forge events are not part of this Module. */
public final class DamagePipeline {
    public static final Id SAITAMA_TARGET = Id.parse("onepunchcrafts:identity/saitama");
    private static final List<DamageStage> PRE_APPLY = List.of(
            DamageStage.OUTGOING, DamageStage.INTERCEPT, DamageStage.INCOMING, DamageStage.CLAMP);

    private final Map<DamageStage, List<DamageInterceptor>> interceptors = new EnumMap<>(DamageStage.class);
    private final Map<DamageStage, List<DamageObserver>> observers = new EnumMap<>(DamageStage.class);

    public DamagePipeline() {
        register(new BorosMitigationInterceptor());
    }

    public void register(DamageInterceptor interceptor) {
        if (interceptor.stage() == DamageStage.APPLY || interceptor.stage() == DamageStage.POST_APPLY
                || interceptor.stage() == DamageStage.VERIFY || interceptor.stage() == DamageStage.TARGET_POLICY)
            throw new IllegalArgumentException("Stage is owned by the pipeline: " + interceptor.stage());
        List<DamageInterceptor> stage = interceptors.computeIfAbsent(interceptor.stage(), ignored -> new ArrayList<>());
        if (stage.stream().anyMatch(existing -> existing.id().equals(interceptor.id())))
            throw new IllegalArgumentException("Duplicate interceptor " + interceptor.id());
        stage.add(interceptor);
        stage.sort(Comparator.comparingInt(DamageInterceptor::order).thenComparing(DamageInterceptor::id));
    }

    public void observe(DamageObserver observer) {
        observers.computeIfAbsent(observer.stage(), ignored -> new ArrayList<>()).add(observer);
    }

    public DamageApplier.ApplyResult execute(DamageContext original, DamageApplier applier) {
        if (original.target().hasTag(SAITAMA_TARGET)) {
            DamageApplier.ApplyResult immune = new DamageApplier.ApplyResult(
                    original.target().health(), original.target().health(), original.target().alive(),
                    DamageApplier.Method.CANON_IMMUNE);
            notify(DamageStage.TARGET_POLICY, original, immune);
            return immune;
        }

        DamageContext current = original;
        for (DamageStage stage : PRE_APPLY) {
            for (DamageInterceptor interceptor : interceptors.getOrDefault(stage, List.of()))
                current = interceptor.intercept(current);
        }

        DamageApplier.ApplyResult result = applier.apply(current);
        notify(DamageStage.POST_APPLY, current, result);
        boolean outcomeDenied = current.spec().tier() == com.onepunchcrafts.api.combat.DamageTier.SERIOUS
                ? result.alive() : !result.hadEffect();
        if (current.spec().unstoppable() && outcomeDenied)
            result = applier.force(current, result);
        notify(DamageStage.VERIFY, current, result);
        return result;
    }

    private void notify(DamageStage stage, DamageContext context, DamageApplier.ApplyResult result) {
        for (DamageObserver observer : observers.getOrDefault(stage, List.of())) observer.observe(context, result);
    }
}
