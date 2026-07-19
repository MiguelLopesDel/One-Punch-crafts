package com.onepunchcrafts.content;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.PowerSetDefinition;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.api.ability.Ability;
import com.onepunchcrafts.api.ability.AttackPlan;
import com.onepunchcrafts.api.ability.Timeline;
import com.onepunchcrafts.api.combat.DamageSpec;
import com.onepunchcrafts.api.combat.DamageTier;
import com.onepunchcrafts.api.combat.StrikeDefinition;
import com.onepunchcrafts.api.effect.EffectSpec;
import com.onepunchcrafts.runtime.PowerRegistries;
import com.onepunchcrafts.runtime.ability.DeclarativeAbility;
import com.onepunchcrafts.runtime.combat.DamagePipeline;

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
    public static final List<Id> VFX_TECHNIQUES = List.of(
            WEAK_PUNCH, NORMAL_PUNCH, SERIOUS_PUNCH, WEAKENING_PUNCH,
            QUICK_BACKSTAB, NORMAL_PUNCHES_IN_AREA, DASH, SERIOUS_FART,
            SPEED, WEIGHT, KNOCKBACK_RESISTANCE, ATTACK_KNOCKBACK,
            SWIM_SPEED, BREAK_BLOCKS, EXTREME_SPEED, EXTREME_JUMP);

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
    public static final Id TIMELINE_QUICK_BACKSTAB = id("timeline/quick_backstab");
    public static final Id TIMELINE_NORMAL_PUNCHES_IN_AREA = id("timeline/normal_punches_in_area");

    private SaitamaContent() {}

    public static void register(PowerRegistries registries) {
        registerStrikes(registries);
        registerTechniques(registries);
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
                        Timeline.builder(TIMELINE_QUICK_BACKSTAB, 1)
                                .at(0, new Timeline.Command.TeleportToTarget(Timeline.TargetRef.PRIMARY))
                                .at(0, new Timeline.Command.StrikeTarget(NORMAL_STRIKE, Timeline.TargetRef.PRIMARY))
                                .build())));
        registries.abilities.register(DASH, new DeclarativeAbility(DASH, ignored -> Optional.empty(),
                ignored -> new Ability.Activation.Scheduled(Timeline.builder(id("timeline/dash"), 1)
                        .at(0, new Timeline.Command.Dash(30)).build())));

        registries.powerSets.register(POWER_SET, new PowerSetDefinition(POWER_SET, Map.of(
                ATTR_SPEED, 0.0, ATTR_WEIGHT, 0.0, ATTR_KNOCKBACK_RESISTANCE, 0.0,
                ATTR_ATTACK_KNOCKBACK, 0.0, ATTR_SWIM_SPEED, 0.0), Map.of(),
                Set.of(DamagePipeline.SAITAMA_TARGET), List.of(
                new PowerSetDefinition.TechniquePage("technique.page.combat", List.of(
                        WEAK_PUNCH, NORMAL_PUNCH, SERIOUS_PUNCH, WEAKENING_PUNCH,
                        QUICK_BACKSTAB, NORMAL_PUNCHES_IN_AREA, DASH, SERIOUS_FART)),
                new PowerSetDefinition.TechniquePage("technique.page.body", List.of(
                        SPEED, WEIGHT, KNOCKBACK_RESISTANCE, ATTACK_KNOCKBACK,
                        SWIM_SPEED, BREAK_BLOCKS, EXTREME_SPEED, EXTREME_JUMP)))));
    }

    private static void registerTechniques(PowerRegistries registries) {
        registries.techniques.register(WEAK_PUNCH, Technique.combat(WEAK_PUNCH,
                "skill.saitama.weak_punch", WEAK_PUNCH,
                "technique.saitama.weak_punch.primary", "technique.saitama.weak_punch.active"));
        registries.techniques.register(NORMAL_PUNCH, Technique.combat(NORMAL_PUNCH,
                "skill.saitama.normal_punch", NORMAL_PUNCH,
                "technique.saitama.normal_punch.primary", "technique.saitama.normal_punch.active"));
        registries.techniques.register(SERIOUS_PUNCH, Technique.combat(SERIOUS_PUNCH,
                "skill.saitama.serious_punch", SERIOUS_PUNCH,
                "technique.saitama.serious_punch.primary", "technique.saitama.serious_punch.active"));
        registries.techniques.register(WEAKENING_PUNCH, Technique.primary(WEAKENING_PUNCH,
                "skill.saitama.weakening_punch", WEAKENING_PUNCH,
                "technique.saitama.weakening_punch.primary"));
        registries.techniques.register(QUICK_BACKSTAB, Technique.cast(QUICK_BACKSTAB,
                "skill.saitama.quick_backstab", QUICK_BACKSTAB, "technique.saitama.quick_backstab.active"));
        registries.techniques.register(NORMAL_PUNCHES_IN_AREA, Technique.cast(NORMAL_PUNCHES_IN_AREA,
                "skill.saitama.normalpuncharmy", NORMAL_PUNCHES_IN_AREA,
                "technique.saitama.normal_punches_in_area.active"));
        registries.techniques.register(DASH, Technique.cast(DASH,
                "skill.saitama.dash", DASH, "technique.saitama.dash.active"));
        registries.techniques.register(SERIOUS_FART, Technique.toggle(SERIOUS_FART,
                "skill.saitama.serious_fart", TAG_SERIOUS_FART, "technique.action.toggle"));
        registries.techniques.register(SPEED, Technique.adjustable(SPEED,
                "skill.saitama.super_speed", ATTR_SPEED, 0, 500, 1, "technique.action.adjust"));
        registries.techniques.register(WEIGHT, Technique.adjustable(WEIGHT,
                "skill.saitama.set_weight", ATTR_WEIGHT, 0, 500, 1, "technique.action.adjust"));
        registries.techniques.register(KNOCKBACK_RESISTANCE, Technique.adjustable(KNOCKBACK_RESISTANCE,
                "skill.saitama.knockback_resistance", ATTR_KNOCKBACK_RESISTANCE, 0, 500, 1,
                "technique.action.adjust"));
        registries.techniques.register(ATTACK_KNOCKBACK, Technique.adjustable(ATTACK_KNOCKBACK,
                "skill.saitama.attack_knockback", ATTR_ATTACK_KNOCKBACK, 0, 500, 1,
                "technique.action.adjust"));
        registries.techniques.register(SWIM_SPEED, Technique.adjustable(SWIM_SPEED,
                "skill.saitama.swim_speed", ATTR_SWIM_SPEED, 0, 500, 1, "technique.action.adjust"));
        registries.techniques.register(BREAK_BLOCKS, Technique.toggle(BREAK_BLOCKS,
                "skill.saitama.break_blocks_quickly", TAG_BREAK_BLOCKS, "technique.action.toggle"));
        registries.techniques.register(EXTREME_SPEED, Technique.toggle(EXTREME_SPEED,
                "skill.saitama.extreme_speed", TAG_EXTREME_SPEED, "technique.action.toggle"));
        registries.techniques.register(EXTREME_JUMP, Technique.toggle(EXTREME_JUMP,
                "skill.saitama.extreme_jump", TAG_EXTREME_JUMP, "technique.action.toggle"));
    }

    private static void registerStrikes(PowerRegistries registries) {
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
            Timeline.Builder timeline = Timeline.builder(TIMELINE_NORMAL_PUNCHES_IN_AREA,
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
