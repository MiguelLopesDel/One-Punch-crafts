package com.onepunchcrafts.v3.api.combat;

import com.onepunchcrafts.v3.api.Id;

import java.util.Objects;
import java.util.Set;

/** Immutable identity and policy of one atomic hit. */
public record DamageSpec(
        Id strikeId,
        DamageTier tier,
        double amount,
        boolean unstoppable,
        IFramePolicy iFramePolicy,
        Set<Id> tags
) {
    public DamageSpec {
        Objects.requireNonNull(strikeId);
        Objects.requireNonNull(tier);
        Objects.requireNonNull(iFramePolicy);
        tags = Set.copyOf(tags);
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid damage " + amount);
        if (tier == DamageTier.SERIOUS) unstoppable = true;
    }

    public enum IFramePolicy { RESPECT, IGNORE }

    public DamageSpec withAmount(double newAmount) {
        return new DamageSpec(strikeId, tier, newAmount, unstoppable, iFramePolicy, tags);
    }
}
