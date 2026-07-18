package com.onepunchcrafts.v3.content;

import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.PowerSetDefinition;
import com.onepunchcrafts.v3.api.ability.Ability;
import com.onepunchcrafts.v3.api.ability.AttackPlan;
import com.onepunchcrafts.v3.api.ability.Timeline;
import com.onepunchcrafts.v3.api.combat.DamageSpec;
import com.onepunchcrafts.v3.api.combat.DamageTier;
import com.onepunchcrafts.v3.api.combat.StrikeDefinition;
import com.onepunchcrafts.v3.api.effect.EffectSpec;
import com.onepunchcrafts.v3.core.V3Registries;
import com.onepunchcrafts.v3.core.ability.DeclarativeAbility;
import com.onepunchcrafts.v3.core.combat.DamagePipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SaitamaContent {
    public static final Id POWER_SET = id("powerset/saitama");
    public static final Id WEAK_PUNCH = id("ability/weak_punch");
    public static final Id NORMAL_PUNCH = id("ability/normal_punch");
    public static final Id SERIOUS_PUNCH = id("ability/serious_punch");
    public static final Id WEAKENING_PUNCH = id("ability/weakening_punch");
    public static final Id NORMAL_PUNCHES_IN_AREA = id("ability/normal_punches_in_area");
    public static final Id QUICK_BACKSTAB = id("ability/quick_backstab");
    public static final Id DASH = id("ability/dash");
    public static final Id SERIOUS_FART = id("control/serious_fart");
    public static final Id SPEED = id("control/speed");
    public static final Id BREAK_BLOCKS = id("control/break_blocks_quickly");
    public static final Id WEIGHT = id("control/weight");
    public static final Id KNOCKBACK_RESISTANCE = id("control/knockback_resistance");
    public static final Id ATTACK_KNOCKBACK = id("control/attack_knockback");
    public static final Id SWIM_SPEED = id("control/swim_speed");
    public static final Id EXTREME_SPEED = id("control/extreme_speed");
    public static final Id EXTREME_JUMP = id("control/extreme_jump");

    public static final Id TAG_SERIOUS_FART = id("state/serious_fart");
    public static final Id TAG_BREAK_BLOCKS = id("state/break_blocks_quickly");
    public static final Id TAG_EXTREME_SPEED = id("state/extreme_speed");
    public static final Id TAG_EXTREME_JUMP = id("state/extreme_jump");
    public static final Id ATTR_SPEED = id("attribute/speed_setting");
    public static final Id ATTR_WEIGHT = id("attribute/weight_setting");
    public static final Id ATTR_KNOCKBACK_RESISTANCE = id("attribute/knockback_resistance_setting");
    public static final Id ATTR_ATTACK_KNOCKBACK = id("attribute/attack_knockback_setting");
    public static final Id ATTR_SWIM_SPEED = id("attribute/swim_speed_setting");

    public static final Id WEAK_STRIKE = id("strike/weak_punch");
    public static final Id NORMAL_STRIKE = id("strike/normal_punch");
    public static final Id NORMAL_BARRAGE_STRIKE = id("strike/normal_barrage");
    public static final Id SERIOUS_STRIKE = id("strike/serious_punch");
    public static final Id WEAKENING_STRIKE = id("strike/weakening_punch");

    /** Marks a hit as part of a barrage: no per-hit knockback or explosion. */
    public static final Id TAG_BARRAGE_HIT = id("damage/barrage_hit");

    public static final Id CUE_BARRAGE = id("cue/saitama_barrage");
    public static final Id CUE_WEAK_BARRAGE = id("cue/saitama_weak_barrage");
    public static final Id CUE_BARRAGE_HIT = id("cue/saitama_barrage_hit");
    public static final Id CUE_BARRAGE_FINISH = id("cue/saitama_barrage_finish");
    public static final Id CUE_BARRAGE_END = id("cue/saitama_barrage_end");
    public static final Id CUE_SERIOUS_WINDUP = id("cue/serious_windup");
    public static final Id CUE_DEBRIS_PULL = id("cue/debris_pull");
    public static final Id CUE_SERIOUS_IMPACT = id("cue/serious_impact");
    public static final Id CUE_SERIOUS_AFTERMATH = id("cue/serious_aftermath");
    public static final Id CUE_DELAYED_EXPLOSION = id("cue/delayed_explosion");
    public static final Id EFFECT_PUNCHED = id("effect/saitama_punched");

    private SaitamaContent() {}

    public static void register(V3Registries registries) {
        registerStrikes(registries);
        registries.effects.register(EFFECT_PUNCHED, new EffectSpec(EFFECT_PUNCHED, 4, 0,
                EffectSpec.StackPolicy.REFRESH, List.of(), List.of(),
                List.of(new EffectSpec.Operation.Cue(CUE_DELAYED_EXPLOSION))));

        registries.abilities.register(WEAK_PUNCH, consecutiveWeakPunches());
        registries.abilities.register(NORMAL_PUNCH, consecutiveNormalPunches());
        registries.abilities.register(WEAKENING_PUNCH, new DeclarativeAbility(WEAKENING_PUNCH,
                ignored -> Optional.of(AttackPlan.strike(WEAKENING_STRIKE)), ignored -> new Ability.Activation.Instant()));
        registries.abilities.register(SERIOUS_PUNCH, seriousAbility());
        registries.abilities.register(NORMAL_PUNCHES_IN_AREA, normalPunchesInArea());
        registries.abilities.register(QUICK_BACKSTAB, new DeclarativeAbility(QUICK_BACKSTAB,
                ignored -> Optional.empty(), context -> new Ability.Activation.Scheduled(
                        Timeline.builder(id("timeline/quick_backstab"), 1)
                                .at(0, new Timeline.Command.TeleportToTarget(Timeline.TargetRef.PRIMARY))
                                .at(0, new Timeline.Command.StrikeTarget(NORMAL_STRIKE, Timeline.TargetRef.PRIMARY))
                                .build())));
        registries.abilities.register(DASH, new DeclarativeAbility(DASH, ignored -> Optional.empty(),
                ignored -> new Ability.Activation.Scheduled(Timeline.builder(id("timeline/dash"), 1)
                        .at(0, new Timeline.Command.Dash(30)).build())));

        registries.powerSets.register(POWER_SET, new PowerSetDefinition(POWER_SET, Map.of(
                ATTR_SPEED, 0.0, ATTR_WEIGHT, 0.0, ATTR_KNOCKBACK_RESISTANCE, 0.0,
                ATTR_ATTACK_KNOCKBACK, 0.0, ATTR_SWIM_SPEED, 0.0), Map.of(),
                Set.of(DamagePipeline.SAITAMA_TARGET),
                List.of(List.of(
                        new PowerSetDefinition.AbilityEntry(WEAK_PUNCH),
                        new PowerSetDefinition.AbilityEntry(NORMAL_PUNCH),
                        new PowerSetDefinition.AbilityEntry(SERIOUS_PUNCH),
                        new PowerSetDefinition.ToggleEntry(SERIOUS_FART, TAG_SERIOUS_FART),
                        new PowerSetDefinition.AbilityEntry(WEAKENING_PUNCH),
                        new PowerSetDefinition.AbilityEntry(QUICK_BACKSTAB),
                        new PowerSetDefinition.AdjustableEntry(SPEED, ATTR_SPEED, 0, 500, 1),
                        new PowerSetDefinition.ToggleEntry(BREAK_BLOCKS, TAG_BREAK_BLOCKS),
                        new PowerSetDefinition.AdjustableEntry(WEIGHT, ATTR_WEIGHT, 0, 500, 1),
                        new PowerSetDefinition.AdjustableEntry(KNOCKBACK_RESISTANCE, ATTR_KNOCKBACK_RESISTANCE, 0, 500, 1),
                        new PowerSetDefinition.AdjustableEntry(ATTACK_KNOCKBACK, ATTR_ATTACK_KNOCKBACK, 0, 500, 1),
                        new PowerSetDefinition.AdjustableEntry(SWIM_SPEED, ATTR_SWIM_SPEED, 0, 500, 1),
                        new PowerSetDefinition.AbilityEntry(NORMAL_PUNCHES_IN_AREA),
                        new PowerSetDefinition.AbilityEntry(DASH)), List.of(
                        new PowerSetDefinition.ToggleEntry(EXTREME_SPEED, TAG_EXTREME_SPEED),
                        new PowerSetDefinition.ToggleEntry(EXTREME_JUMP, TAG_EXTREME_JUMP)))));
    }

    private static void registerStrikes(V3Registries registries) {
        registries.strikes.register(WEAK_STRIKE, strike(WEAK_STRIKE, DamageTier.ENHANCED, 100_000, false));
        registries.strikes.register(NORMAL_STRIKE, strike(NORMAL_STRIKE, DamageTier.DRAGON, 10_000_000, false));
        registries.strikes.register(WEAKENING_STRIKE, strike(WEAKENING_STRIKE, DamageTier.ENHANCED, 100, false));
        registries.strikes.register(SERIOUS_STRIKE, strike(SERIOUS_STRIKE, DamageTier.SERIOUS, 1.0e16, true));
        // Barrage hit: same power as a normal punch, but it ignores i-frames so
        // every wave connects, and is tagged so the sink skips per-hit knockback
        // and explosion — the victim is hammered in place, not flung away.
        registries.strikes.register(NORMAL_BARRAGE_STRIKE, new StrikeDefinition(NORMAL_BARRAGE_STRIKE,
                new DamageSpec(NORMAL_BARRAGE_STRIKE, DamageTier.DRAGON, 10_000_000, false,
                        DamageSpec.IFramePolicy.IGNORE, Set.of(TAG_BARRAGE_HIT))));
    }

    private static StrikeDefinition strike(Id id, DamageTier tier, double amount, boolean unstoppable) {
        return new StrikeDefinition(id, new DamageSpec(id, tier, amount, unstoppable,
                tier == DamageTier.SERIOUS ? DamageSpec.IFramePolicy.IGNORE : DamageSpec.IFramePolicy.RESPECT,
                Set.of()));
    }

    /**
     * Consecutive Normal Punches (連続普通のパンチ): left-click still throws one
     * explosive Normal Punch; activating the skill unleashes the ~5-second
     * barrage. The cadence accelerates into a frantic crescendo, every wave
     * sweeps a wide cone (decent AoE, as in the anime opening), and the final
     * beat is a launching finisher.
     */
    private static Ability consecutiveNormalPunches() {
        return DeclarativeAbility.strikeAndTimeline(NORMAL_PUNCH, NORMAL_STRIKE, context -> {
            Timeline.Builder timeline = Timeline.builder(id("timeline/consecutive_normal_punch"),
                            ConsecutiveNormalPunches.DURATION_TICKS)
                    .cue(0, CUE_BARRAGE);
            for (int tick : ConsecutiveNormalPunches.WAVE_TICKS) {
                timeline.at(tick, new Timeline.Command.StrikeCone(NORMAL_BARRAGE_STRIKE,
                        ConsecutiveNormalPunches.RANGE, ConsecutiveNormalPunches.HALF_ANGLE_DEGREES));
                timeline.cue(tick, CUE_BARRAGE_HIT);
            }
            timeline.cue(ConsecutiveNormalPunches.DURATION_TICKS, CUE_BARRAGE_FINISH);
            timeline.cue(ConsecutiveNormalPunches.DURATION_TICKS, CUE_BARRAGE_END);
            return timeline.build();
        });
    }

    private static Ability consecutiveWeakPunches() {
        return DeclarativeAbility.strikeAndTimeline(WEAK_PUNCH, WEAK_STRIKE, ignored -> {
            Timeline.Builder timeline = Timeline.builder(id("timeline/consecutive_weak_punch"), 100)
                    .cue(0, CUE_WEAK_BARRAGE).cue(100, CUE_BARRAGE_END);
            for (int tick = 0; tick < 100; tick++)
                timeline.at(tick, new Timeline.Command.StrikeArea(WEAK_STRIKE, 5, 3));
            return timeline.build();
        });
    }

    private static Ability seriousAbility() {
        return new DeclarativeAbility(SERIOUS_PUNCH, context -> Optional.of(new AttackPlan(List.of(
                new AttackPlan.Action.Strike(SERIOUS_STRIKE),
                new AttackPlan.Action.StartTimeline(seriousTimeline())))),
                ignored -> new Ability.Activation.Scheduled(seriousTimeline()));
    }

    private static Timeline seriousTimeline() {
        return Timeline.builder(id("timeline/serious_punch"), 96)
                .cue(0, CUE_SERIOUS_WINDUP)
                .cue(10, CUE_DEBRIS_PULL)
                .at(14, new Timeline.Command.StrikeCylinder(SERIOUS_STRIKE, 15, 1_000))
                .at(14, new Timeline.Command.DestroyCylinder(15, 1_000))
                .cue(14, CUE_SERIOUS_IMPACT)
                .cue(30, CUE_SERIOUS_AFTERMATH)
                .build();
    }

    private static Ability normalPunchesInArea() {
        return new DeclarativeAbility(NORMAL_PUNCHES_IN_AREA, ignored -> Optional.empty(), context -> {
            Timeline.Builder timeline = Timeline.builder(id("timeline/normal_punches_in_area"),
                    Math.max(1, context.capturedTargets().size() * 5));
            for (int index = 0; index < context.capturedTargets().size(); index++) {
                int tick = index * 5;
                // The target list is captured by the cast; the adapter advances NEXT_CAPTURED deterministically.
                timeline.at(tick, new Timeline.Command.TeleportToTarget(Timeline.TargetRef.NEXT_CAPTURED));
                timeline.at(tick, new Timeline.Command.StrikeTarget(NORMAL_STRIKE, Timeline.TargetRef.NEXT_CAPTURED));
            }
            return new Ability.Activation.Scheduled(timeline.build());
        });
    }

    private static Id id(String path) { return new Id("onepunchcrafts", path); }
}
