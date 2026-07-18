# OnePunchCrafts

A Forge 1.20.1 mod where players become One-Punch Man characters. The domain is
character kits: who you are determines your skills, your durability, and how the
world reacts when you use them.

## Language

### Characters and kits

**Pack**:
The complete kit that makes a player a character — their skills, attributes,
resources and combat rules. A player is Saitama or Boros *because* they hold that
Pack; holding no Pack means an ordinary player.
_Avoid_: class, kit, capability

**Skill**:
One selectable ability inside a Pack, organized in Skill Groups the player cycles
through. A Skill may define different behavior for a Primary Attack and for its
explicit Skill Activation, or may be a toggle (Flight) or adjustable stat.
_Avoid_: ability, power, move

**Skill Group**:
An ordered page of Skills within a Pack (e.g. Boros: basic / transformation /
ultimate). The player switches groups, then cycles skills within the group.
_Avoid_: category, tab

### Combat

**Flux**:
The server-side stream of combat events (attack, hurt, damage, death) that is
offered to the Packs of both the victim and the attacker, letting each apply its
rules (multipliers, mitigation, cancellation).
_Avoid_: event pipeline, damage hook

**Strike**:
A single offensive action whose identity is fixed when it starts. It may be
emitted by a Primary Attack or as one step of a Skill Activation.
_Avoid_: stance, current skill (when referring to the resulting hit)

**Primary Attack**:
The left-click action whose behavior is supplied by the selected punch Skill.
Normal and Weak Punch emit one Strike; Serious Punch also releases its full
sequence.
_Avoid_: skill activation, cast, plain punch

**Skill Activation**:
The explicit use of a selected Skill, distinct from Primary Attack. For punch
Skills it may repeat a Strike or provide a different way to deliver the same move.
_Avoid_: primary attack, click

**Consecutive Normal Punches**:
The anime barrage unleashed by activating Normal Punch: ~5 seconds of rapid
Normal Punch Strikes in the aim cone. One activation is one barrage.
_Avoid_: normal punch (when meaning the barrage), multiple punches

**Consecutive Weak Punches**:
The barrage unleashed by activating Weak Punch: ~5 seconds of repeated Weak Punch
Strikes against nearby targets.
_Avoid_: Weak Punch (when meaning one Strike)

**Normal Punches in Area**:
A composite Saitama Skill that teleports him through nearby targets and delivers
a Normal Punch Strike to each one.
_Avoid_: Consecutive Normal Punches, normal punch army

**Serious Punch**:
Saitama's finisher bypasses every form of mitigation and defeats every
non-Saitama target; a Saitama target survives even another Saitama's Serious
Punch. Its Primary Attack delivers a direct Serious Strike plus the full
sequence; its Skill Activation releases the aimed sequence without requiring a
melee target.
_Avoid_: ultimate punch

**Unstoppable**:
A damage rule: against a target not protected by Saitama Invulnerability, once
the hit lands its outcome cannot be denied by defense, cancellation,
invulnerability or event interference. Serious Punch is the canonical
unstoppable attack.
_Avoid_: uncancellable, bypass damage

**Saitama Invulnerability**:
The absolute durability rule of a Saitama target: attacks cannot damage or kill
him, including a Serious Punch delivered by another Saitama.
_Avoid_: Unstoppable exemption list, event cancellation

**Serious-damage threshold**:
The damage magnitude (≥10¹²) that marks an attack as Serious Punch territory.
Mitigation and caps do not apply at or above it; Saitama Invulnerability still
takes precedence.

**Mitigation**:
Boros' durability rule: each Form takes only a fraction of incoming damage, and
no single sub-threshold hit may exceed the per-hit cap of his max health.
_Avoid_: resistance, armor (armor is a Form, not the rule)

### Boros

**Form**:
Boros' current combat stance: **Armored** (0), **Released** (1) or **Meteoric
Burst** (2). Form scales power, speed, mitigation and regeneration.
_Avoid_: mode, stage

**Energy**:
Boros' resource pool for casting skills (flight, beams, CSRC). Entirely separate
from health; spending it never hurts him, emptying it Exhausts him.
_Avoid_: mana, stamina

**Exhausted**:
The lockout state entered when Energy hits zero: no skill can be cast and Energy
does not recover until the cooldown elapses.
_Avoid_: out of mana, drained

**Ultra Regeneration**:
Boros' *active* healing skill: burns Energy to restore a large fraction of health
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
