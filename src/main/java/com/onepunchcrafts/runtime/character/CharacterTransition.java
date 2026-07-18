package com.onepunchcrafts.runtime.character;

import java.util.Objects;

/**
 * Defines the atomic character-switch contract without depending on Minecraft.
 * Every transition tears down all previous representations before installing
 * exactly one identity and publishing the resulting state.
 */
public final class CharacterTransition {
    private CharacterTransition() {}

    public static void apply(CharacterIdentity target, Runtime runtime) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(runtime, "runtime");

        runtime.resetProjection();
        runtime.clearLegacyPack();
        runtime.clearPowerSet();

        switch (target) {
            case SAITAMA -> runtime.installSaitama();
            case BOROS -> runtime.installBoros();
            case NONE -> { }
        }

        runtime.sync();
    }

    public interface Runtime {
        void resetProjection();
        void clearLegacyPack();
        void clearPowerSet();
        void installSaitama();
        void installBoros();
        void sync();
    }
}
