package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

@Mod.EventBusSubscriber
public class PlayerTickEventHandler {

    private static final Map<Player, Integer> shiftHoldTime = new HashMap<>();

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer player) {
            player.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
                if (cap.isSaitama()) {
                    int superSpeed = 100;
                    if (player.isOnFire())
                        player.clearFire();
                    boolean lessVelocity = player.getEffect(MobEffects.MOVEMENT_SPEED) == null ||
                            player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() < superSpeed;
                    boolean greatVelocity = player.getEffect(MobEffects.MOVEMENT_SPEED) != null &&
                            player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() >= superSpeed;
                    if (cap.isSuperSpeed() && lessVelocity) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, superSpeed));
                    } else if (!cap.isSuperSpeed() && greatVelocity)
                        player.removeEffect(MobEffects.MOVEMENT_SPEED);
                    removeEffectsOfSaitama(player);
                    applyEffectInSaitama(player);
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
            });
            explodeNormalMobs(player);
        }
    }

    private static void removeEffectsOfSaitama(ServerPlayer player) {
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
    }

    private static void applyEffectInSaitama(ServerPlayer player) {
        if (player.getEffect(MobEffects.NIGHT_VISION) == null) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 255));
        }
        if (player.getEffect(MobEffects.DIG_SPEED) == null) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, -1, 255));
        }
        if (player.getEffect(MobEffects.DOLPHINS_GRACE) == null) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, -1, 255));
        }
        if (player.getEffect(MobEffects.MOVEMENT_SPEED) == null) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 5));
        }
    }

    private static void explodeNormalMobs(ServerPlayer player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        AABB aabb = new AABB(x - 100, y - 100, z - 100, x + 100, y + 100, z + 100);
        ServerLevel level = (ServerLevel) player.level();
        level.getEntitiesOfClass(LivingEntity.class, aabb, e -> e.getTags().contains("targetnormalpunch")).stream().filter(LivingEntity::isDeadOrDying).forEach(
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
