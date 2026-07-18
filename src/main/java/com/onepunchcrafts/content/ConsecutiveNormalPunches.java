package com.onepunchcrafts.content;

import java.util.ArrayList;
import java.util.List;

/** Shared gameplay/presentation profile for Consecutive Normal Punches. */
public final class ConsecutiveNormalPunches {
    public static final int DURATION_TICKS = 100;
    public static final double RANGE = 12.0;
    public static final double HALF_ANGLE_DEGREES = 42.0;
    public static final List<Integer> WAVE_TICKS = buildWaveTicks();

    private ConsecutiveNormalPunches() {}

    public static boolean isWaveTick(int tick) {
        return tick >= 0 && tick < DURATION_TICKS && WAVE_TICKS.contains(tick);
    }

    public static float progress(int tick) {
        return Math.max(0.0f, Math.min(1.0f, tick / (float) DURATION_TICKS));
    }

    private static List<Integer> buildWaveTicks() {
        List<Integer> ticks = new ArrayList<>();
        for (int tick = 0; tick < DURATION_TICKS; ) {
            ticks.add(tick);
            tick += intervalAt(tick);
        }
        return List.copyOf(ticks);
    }

    private static int intervalAt(int tick) {
        return Math.max(1, Math.round(4.0f - 3.0f * progress(tick)));
    }
}
