package com.onepunchcrafts.v3.api.combat;

import com.onepunchcrafts.v3.api.Id;

public interface DamageObserver {
    Id id();
    DamageStage stage();
    void observe(DamageContext context, DamageApplier.ApplyResult result);
}
