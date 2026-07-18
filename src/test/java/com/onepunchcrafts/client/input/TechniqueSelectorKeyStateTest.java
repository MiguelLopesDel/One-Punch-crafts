package com.onepunchcrafts.client.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TechniqueSelectorKeyStateTest {
    @Test
    void wheelKeepsHoldFromPhysicalKeyWhenInGameMappingBecomesInactive() {
        boolean mappingDown = false; // IN_GAME is inactive while a Screen is open.
        boolean wheelOwnsInput = true;
        boolean physicalDown = true;

        assertTrue(TechniqueSelectorKeyState.resolve(mappingDown, wheelOwnsInput, physicalDown));
    }
}
