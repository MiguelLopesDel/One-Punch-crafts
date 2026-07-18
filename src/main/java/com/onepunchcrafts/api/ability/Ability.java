package com.onepunchcrafts.api.ability;

import com.onepunchcrafts.api.Id;

import java.util.Optional;

/** Server-only gameplay interface; it contains no client rendering types. */
public interface Ability {
    Id id();
    default Optional<AttackPlan> primaryAttack(AbilityContext context) { return Optional.empty(); }
    Activation activate(AbilityContext context);

    sealed interface Activation permits Activation.Rejected, Activation.Instant, Activation.Scheduled {
        record Rejected(String reason) implements Activation {}
        record Instant() implements Activation {}
        record Scheduled(Timeline timeline) implements Activation {}
    }
}
