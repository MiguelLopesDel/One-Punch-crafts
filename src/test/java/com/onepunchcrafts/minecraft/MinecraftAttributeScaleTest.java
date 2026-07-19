package com.onepunchcrafts.minecraft;

import com.onepunchcrafts.runtime.combat.BorosMitigationInterceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftAttributeScaleTest {

    @Test
    void raisesOldAttributeFixCeilingToBorosDomainScale() {
        assertEquals(BorosMitigationInterceptor.MAX_HEALTH,
                MinecraftAttributeScale.requiredMaximum(1_000_000,
                        BorosMitigationInterceptor.MAX_HEALTH));
    }

    @Test
    void preservesAConfiguredCeilingThatIsAlreadyHigher() {
        assertEquals(300_000_000,
                MinecraftAttributeScale.requiredMaximum(300_000_000,
                        BorosMitigationInterceptor.MAX_HEALTH));
    }
}
