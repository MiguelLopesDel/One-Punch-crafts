package com.onepunchcrafts.v3.api.ability;

import com.onepunchcrafts.v3.api.Id;

import java.util.List;

/** Identity captured at the primary-attack boundary. */
public record AttackPlan(List<Action> actions) {
    public AttackPlan { actions = List.copyOf(actions); }
    public static AttackPlan strike(Id strike) { return new AttackPlan(List.of(new Action.Strike(strike))); }

    public sealed interface Action permits Action.Strike, Action.StartTimeline {
        record Strike(Id strikeId) implements Action {}
        record StartTimeline(Timeline timeline) implements Action {}
    }
}
