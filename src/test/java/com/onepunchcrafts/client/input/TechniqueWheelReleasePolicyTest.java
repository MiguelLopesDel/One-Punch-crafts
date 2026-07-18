package com.onepunchcrafts.client.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TechniqueWheelReleasePolicyTest {
    @Test
    void releaseKeepsWheelOpenWhenTouchpadCouldNotMoveWhileKeyWasHeld() {
        assertEquals(TechniqueWheelReleasePolicy.Action.KEEP_OPEN,
                TechniqueWheelReleasePolicy.resolve(false, false));
    }

    @Test
    void releaseStillConfirmsFastMouseSelection() {
        assertEquals(TechniqueWheelReleasePolicy.Action.CONFIRM,
                TechniqueWheelReleasePolicy.resolve(true, true));
    }
}
