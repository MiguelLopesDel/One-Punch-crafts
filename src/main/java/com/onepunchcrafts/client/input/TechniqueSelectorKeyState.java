package com.onepunchcrafts.client.input;

/** Resolves the hold state while the radial selector owns the current screen. */
public final class TechniqueSelectorKeyState {
    private TechniqueSelectorKeyState() {}

    public static boolean resolve(boolean mappingDown, boolean wheelOwnsInput, boolean physicalDown) {
        return wheelOwnsInput ? physicalDown : mappingDown;
    }
}
