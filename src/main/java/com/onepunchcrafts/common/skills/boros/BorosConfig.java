package com.onepunchcrafts.common.skills.boros;

import lombok.Getter;
import lombok.Setter;

public class BorosConfig {
    public static final int MAX_ENERGY = 1_000_000_000;
    // Boros' energy visibly overflows (his armour exists to restrain it), so it
    // recovers fast; the low tier no longer traps him near empty.
    public static final float REGEN_RATE_NORMAL = 250_000.0f;
    public static final float REGEN_RATE_HALF = 150_000.0f;
    public static final float REGEN_RATE_LOW = 60_000.0f;
    public static final int EXHAUSTED_COOLDOWN_TICKS = 6_000; // 5 minutes

    // Custos de energia
    public static final float ACTIVE_REGEN_COST = 150_000_000f;

    public static final float ENERGY_BLAST_COST = 4_000_000f;
    public static final float ROARING_CANNON_COST = 160_000_000f;

    // The CSRC is the "all my remaining energy" finisher: it spends whatever
    // Boros has left (never a fixed slice), needing at least this much to be
    // worth firing, and its power scales with the energy actually poured in.
    public static final float CSRC_MIN_ENERGY = 300_000_000f;
    public static final float METEORIC_BURST_TICK_COST = 100_000.0f;
    public static final float FLIGHT_COST = 26_000.0f;

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
