package com.onepunchcrafts.v3.minecraft;

import com.onepunchcrafts.common.skills.saitama.SeriousPunch;
import com.onepunchcrafts.network.packet.SaitamaVfxPacket;
import com.onepunchcrafts.network.packet.SeriousPunchVfxPacket;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AnimationPacket;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.OnePunchV3;
import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.ability.Timeline;
import com.onepunchcrafts.v3.api.combat.DamageContext;
import com.onepunchcrafts.v3.api.combat.DamageTier;
import com.onepunchcrafts.v3.api.effect.EffectSpec;
import com.onepunchcrafts.v3.content.SaitamaContent;
import com.onepunchcrafts.v3.core.PowerEngine;
import com.onepunchcrafts.v3.core.ability.AbilityBook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.UUID;

/** Executes pure Timeline commands against Minecraft at the environment Seam. */
public final class MinecraftExecutionSink implements PowerEngine.ExecutionSink {
    private final ServerPlayer actor;

    public MinecraftExecutionSink(ServerPlayer actor) { this.actor = actor; }

    @Override public void strike(Id strikeId, String targetId) {
        LivingEntity target = living(targetId);
        if (target == null || target == actor) return;
        var strike = OnePunchV3.REGISTRIES.strikes.require(strikeId);
        var damage = strike.damage();
        if (strikeId.equals(SaitamaContent.WEAKENING_STRIKE)) {
            stripEquipment(target);
            damage = damage.withAmount(Math.min(Math.max(0, target.getHealth() - 0.0001), 100_000));
        }
        MinecraftDamageAdapter adapter = new MinecraftDamageAdapter(actor, target);
        var result = OnePunchV3.DAMAGE.execute(new DamageContext(actor.getStringUUID(), adapter, damage), adapter);
        if (damage.tier() == DamageTier.DRAGON && result.hadEffect()) {
            MinecraftEffectRuntime.apply(target, SaitamaContent.EFFECT_PUNCHED);
            Vec3 offset = target.position().subtract(actor.position());
            target.knockback(5, offset.x, offset.z);
        }
        impactCue(target, damage.tier());
    }

