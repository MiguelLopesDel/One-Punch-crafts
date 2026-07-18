package com.onepunchcrafts.minecraft;

import com.onepunchcrafts.network.packet.SaitamaVfxPacket;
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

    private SaitamaMinecraftSystem() {}

    /** Releases per-player transient state when Saitama is removed or reassigned. */
    public static void clear(ServerPlayer player) {
        SHIFT_HOLD.remove(player.getUUID());
        PREVIOUS.remove(player.getUUID());
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
        speedTrail(player);
    }

    private static void speedTrail(ServerPlayer player) {
        Vec3 current = player.position();
        Vec3 previous = PREVIOUS.put(player.getUUID(), current);
        if (previous == null || player.level().getGameTime() % 2 != 0 || player.isSpectator()) return;
        Vec3 movement = current.subtract(previous);
        double speed = movement.horizontalDistance();
        if (speed < 0.5) return;
        Vec3 direction = new Vec3(movement.x, 0, movement.z).normalize();
        SaitamaVfxPacket.broadcast(player.serverLevel(), new SaitamaVfxPacket(player.getId(),
                current.add(0, 0.9, 0).subtract(direction.scale(0.6)), direction, (float) speed,
                SaitamaVfxPacket.STYLE_SPEED_TRAIL, 6));
    }

    private static double value(com.onepunchcrafts.runtime.state.PowerState state, com.onepunchcrafts.api.Id id) {
        return state.attributes().value(id);
    }

    private static void set(ServerPlayer player, Attribute attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && Double.compare(instance.getBaseValue(), value) != 0) instance.setBaseValue(value);
    }
}
