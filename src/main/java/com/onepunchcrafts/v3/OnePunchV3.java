package com.onepunchcrafts.v3;

import com.onepunchcrafts.v3.core.PowerEngine;
import com.onepunchcrafts.v3.core.V3Registries;
import com.onepunchcrafts.v3.core.combat.DamagePipeline;

/** Process-wide immutable v3 composition root. */
public final class OnePunchV3 {
    public static final V3Registries REGISTRIES = new V3Registries();
    public static final PowerEngine POWERS = new PowerEngine(REGISTRIES);
    public static final DamagePipeline DAMAGE = new DamagePipeline();
    private static boolean bootstrapped;

    /** Public addon seam. Mods register during construction, before common setup. */
    public static synchronized void register(java.util.function.Consumer<V3Registries> registration) {
        if (bootstrapped) throw new IllegalStateException("v3 content registration already closed");
        registration.accept(REGISTRIES);
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        REGISTRIES.freeze();
        bootstrapped = true;
    }

    private OnePunchV3() {}
}
