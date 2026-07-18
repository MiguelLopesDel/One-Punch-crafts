package com.onepunchcrafts.v3.api.combat;

import com.onepunchcrafts.v3.api.Id;

/** Ordered extension point replacing Forge priority races. */
public interface DamageInterceptor {
    Id id();
    DamageStage stage();
    int order();
    DamageContext intercept(DamageContext context);
}
