package com.onepunchcrafts.common.dimension;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DimensionalPunchPresentationTest {

    @Test
    void everyCastHasAnimationCinematicAndServerVisibleFallback() {
        List<String> beats = new ArrayList<>();
        DimensionalPunchPresentation.Sink sink = new DimensionalPunchPresentation.Sink() {
            @Override public void playPunchAnimation() { beats.add("animation"); }
            @Override public void startCinematic(DimensionalPunchPresentation.Cast cast) { beats.add("cinematic"); }
            @Override public void showServerFallback(DimensionalPunchPresentation.Cast cast) { beats.add("fallback"); }
            @Override public void showImpactFallback(DimensionalPunchPresentation.Cast cast) { beats.add("impact"); }
        };

        var cast = new DimensionalPunchPresentation.Cast(7, Vec3.ZERO, new Vec3(0, 0, 1), 12);
        DimensionalPunchPresentation.begin(sink, cast);
        DimensionalPunchPresentation.impact(sink, cast);

        assertEquals(List.of("animation", "cinematic", "fallback", "impact"), beats);
    }
}
