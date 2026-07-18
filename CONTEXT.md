# OnePunchCrafts

A Forge 1.20.1 mod where players become One-Punch Man characters. The domain is
Power Sets: who you are determines your Techniques, durability, and how the
world reacts when you use them.

## Language

### Characters and kits

**Power Set**:
The complete definition that makes a player a character — their Techniques,
attributes, resources and combat rules. Holding no Power Set means an ordinary player.
_Avoid_: Pack, class, kit, capability

**Technique**:
One selectable entry in a Power Set. It routes Primary Attack and explicit Technique
Activation independently, and may instead expose a toggle or adjustable control.
_Avoid_: selected Ability, Skill, stance, move

**Technique Page**:
An ordered radial-wheel page containing at most eight Techniques. The first page is
the character's fast combat selection; additional pages hold broader controls.
_Avoid_: Skill Group, category, loadout index

**Ability**:
An executable behavior referenced by a Technique or composite action. An Ability is
never selected directly and may produce an Attack Plan or Timeline.
_Avoid_: Technique, Skill

### Combat

**Flux**:
The ordered resolution of a Strike, where attacker rules, target rules and
world rules may influence the attempted hit before its outcome is observed.
Forge's event sequence is an integration mechanism, not the Flux itself.
_Avoid_: EventBus relay, damage hook

**Strike**:
A single offensive action whose identity is fixed when it starts. It may be
emitted by a Primary Attack or as one step of a Technique Activation.
_Avoid_: stance, current skill (when referring to the resulting hit)

**Primary Attack**:
The left-click action whose behavior is supplied by the selected punch Technique.
Normal and Weak Punch emit one Strike; Serious Punch also releases its full
sequence.
_Avoid_: Technique Activation, Skill Activation, cast, plain punch

**Technique Activation**:
The explicit use of a selected Technique, distinct from Primary Attack. For punch
Techniques it may repeat a Strike or provide a different way to deliver the same move.
_Avoid_: Primary Attack, Skill Activation, click

**Consecutive Normal Punches**:
The anime barrage unleashed by activating Normal Punch: ~5 seconds of accelerating
Normal Punch Strikes across a steerable wide aim cone. Its waves pin every target
they catch, then a final shockwave launches survivors. One activation is one barrage.
_Avoid_: normal punch (when meaning the barrage), multiple punches

**Consecutive Weak Punches**:
The barrage unleashed by activating Weak Punch: ~5 seconds of repeated Weak Punch
Strikes against nearby targets.
_Avoid_: Weak Punch (when meaning one Strike)

**Normal Punches in Area**:
A composite Saitama Technique that teleports him through nearby targets and delivers
a Normal Punch Strike to each one.
_Avoid_: Consecutive Normal Punches, normal punch army

**Quick Backstab**:
A composite Saitama Technique that acquires a distant target, teleports Saitama to
it and delivers one Normal Punch Strike.
_Avoid_: Primary Attack, Weak Punch

**Serious Punch**:
Saitama's finisher bypasses every form of mitigation and defeats every eligible
non-Saitama combat target; a Saitama target survives even another Saitama's
Serious Punch. Creative and spectator players are outside the eligible target
set. Its Primary Attack delivers a direct Serious Strike plus the full sequence;
its Technique Activation releases the aimed sequence without requiring a melee
target.
_Avoid_: ultimate punch

**Unstoppable**:
A damage rule: against an eligible target not protected by Saitama
Invulnerability, once the hit lands its outcome cannot be denied by defense,
cancellation, invulnerability or event interference. Serious Punch is the
canonical unstoppable attack.
_Avoid_: uncancellable, bypass damage

**Saitama Invulnerability**:
The absolute durability rule of a Saitama target: attacks cannot damage or kill
him, including a Serious Punch delivered by another Saitama.
_Avoid_: Unstoppable exemption list, event cancellation

**Damage Tier**:
The semantic class of a Strike's damage. Rules such as mitigation, caps and
Unstoppable depend on the tier, never on crossing a numeric threshold.
_Avoid_: damage threshold, giant multiplier

**Mitigation**:
Boros' durability rule: each Form takes only a fraction of incoming damage, and
no single sub-threshold hit may exceed the per-hit cap of his max health.
_Avoid_: resistance, armor (armor is a Form, not the rule)

### Boros

**Form**:
Boros' current combat stance: **Armored**, **Released** or **Meteoric Burst**.
Form scales power, speed, mitigation and regeneration.
_Avoid_: mode, stage

**Energy**:
Boros' resource pool for activating Techniques (flight, beams, CSRC). Entirely separate
from health; spending it never hurts him, emptying it Exhausts him.
_Avoid_: mana, stamina

**Exhausted**:
The lockout state entered when Energy hits zero: no skill can be cast and Energy
does not recover until the cooldown elapses.
_Avoid_: out of mana, drained

**Ultra Regeneration**:
Boros' active healing Technique: burns Energy to restore a large fraction of health
over a few seconds, on its own cooldown.
_Avoid_: regen (ambiguous), heal

**Passive Regeneration**:
Boros' always-on health recovery, faster in higher Forms. Costs nothing.
_Avoid_: regen (ambiguous)

**CSRC (Collapsing Star Roaring Cannon)**:
Boros' ultimate beam, only castable in Meteoric Burst. Charge, release,
destruction pipeline and its own full-screen presentation.
_Avoid_: laser, big beam

### Presentation

**VFX Style**:
A lightweight fire-and-forget visual effect identified by a style id (punch
impact, barrage, dash, speed trail). Stateless: one packet spawns it, it plays
out and expires.
_Avoid_: particle effect, animation

**Cinematic**:
A scripted, phased presentation sequence that temporarily owns the screen —
camera, field of view, sound beats and full-screen treatment — driven by a
deterministic client-side timeline (e.g. Serious Punch, CSRC).
_Avoid_: cutscene (the player keeps control), VFX (too generic)

**Windup**:
The held-tension phase of a Cinematic between cast and impact, during which the
world visibly "holds its breath" and the actual effect has not landed yet.
_Avoid_: charge (reserved for CSRC's long pre-fire), delay
