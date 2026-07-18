package com.onepunchcrafts.runtime.character;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CharacterTransitionTest {
    @Test
    void borosToSaitamaClearsEveryPreviousIdentityBeforeInstallingSaitama() {
        RecordingRuntime runtime = RecordingRuntime.boros();

        CharacterTransition.apply(CharacterIdentity.SAITAMA, runtime);

        assertFalse(runtime.boros);
        assertTrue(runtime.saitama);
        assertEquals(List.of("resetProjection", "clearLegacyPack", "clearPowerSet",
                "installSaitama", "sync"), runtime.operations);
    }

    @Test
    void saitamaToBorosClearsEveryPreviousIdentityBeforeInstallingBoros() {
        RecordingRuntime runtime = RecordingRuntime.saitama();

        CharacterTransition.apply(CharacterIdentity.BOROS, runtime);

        assertTrue(runtime.boros);
        assertFalse(runtime.saitama);
        assertEquals(List.of("resetProjection", "clearLegacyPack", "clearPowerSet",
                "installBoros", "sync"), runtime.operations);
    }

    @Test
    void removingACharacterLeavesNeitherIdentityInstalled() {
        RecordingRuntime runtime = RecordingRuntime.overlapping();

        CharacterTransition.apply(CharacterIdentity.NONE, runtime);

        assertFalse(runtime.boros);
        assertFalse(runtime.saitama);
        assertEquals(List.of("resetProjection", "clearLegacyPack", "clearPowerSet", "sync"),
                runtime.operations);
    }

    /** Allows the core contract to run without resolving the Minecraft dependency graph. */
    public static void main(String[] args) {
        CharacterTransitionTest test = new CharacterTransitionTest();
        test.borosToSaitamaClearsEveryPreviousIdentityBeforeInstallingSaitama();
        test.saitamaToBorosClearsEveryPreviousIdentityBeforeInstallingBoros();
        test.removingACharacterLeavesNeitherIdentityInstalled();
    }

    private static final class RecordingRuntime implements CharacterTransition.Runtime {
        private final List<String> operations = new ArrayList<>();
        private boolean boros;
        private boolean saitama;

        private RecordingRuntime(boolean boros, boolean saitama) {
            this.boros = boros;
            this.saitama = saitama;
        }

        static RecordingRuntime boros() { return new RecordingRuntime(true, false); }
        static RecordingRuntime saitama() { return new RecordingRuntime(false, true); }
        static RecordingRuntime overlapping() { return new RecordingRuntime(true, true); }

        @Override public void resetProjection() { operations.add("resetProjection"); }
        @Override public void clearLegacyPack() { operations.add("clearLegacyPack"); boros = false; }
        @Override public void clearPowerSet() { operations.add("clearPowerSet"); saitama = false; }
        @Override public void installSaitama() { operations.add("installSaitama"); saitama = true; }
        @Override public void installBoros() { operations.add("installBoros"); boros = true; }
        @Override public void sync() { operations.add("sync"); }
    }
}
