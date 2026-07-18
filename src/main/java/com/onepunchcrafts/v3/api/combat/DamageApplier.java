package com.onepunchcrafts.v3.api.combat;

/** The only Seam through which the core mutates a target. */
public interface DamageApplier {
    ApplyResult apply(DamageContext context);
    ApplyResult force(DamageContext context, ApplyResult deniedResult);

    record ApplyResult(double healthBefore, double healthAfter, boolean alive, Method method) {
        public double healthLost() { return Math.max(0, healthBefore - healthAfter); }
        public boolean hadEffect() { return healthLost() > 0 || !alive; }
    }

    enum Method { NORMAL, FALLBACK_DAMAGE, DIRECT_HEALTH, CANON_IMMUNE, DENIED }
}
