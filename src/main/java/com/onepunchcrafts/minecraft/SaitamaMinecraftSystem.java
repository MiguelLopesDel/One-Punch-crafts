package com.onepunchcrafts.minecraft;

import com.onepunchcrafts.network.packet.SaitamaVfxPacket;
import com.onepunchcrafts.network.packet.SaitamaTechniqueVfxPacket;
import com.onepunchcrafts.api.presentation.VfxProfile;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.content.SaitamaContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Minecraft projection of Saitama attributes/passives; writes only on change. */
public final class SaitamaMinecraftSystem {
    private static final Map<UUID, Integer> SHIFT_HOLD = new HashMap<>();
    private static final Map<UUID, Vec3> PREVIOUS = new HashMap<>();
    private static final Map<UUID, Boolean> PREVIOUS_GROUND = new HashMap<>();
    private static final Map<UUID, Double> PREVIOUS_VERTICAL_SPEED = new HashMap<>();

    private SaitamaMinecraftSystem() {}

    /** Releases per-player transient state when Saitama is removed or reassigned. */
    public static void clear(ServerPlayer player) {
        SHIFT_HOLD.remove(player.getUUID());
        PREVIOUS.remove(player.getUUID());
        PREVIOUS_GROUND.remove(player.getUUID());
        PREVIOUS_VERTICAL_SPEED.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        var state = HelpUtility.getSkillData(player).getPowerState();
        if (!state.powerSetId().equals(SaitamaContent.POWER_SET)) return;

        set(player, ForgeMod.ENTITY_GRAVITY.get(), value(state, SaitamaContent.ATTR_WEIGHT) == 0
                ? 0.08 : value(state, SaitamaContent.ATTR_WEIGHT) / 10.0);
        set(player, Attributes.MOVEMENT_SPEED, value(state, SaitamaContent.ATTR_SPEED) == 0
                ? 0.1 : value(state, SaitamaContent.ATTR_SPEED) / 9.0);
        set(player, Attributes.ATTACK_KNOCKBACK, value(state, SaitamaContent.ATTR_ATTACK_KNOCKBACK));
        set(player, Attributes.KNOCKBACK_RESISTANCE, value(state, SaitamaContent.ATTR_KNOCKBACK_RESISTANCE));
        set(player, ForgeMod.SWIM_SPEED.get(), value(state, SaitamaContent.ATTR_SWIM_SPEED) == 0
                ? 1 : value(state, SaitamaContent.ATTR_SWIM_SPEED));
        set(player, Attributes.ATTACK_SPEED, 500);

        if (player.isOnFire()) player.clearFire();
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.LEVITATION);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        HelpUtility.applySaitamaEffectsSet(player);

