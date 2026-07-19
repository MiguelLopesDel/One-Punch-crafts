package com.onepunchcrafts.minecraft;

import com.onepunchcrafts.api.presentation.VfxProfile;
import com.onepunchcrafts.content.SaitamaContent;
import com.onepunchcrafts.runtime.state.PowerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerStateCodecTest {
    @Test
    void persistsIndependentVfxProfiles() {
        PowerState original = new PowerState();
        original.vfxPreferences().set(SaitamaContent.NORMAL_PUNCH, VfxProfile.ORIGINAL);
        original.vfxPreferences().set(SaitamaContent.DASH, VfxProfile.NEW);

        PowerState decoded = new PowerState();
        PowerStateCodec.decodeInto(PowerStateCodec.encode(original), decoded);

        assertEquals(VfxProfile.ORIGINAL, decoded.vfxPreferences().get(SaitamaContent.NORMAL_PUNCH));
        assertEquals(VfxProfile.NEW, decoded.vfxPreferences().get(SaitamaContent.DASH));
    }
}
