package com.onepunchcrafts.runtime.ability;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.ability.Ability;
import com.onepunchcrafts.api.ability.AbilityContext;
import com.onepunchcrafts.api.ability.AttackPlan;
import com.onepunchcrafts.api.ability.Timeline;

import java.util.Optional;
import java.util.function.Function;

public final class DeclarativeAbility implements Ability {
    private final Id id;
    private final Function<AbilityContext, Optional<AttackPlan>> primary;
    private final Function<AbilityContext, Activation> activation;

    public DeclarativeAbility(Id id, Function<AbilityContext, Optional<AttackPlan>> primary,
                              Function<AbilityContext, Activation> activation) {
        this.id = id;
        this.primary = primary;
        this.activation = activation;
    }

    public static DeclarativeAbility strikeAndTimeline(Id id, Id strike, Function<AbilityContext, Timeline> timeline) {
        return new DeclarativeAbility(id,
                ignored -> Optional.of(AttackPlan.strike(strike)),
                context -> new Activation.Scheduled(timeline.apply(context)));
    }

    @Override public Id id() { return id; }
    @Override public Optional<AttackPlan> primaryAttack(AbilityContext context) { return primary.apply(context); }
    @Override public Activation activate(AbilityContext context) { return activation.apply(context); }
}