        if (!state.tags().contains(SaitamaContent.TAG_EXTREME_JUMP)) {
            if (player.isShiftKeyDown()) {
                int held = SHIFT_HOLD.merge(player.getUUID(), 1, Integer::sum);
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 1, Math.min(held, 127)));
            } else SHIFT_HOLD.remove(player.getUUID());
        }
        movementVfx(player, state);
    }

    private static void movementVfx(ServerPlayer player, com.onepunchcrafts.runtime.state.PowerState state) {
        Vec3 current = player.position();
        Vec3 previous = PREVIOUS.put(player.getUUID(), current);
        boolean onGround = player.onGround();
        Boolean wasOnGround = PREVIOUS_GROUND.put(player.getUUID(), onGround);
        double verticalSpeed = player.getDeltaMovement().y;
        Double previousVertical = PREVIOUS_VERTICAL_SPEED.put(player.getUUID(), verticalSpeed);
        if (previous == null || wasOnGround == null || player.isSpectator()) return;

        Vec3 movement = current.subtract(previous);
        double speed = movement.horizontalDistance();
        Vec3 horizontalDirection = speed < 1.0e-4 ? player.getLookAngle()
                : new Vec3(movement.x, 0, movement.z).normalize();
        long gameTime = player.level().getGameTime();

        if (gameTime % 2 == 0 && speed >= 0.5) {
            Vec3 trailOrigin = current.add(0, 0.9, 0).subtract(horizontalDirection.scale(0.6));
            if (state.tags().contains(SaitamaContent.TAG_EXTREME_SPEED)
                    && state.vfxPreferences().get(SaitamaContent.EXTREME_SPEED) == VfxProfile.NEW) {
                SaitamaTechniqueVfxPacket.broadcast(player.serverLevel(), new SaitamaTechniqueVfxPacket(
                        player.getId(), trailOrigin, horizontalDirection, (float) speed,
                        SaitamaTechniqueVfxPacket.EXTREME_SPEED, 6));
            } else {
                VfxProfile selected = state.tags().contains(SaitamaContent.TAG_EXTREME_SPEED)
                        ? state.vfxPreferences().get(SaitamaContent.EXTREME_SPEED)
                        : state.vfxPreferences().get(SaitamaContent.SPEED);
                SaitamaVfxPacket.broadcast(player.serverLevel(), new SaitamaVfxPacket(player.getId(),
                        trailOrigin, horizontalDirection, (float) speed,
                        SaitamaVfxPacket.STYLE_SPEED_TRAIL, 6, selected));
            }
        }

        if (gameTime % 2 == 0 && player.isInWater() && movement.lengthSqr() > 0.02
                && state.vfxPreferences().get(SaitamaContent.SWIM_SPEED) == VfxProfile.NEW) {
            Vec3 swimDirection = movement.lengthSqr() < 1.0e-4 ? player.getLookAngle() : movement.normalize();
            float swimScale = (float) Math.min(5.0, Math.max(0.5,
                    value(state, SaitamaContent.ATTR_SWIM_SPEED)));
            SaitamaTechniqueVfxPacket.broadcast(player.serverLevel(), new SaitamaTechniqueVfxPacket(
                    player.getId(), current.add(0, player.getBbHeight() * 0.5, 0), swimDirection,
                    swimScale, SaitamaTechniqueVfxPacket.SWIM_WAKE, 7));
        }

        double weight = value(state, SaitamaContent.ATTR_WEIGHT);
        if (!wasOnGround && onGround
                && state.vfxPreferences().get(SaitamaContent.WEIGHT) == VfxProfile.NEW) {
            float force = (float) Math.min(6.0, Math.max(0.7,
                    Math.abs(previousVertical == null ? verticalSpeed : previousVertical) * 2.5 + weight / 60.0));
            SaitamaTechniqueVfxPacket.broadcast(player.serverLevel(), new SaitamaTechniqueVfxPacket(
                    player.getId(), current.add(0, 0.06, 0), new Vec3(0, 1, 0), force,
                    SaitamaTechniqueVfxPacket.WEIGHT, 16));
        } else if (onGround && speed > 0.08 && weight > 1 && gameTime % 6 == 0
                && state.vfxPreferences().get(SaitamaContent.WEIGHT) == VfxProfile.NEW) {
            float force = (float) Math.min(2.5, 0.25 + weight / 120.0);
            SaitamaTechniqueVfxPacket.broadcast(player.serverLevel(), new SaitamaTechniqueVfxPacket(
                    player.getId(), current.add(0, 0.04, 0), horizontalDirection, force,
                    SaitamaTechniqueVfxPacket.WEIGHT, 8));
        }

        if (wasOnGround && !onGround && verticalSpeed > 0.05
                && state.tags().contains(SaitamaContent.TAG_EXTREME_JUMP)) {
            if (state.vfxPreferences().get(SaitamaContent.EXTREME_JUMP) != VfxProfile.NEW) return;
            Vec3 jumpDirection = movement.lengthSqr() < 1.0e-4 ? new Vec3(0, 1, 0) : movement.normalize();
            SaitamaTechniqueVfxPacket.broadcast(player.serverLevel(), new SaitamaTechniqueVfxPacket(
                    player.getId(), previous.add(0, 0.06, 0), jumpDirection,
                    (float) Math.min(6.0, 1.5 + verticalSpeed * 2.0),
                    SaitamaTechniqueVfxPacket.EXTREME_JUMP, 18));
        }
    }


    private static double value(com.onepunchcrafts.runtime.state.PowerState state, com.onepunchcrafts.api.Id id) {
        return state.attributes().value(id);
    }

    private static void set(ServerPlayer player, Attribute attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && Double.compare(instance.getBaseValue(), value) != 0) instance.setBaseValue(value);
    }
}
