package com.onepunchcrafts.runtime;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.PowerSetDefinition;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.api.ability.Ability;
import com.onepunchcrafts.api.ability.AbilityContext;
import com.onepunchcrafts.api.ability.AttackPlan;
import com.onepunchcrafts.api.effect.EffectSpec;
import com.onepunchcrafts.runtime.ability.AbilityBook;
import com.onepunchcrafts.runtime.state.PowerState;

import java.util.List;
import java.util.Objects;

/** Facade for the pure power Module. Environment code talks only to this Interface. */
public final class PowerEngine {
    private final PowerRegistries registries;

    public PowerEngine(PowerRegistries registries) { this.registries = Objects.requireNonNull(registries); }

    public void assign(PowerState state, Id powerSetId) {
        PowerSetDefinition definition = registries.powerSets.require(powerSetId);
        state.reset();
        state.powerSetId(powerSetId);
        definition.baseAttributes().forEach(state.attributes()::setBase);
        definition.resources().forEach((id, resource) -> state.resources().define(id,
                resource.initial(), resource.maximum(), resource.regenerationPerTick()));
        definition.identityTags().forEach(state.tags()::add);
        Id initial = definition.techniquePages().get(0).techniques().get(0);
        registries.techniques.require(initial);
        state.abilities().selectedPage(0);
        state.abilities().select(initial);
    }

    public void clear(PowerState state) { state.reset(); }

    public Id select(PowerState state, Id techniqueId) {
        PowerSetDefinition powerSet = powerSet(state);
        powerSet.requireTechnique(techniqueId);
        registries.techniques.require(techniqueId);
        state.abilities().selectedPage(powerSet.pageOf(techniqueId));
        state.abilities().select(techniqueId);
        return techniqueId;
    }

    public Id selectRelative(PowerState state, int direction) {
        PowerSetDefinition powerSet = powerSet(state);
        List<Id> techniques = powerSet.techniquePages().get(state.abilities().selectedPage()).techniques();
        int current = Math.max(0, techniques.indexOf(state.abilities().selectedTechnique()));
        return select(state, techniques.get(Math.floorMod(current + Integer.signum(direction), techniques.size())));
    }

    public Id selectPage(PowerState state, int direction) {
        PowerSetDefinition powerSet = powerSet(state);
        int page = Math.floorMod(state.abilities().selectedPage() + Integer.signum(direction),
                powerSet.techniquePages().size());
        return select(state, powerSet.techniquePages().get(page).techniques().get(0));
    }

    public Id swapPrevious(PowerState state) {
        Id previous = state.abilities().previousTechnique();
        if (previous == null) return state.abilities().selectedTechnique();
        return select(state, previous);
    }

    public boolean primaryAttack(PowerState state, AbilityContext context, ExecutionSink sink) {
        if (state.powerSetId().equals(PowerState.NONE) || state.abilities().selectedTechnique() == null) return false;
        Technique technique = requireOwnedTechnique(state, state.abilities().selectedTechnique());
        if (technique.primaryAbility().isEmpty()) return false;
        Ability ability = registries.abilities.require(technique.primaryAbility().orElseThrow());
        return ability.primaryAttack(context).map(plan -> {
            executePlan(state, context, plan, sink);
            return true;
        }).orElse(false);
    }

    public Ability.Activation activate(PowerState state, Id techniqueId, AbilityContext context, ExecutionSink sink) {
        Technique technique;
        try { technique = requireOwnedTechnique(state, techniqueId); }
        catch (IllegalArgumentException ignored) { return new Ability.Activation.Rejected("technique_not_owned"); }

        if (technique.activeAction() instanceof Technique.ActiveAction.Toggle toggle) {
            if (state.tags().contains(toggle.tag())) state.tags().remove(toggle.tag());
            else state.tags().add(toggle.tag());
            return new Ability.Activation.Instant();
        }
        if (technique.activeAction() instanceof Technique.ActiveAction.Adjust)
            return new Ability.Activation.Rejected("technique_requires_scroll");
        if (technique.activeAction() instanceof Technique.ActiveAction.None)
            return new Ability.Activation.Rejected("technique_has_no_active_action");

        Id abilityId = ((Technique.ActiveAction.Cast) technique.activeAction()).ability();
        Ability.Activation activation = registries.abilities.require(abilityId).activate(context);
        if (activation instanceof Ability.Activation.Scheduled scheduled) {
            state.abilities().start(scheduled.timeline(), context.gameTick(), context.primaryTarget().orElse(null),
                    context.capturedTargets(), context.parameters());
            state.abilities().tick(context.gameTick(), sink::timeline);
        }
        return activation;
    }

    public double adjust(PowerState state, Id techniqueId, int direction) {
        Technique technique = requireOwnedTechnique(state, techniqueId);
        if (!(technique.activeAction() instanceof Technique.ActiveAction.Adjust adjustable)) return Double.NaN;
        double current = state.attributes().base(adjustable.attribute());
        return setAdjustment(state, techniqueId, current + Integer.signum(direction) * adjustable.step());
    }

    /** Applies an absolute slider intent, clamped and snapped to the Technique's declared range. */
    public double setAdjustment(PowerState state, Id techniqueId, double requestedValue) {
        Technique technique = requireOwnedTechnique(state, techniqueId);
        if (!(technique.activeAction() instanceof Technique.ActiveAction.Adjust adjustable)
                || !Double.isFinite(requestedValue)) return Double.NaN;
        double bounded = Math.max(adjustable.minimum(), Math.min(adjustable.maximum(), requestedValue));
        double steps = Math.round((bounded - adjustable.minimum()) / adjustable.step());
        double next = Math.max(adjustable.minimum(), Math.min(adjustable.maximum(),
                adjustable.minimum() + steps * adjustable.step()));
        state.attributes().setBase(adjustable.attribute(), next);
        return next;
    }

    public void tick(PowerState state, long now, ExecutionSink sink) {
        state.tick(now, sink::effect);
        state.abilities().tick(now, sink::timeline);
    }

    private PowerSetDefinition powerSet(PowerState state) {
        return registries.powerSets.require(state.powerSetId());
    }

    private Technique requireOwnedTechnique(PowerState state, Id techniqueId) {
        powerSet(state).requireTechnique(techniqueId);
        return registries.techniques.require(techniqueId);
    }

    private void executePlan(PowerState state, AbilityContext context, AttackPlan plan, ExecutionSink sink) {
        for (AttackPlan.Action action : plan.actions()) {
            if (action instanceof AttackPlan.Action.Strike strike)
                sink.strike(strike.strikeId(), context.primaryTarget().orElse(null));
            if (action instanceof AttackPlan.Action.StartTimeline start) {
                state.abilities().start(start.timeline(), context.gameTick(), context.primaryTarget().orElse(null),
                        context.capturedTargets(), context.parameters());
                state.abilities().tick(context.gameTick(), sink::timeline);
            }
        }
    }

    public PowerRegistries registries() { return registries; }

    public interface ExecutionSink {
        void strike(Id strikeId, String targetId);
        void timeline(AbilityBook.Emission emission);
        void effect(EffectSpec.Operation operation);
    }
}
