package com.onepunchcrafts.api.combat;

import com.onepunchcrafts.api.Id;

public interface DamageObserver {
    Id id();
    DamageStage stage();
    void observe(DamageContext context, DamageApplier.ApplyResult result);
}
