package com.onepunchcrafts.common.skills.boros;

import lombok.Getter;
import lombok.Setter;

public class BorosConfig {
    public static final int MAX_ENERGY = 1_000_000_000;
    public static final float REGEN_RATE_NORMAL = 125_000.0f;
    public static final float REGEN_RATE_HALF = 55_000.0f;
    public static final float REGEN_RATE_LOW = 12_000.0f;
    public static final int EXHAUSTED_COOLDOWN_TICKS = 24_000;

    // Custos de energia
    public static final float ACTIVE_REGEN_COST = 150_000_000f;

    public static final float ENERGY_BLAST_COST = 4_000_000f;

    public static final float CSRC_COST = 500_000_000f;
    public static final float METEORIC_BURST_TICK_COST = 850_000.0f;
    public static final float FLIGHT_COST = 90_000.0f;

    @Getter
    @Setter
    private long exhaustedTimestamp = 0;

    @Getter
    @Setter
    private boolean isExhausted = false;

    public float getRegenerationRate(float currentEnergy) {
        if (isExhausted) return 0f;

        float percentage = (currentEnergy / MAX_ENERGY) * 100f;

        if (percentage <= 10f) {
            return REGEN_RATE_LOW;
        } else if (percentage <= 50f) {
            return REGEN_RATE_HALF;
        }
        return REGEN_RATE_NORMAL;
    }

    public boolean canRecover(long currentTick) {
        if (!isExhausted) return true;
        long referenceTick = exhaustedTimestamp > currentTick + EXHAUSTED_COOLDOWN_TICKS
                ? System.currentTimeMillis() / 50
                : currentTick;
        return (referenceTick - exhaustedTimestamp) >= EXHAUSTED_COOLDOWN_TICKS;
    }
}
