package com.onepunchcrafts.v3.core;

import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.PowerSetDefinition;
import com.onepunchcrafts.v3.api.ability.Ability;
import com.onepunchcrafts.v3.api.ability.AbilityContext;
import com.onepunchcrafts.v3.api.ability.AttackPlan;
import com.onepunchcrafts.v3.api.effect.EffectSpec;
import com.onepunchcrafts.v3.core.ability.AbilityBook;
import com.onepunchcrafts.v3.core.state.PowerState;

import java.util.List;
import java.util.Objects;

/** Facade for the pure v3 Module. Environment code talks only to this Interface. */
public final class PowerEngine {
    private final V3Registries registries;

    public PowerEngine(V3Registries registries) { this.registries = Objects.requireNonNull(registries); }

    public void assign(PowerState state, Id powerSetId) {
        PowerSetDefinition definition = registries.powerSets.require(powerSetId);
        state.reset();
        state.powerSetId(powerSetId);
        definition.baseAttributes().forEach(state.attributes()::setBase);
        definition.resources().forEach((id, resource) -> state.resources().define(id,
                resource.initial(), resource.maximum(), resource.regenerationPerTick()));
        definition.identityTags().forEach(state.tags()::add);
        state.abilities().selectedGroup(0);
        state.abilities().select(definition.loadout().get(0).get(0).id());
    }

    public void clear(PowerState state) {
        state.reset();
    }

    public Id selectRelative(PowerState state, int direction) {
        PowerSetDefinition powerSet = registries.powerSets.require(state.powerSetId());
        List<PowerSetDefinition.LoadoutEntry> loadout = powerSet.loadout().get(state.abilities().selectedGroup());
        int current = Math.max(0, loadout.stream().map(PowerSetDefinition.LoadoutEntry::id).toList()
                .indexOf(state.abilities().selectedAbility()));
        int next = Math.floorMod(current + Integer.signum(direction), loadout.size());
        Id selected = loadout.get(next).id();
        state.abilities().select(selected);
        return selected;
    }

    public Id selectGroup(PowerState state, int direction) {
        PowerSetDefinition powerSet = registries.powerSets.require(state.powerSetId());
        int group = Math.floorMod(state.abilities().selectedGroup() + Integer.signum(direction), powerSet.loadout().size());
        state.abilities().selectedGroup(group);
        Id selected = powerSet.loadout().get(group).get(0).id();
        state.abilities().select(selected);
        return selected;
    }

    public boolean primaryAttack(PowerState state, AbilityContext context, ExecutionSink sink) {
        if (state.powerSetId().equals(PowerState.NONE)) return false;
        PowerSetDefinition.LoadoutEntry entry = registries.powerSets.require(state.powerSetId())
                .requireEntry(state.abilities().selectedAbility());
        if (!(entry instanceof PowerSetDefinition.AbilityEntry)) return false;
        Ability ability = registries.abilities.require(entry.id());
        return ability.primaryAttack(context).map(plan -> {
            executePlan(state, context, plan, sink);
            return true;
        }).orElse(false);
    }

    public Ability.Activation activate(PowerState state, Id entryId, AbilityContext context, ExecutionSink sink) {
        PowerSetDefinition powerSet = registries.powerSets.require(state.powerSetId());
        PowerSetDefinition.LoadoutEntry entry;
        try { entry = powerSet.requireEntry(entryId); }
        catch (IllegalArgumentException ignored) { return new Ability.Activation.Rejected("entry_not_owned"); }
        if (entry instanceof PowerSetDefinition.ToggleEntry toggle) {
            if (state.tags().contains(toggle.tag())) state.tags().remove(toggle.tag());
            else state.tags().add(toggle.tag());
            return new Ability.Activation.Instant();
        }
        if (entry instanceof PowerSetDefinition.AdjustableEntry) return new Ability.Activation.Instant();
        Ability.Activation activation = registries.abilities.require(entryId).activate(context);
        if (activation instanceof Ability.Activation.Scheduled scheduled) {
            state.abilities().start(scheduled.timeline(), context.gameTick(), context.primaryTarget().orElse(null),
                    context.capturedTargets(), context.parameters());
            // Tick zero is intentionally emitted now, so input and cue share the same origin tick.
            state.abilities().tick(context.gameTick(), sink::timeline);
        }
        return activation;
    }

    public double adjust(PowerState state, Id entryId, int direction) {
        PowerSetDefinition.LoadoutEntry entry = registries.powerSets.require(state.powerSetId()).requireEntry(entryId);
        if (!(entry instanceof PowerSetDefinition.AdjustableEntry adjustable))
            return Double.NaN;
        double current = state.attributes().base(adjustable.attribute());
        double next = Math.max(adjustable.minimum(), Math.min(adjustable.maximum(),
                current + Integer.signum(direction) * adjustable.step()));
        state.attributes().setBase(adjustable.attribute(), next);
        return next;
    }

    public void tick(PowerState state, long now, ExecutionSink sink) {
        state.tick(now, sink::effect);
        state.abilities().tick(now, sink::timeline);
    }

    private void executePlan(PowerState state, AbilityContext context, AttackPlan plan, ExecutionSink sink) {
        for (AttackPlan.Action action : plan.actions()) {
            if (action instanceof AttackPlan.Action.Strike strike) sink.strike(strike.strikeId(), context.primaryTarget().orElse(null));
            if (action instanceof AttackPlan.Action.StartTimeline start) {
                state.abilities().start(start.timeline(), context.gameTick(), context.primaryTarget().orElse(null),
                        context.capturedTargets(), context.parameters());
                state.abilities().tick(context.gameTick(), sink::timeline);
            }
        }
    }

    public V3Registries registries() { return registries; }

    public interface ExecutionSink {
        void strike(Id strikeId, String targetId);
        void timeline(AbilityBook.Emission emission);
        void effect(EffectSpec.Operation operation);
    }
}
