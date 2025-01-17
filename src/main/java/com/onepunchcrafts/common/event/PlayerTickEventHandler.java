package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

@Mod.EventBusSubscriber
public class PlayerTickEventHandler {

    private static final Map<Player, Integer> shiftHoldTime = new HashMap<>();

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        HelpUtility.getSkillData(player).getSkillPack().tick(event);
        if (event.player instanceof ServerPlayer serverPlayer) {
            manageEffectsAndAttributes(event);
            explodeNormalMobs(serverPlayer);
        }
    }

    private static void manageEffectsAndAttributes(TickEvent.PlayerTickEvent event) {
        ServerPlayer player = (ServerPlayer) event.player;
        player.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            if (cap.getSkillPack() instanceof SaitamaPack saitamaPack) {
                modifyAttributes(player, saitamaPack);
                if (player.isOnFire())
                    player.clearFire();
                removeNegativeEffectsOfSaitama(player);
                HelpUtility.applySaitamaEffectsSet(player);
                if (event.phase == TickEvent.Phase.END)
                    handlerJumpPower(player);
            }
        });
    }

    private static void handlerJumpPower(ServerPlayer player) {
        int value = shiftHoldTime.getOrDefault(player, 0);
        if (player.isShiftKeyDown()) {
            shiftHoldTime.put(player, ++value);
        } else {
            shiftHoldTime.remove(player);
        }
        if (shiftHoldTime.containsKey(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 1, Math.min(value, 127)));
        }
    }

    private static void modifyAttributes(ServerPlayer player, SaitamaPack cap) {
        if (player.isSpectator())
            return;
        //0.08
        if (cap.getWeight() != 0)
            player.getAttribute(ForgeMod.ENTITY_GRAVITY.get()).setBaseValue((double) cap.getWeight() / 10);
        else
            player.getAttribute(ForgeMod.ENTITY_GRAVITY.get()).setBaseValue(0.08);
        if (cap.getSpeed() != 0)
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double) cap.getSpeed() / 9);
        else
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1F);

        if (cap.getAttackKnockback() != 0)
            player.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(cap.getAttackKnockback());
        else
            player.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(0);

        if (cap.getKnockbackResistance() != 0)
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(cap.getKnockbackResistance());
        else
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);

        if (cap.getSwimSpeed() != 0)
            player.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(cap.getSwimSpeed());
        else
            player.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(1.0D);

        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(500F);
    }

    private static void removeNegativeEffectsOfSaitama(ServerPlayer player) {
        if (player.getEffect(MobEffects.DARKNESS) != null) {
            player.removeEffect(MobEffects.DARKNESS);
        }
        if (player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (player.getEffect(MobEffects.BLINDNESS) != null) {
            player.removeEffect(MobEffects.BLINDNESS);
        }
        if (player.getEffect(MobEffects.WEAKNESS) != null) {
            player.removeEffect(MobEffects.WEAKNESS);
        }
        if (player.getEffect(MobEffects.LEVITATION) != null) {
            player.removeEffect(MobEffects.LEVITATION);
        }
        if (player.getEffect(MobEffects.POISON) != null) {
            player.removeEffect(MobEffects.POISON);
        }
        if (player.getEffect(MobEffects.DIG_SLOWDOWN) != null) {
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
        if (player.getEffect(MobEffects.CONFUSION) != null) {
            player.removeEffect(MobEffects.CONFUSION);
        }
    }

    private static void explodeNormalMobs(ServerPlayer player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        AABB aabb = new AABB(x - 100, y - 100, z - 100, x + 100, y + 100, z + 100);
        ServerLevel level = (ServerLevel) player.level();
        level.getEntitiesOfClass(LivingEntity.class, aabb, e -> e.getTags().contains("targetnormalpunch")).forEach(
                e -> {
                    double x1 = e.getX();
                    double y1 = e.getY();
                    double z1 = e.getZ();
                    level.explode(e, x1, y1 + 0.0625D, z1, 12.0F, Level.ExplosionInteraction.MOB);
                    level.sendParticles(ParticleTypes.FLAME, x1, y1, z1, 10, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.FLASH, x1, y1, z1, 10, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.FIREWORK, x1, y1, z1, 10, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.FIREWORK, x1, y1, z1, 10, 0, 0, 0, 0);
                }
        );
    }

}