    @Override public void timeline(AbilityBook.Emission emission) {
        Timeline.Command command = emission.step().command();
        if (command instanceof Timeline.Command.Cue cue) cue(cue.cueId(), emission);
        else if (command instanceof Timeline.Command.StrikeTarget strike)
            strike(strike.strikeId(), resolveTarget(strike.target(), emission));
        else if (command instanceof Timeline.Command.TeleportToTarget teleport) {
            LivingEntity target = living(resolveTarget(teleport.target(), emission));
            if (target != null) actor.teleportTo(target.getX(), target.getY(), target.getZ());
        } else if (command instanceof Timeline.Command.StrikeArea area) {
            AABB bounds = actor.getBoundingBox().inflate(area.horizontalRadius(), area.verticalRadius(), area.horizontalRadius());
            actor.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds,
                    target -> target != actor && target.isAlive()).forEach(target -> strike(area.strikeId(), target.getStringUUID()));
        } else if (command instanceof Timeline.Command.StrikeCone cone) {
            Vec3 origin = origin(emission);
            Vec3 look = look(emission);
            double minimumDot = Math.cos(Math.toRadians(cone.halfAngleDegrees()));
            AABB bounds = new AABB(origin, origin.add(look.scale(cone.range()))).inflate(cone.range());
            actor.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds, target -> {
                if (target == actor || !target.isAlive()) return false;
                Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
                return toTarget.lengthSqr() <= cone.range() * cone.range()
                        && toTarget.normalize().dot(look) >= minimumDot;
            }).forEach(target -> strike(cone.strikeId(), target.getStringUUID()));
        } else if (command instanceof Timeline.Command.StrikeCylinder cylinder) {
            Vec3 origin = origin(emission);
            Vec3 look = look(emission);
            Vec3 end = origin.add(look.scale(cylinder.length()));
            AABB bounds = new AABB(origin, end).inflate(cylinder.radius());
            actor.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds, target -> {
                if (target == actor || !target.isAlive()) return false;
                Vec3 relative = target.getBoundingBox().getCenter().subtract(origin);
                double along = relative.dot(look);
                return along >= 0 && along <= cylinder.length()
                        && relative.subtract(look.scale(along)).lengthSqr() <= cylinder.radius() * cylinder.radius();
            }).forEach(target -> strike(cylinder.strikeId(), target.getStringUUID()));
        } else if (command instanceof Timeline.Command.DestroyCylinder) {
            Vec3 direction = look(emission);
            SeriousPunch.releaseSeriousVisuals(actor, actor.serverLevel(), direction);
            MinecraftDestructionSystem.startCylinder(actor, origin(emission), direction,
                    ((Timeline.Command.DestroyCylinder) command).radius(),
                    ((Timeline.Command.DestroyCylinder) command).length());
        } else if (command instanceof Timeline.Command.Dash dash) {
            Vec3 start = actor.getEyePosition();
            Vec3 direction = look(emission);
            Vec3 end = start.add(direction.scale(dash.distance()));
            Vec3 hit = actor.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, actor)).getLocation().subtract(direction);
            actor.teleportTo(hit.x, hit.y - actor.getEyeHeight(), hit.z);
            SaitamaVfxPacket.broadcast(actor.serverLevel(), new SaitamaVfxPacket(actor.getId(), start, direction,
                    (float) start.distanceTo(hit), SaitamaVfxPacket.STYLE_DASH, 12));
        }
    }

    @Override public void effect(EffectSpec.Operation operation) {
        if (operation instanceof EffectSpec.Operation.AddTag add) state().tags().add(add.tag());
        else if (operation instanceof EffectSpec.Operation.RemoveTag remove) state().tags().remove(remove.tag());
        else if (operation instanceof EffectSpec.Operation.AttributeDelta delta)
            state().attributes().setBase(delta.attribute(), state().attributes().base(delta.attribute()) + delta.amount());
        else if (operation instanceof EffectSpec.Operation.ResourceDelta delta)
            state().resources().add(delta.resource(), delta.amount());
    }

    private void cue(Id cue, AbilityBook.Emission emission) {
        if (cue.equals(SaitamaContent.CUE_SERIOUS_WINDUP)) {
            HelpUtility.clientEffects(actor);
            SeriousPunchVfxPacket.broadcast(actor.serverLevel(), new SeriousPunchVfxPacket(
                    actor.getId(), origin(emission).add(0, actor.getEyeHeight(), 0).add(look(emission).scale(1.2)),
                    look(emission), SeriousPunch.WINDUP_TICKS));
        } else if (cue.equals(SaitamaContent.CUE_BARRAGE)) {
            NetworkRegister.sendToAllClientsExcept(actor, new AnimationPacket(actor.getStringUUID(), "multiple_punches"));
            SaitamaVfxPacket.broadcast(actor.serverLevel(), new SaitamaVfxPacket(actor.getId(), actor.getEyePosition(),
                    actor.getLookAngle(), 1, SaitamaVfxPacket.STYLE_BARRAGE, 100));
        } else if (cue.equals(SaitamaContent.CUE_BARRAGE_END)) {
            NetworkRegister.sendToAllClientsExcept(actor, new AnimationPacket(actor.getStringUUID(), "stop"));
        }
    }

    private void impactCue(LivingEntity target, DamageTier tier) {
        if (tier == DamageTier.SERIOUS) return;
        float scale = tier == DamageTier.DRAGON ? 1.0f : 0.5f;
        Vec3 direction = target.position().subtract(actor.position());
        if (direction.lengthSqr() > 0) direction = direction.normalize();
        SaitamaVfxPacket.broadcast(actor.serverLevel(), new SaitamaVfxPacket(actor.getId(),
                target.position().add(0, target.getBbHeight() * 0.6, 0), direction, scale,
                SaitamaVfxPacket.STYLE_PUNCH_IMPACT, tier == DamageTier.DRAGON ? 7 : 5));
    }

    private void stripEquipment(LivingEntity target) {
        for (InteractionHand hand : InteractionHand.values()) {
            target.spawnAtLocation(target.getItemInHand(hand));
            target.setItemInHand(hand, ItemStack.EMPTY);
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            target.spawnAtLocation(target.getItemBySlot(slot));
            target.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private String resolveTarget(Timeline.TargetRef reference, AbilityBook.Emission emission) {
        if (reference == Timeline.TargetRef.PRIMARY) return emission.primaryTarget();
        int index = Math.max(0, emission.step().tick() / 5);
        return index < emission.capturedTargets().size() ? emission.capturedTargets().get(index) : null;
    }

    private LivingEntity living(String id) {
        if (id == null) return null;
        try {
            Entity entity = actor.serverLevel().getEntity(UUID.fromString(id));
            return entity instanceof LivingEntity living ? living : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Vec3 origin(AbilityBook.Emission emission) {
        return new Vec3(value(emission, "origin_x", actor.getX()), value(emission, "origin_y", actor.getY()),
                value(emission, "origin_z", actor.getZ()));
    }

    private Vec3 look(AbilityBook.Emission emission) {
        Vec3 look = new Vec3(value(emission, "look_x", actor.getLookAngle().x),
                value(emission, "look_y", actor.getLookAngle().y), value(emission, "look_z", actor.getLookAngle().z));
        return look.lengthSqr() == 0 ? actor.getLookAngle() : look.normalize();
    }

    private double value(AbilityBook.Emission emission, String key, double fallback) {
        return emission.parameters().getOrDefault(key, fallback);
    }

    private com.onepunchcrafts.v3.core.state.PowerState state() {
        return HelpUtility.getSkillData(actor).getPowerState();
    }
}
