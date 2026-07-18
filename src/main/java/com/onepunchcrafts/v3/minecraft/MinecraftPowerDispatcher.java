package com.onepunchcrafts.v3.minecraft;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.OnePunchV3;
import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.ability.AbilityContext;
import com.onepunchcrafts.v3.content.SaitamaContent;
import com.onepunchcrafts.v3.core.state.PowerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Forge/Minecraft inbound Adapter for primary attack, cast and tick intents. */
public final class MinecraftPowerDispatcher {
    private MinecraftPowerDispatcher() {}

    public static boolean primaryAttack(ServerPlayer player, Entity target) {
        if (!(target instanceof LivingEntity living)) return false;
        PowerState state = HelpUtility.getSkillData(player).getPowerState();
        return OnePunchV3.POWERS.primaryAttack(state, context(player, state, Optional.of(living), List.of()),
                new MinecraftExecutionSink(player));
    }

    public static void cast(ServerPlayer player, Id abilityId) {
        PowerState state = HelpUtility.getSkillData(player).getPowerState();
        Optional<LivingEntity> primary = Optional.empty();
        List<String> captured = List.of();
        if (abilityId.equals(SaitamaContent.QUICK_BACKSTAB)) primary = rayTarget(player, 300);
        if (abilityId.equals(SaitamaContent.NORMAL_PUNCHES_IN_AREA)) {
            AABB area = player.getBoundingBox().inflate(50, 53, 50);
            captured = player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != player && entity.isAlive()).stream()
                    .sorted(Comparator.comparingDouble(player::distanceToSqr)).map(Entity::getStringUUID).toList();
        }
        OnePunchV3.POWERS.activate(state, abilityId, context(player, state, primary, captured), new MinecraftExecutionSink(player));
    }

    public static void tick(ServerPlayer player) {
        PowerState state = HelpUtility.getSkillData(player).getPowerState();
        if (!state.powerSetId().equals(PowerState.NONE))
            OnePunchV3.POWERS.tick(state, player.serverLevel().getGameTime(), new MinecraftExecutionSink(player));
        SaitamaMinecraftSystem.tick(player);
    }

    private static AbilityContext context(ServerPlayer player, PowerState state, Optional<LivingEntity> target,
                                          List<String> captured) {
        Vec3 look = player.getLookAngle();
        return new AbilityContext(player.getStringUUID(), player.serverLevel().getGameTime(),
                target.map(Entity::getStringUUID), captured, state.powerSetId(), Map.of(
                "origin_x", player.getX(), "origin_y", player.getY(), "origin_z", player.getZ(),
                "look_x", look.x, "look_y", look.y, "look_z", look.z));
    }

    private static Optional<LivingEntity> rayTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB bounds = new AABB(start, start.add(look.scale(range))).inflate(1);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity != player && entity.isAlive())
                .stream().filter(entity -> {
                    Vec3 to = entity.getBoundingBox().getCenter().subtract(start);
                    return to.normalize().dot(look) > 0.995;
                }).min(Comparator.comparingDouble(player::distanceToSqr));
    }
}
