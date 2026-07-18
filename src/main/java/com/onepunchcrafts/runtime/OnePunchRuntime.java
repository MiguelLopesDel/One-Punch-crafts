package com.onepunchcrafts.runtime;

import com.onepunchcrafts.runtime.combat.DamagePipeline;

/** Process-wide immutable composition root for the power runtime. */
public final class OnePunchRuntime {
    public static final PowerRegistries REGISTRIES = new PowerRegistries();
    public static final PowerEngine POWERS = new PowerEngine(REGISTRIES);
    public static final DamagePipeline DAMAGE = new DamagePipeline();
    private static boolean bootstrapped;

    /** Public addon seam. Mods register during construction, before common setup. */
    public static synchronized void register(java.util.function.Consumer<PowerRegistries> registration) {
        if (bootstrapped) throw new IllegalStateException("Power content registration already closed");
        registration.accept(REGISTRIES);
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        REGISTRIES.freeze();
        bootstrapped = true;
    }

    private OnePunchRuntime() {}
}
