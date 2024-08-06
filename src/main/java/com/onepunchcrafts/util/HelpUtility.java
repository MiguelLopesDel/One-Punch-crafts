package com.onepunchcrafts.util;

import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.entity.GuardianCrystalEntity;
import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity;
import com.brandon3055.draconicevolution.entity.guardian.GuardianFightManager;
import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity.SHIELD_POWER;

public class HelpUtility {

    /**
     * Verifica se o jogador tem a capacidade do mod e se ele é saitama.
     *
     * @param player
     * @return caso o jogador tenha a capacidade e seja um saitama é retorna a propria capacidade caso contrario
     * um optional empty;
     */
    public static Optional<OnePunchPlayer> verifyIsSaitamaAndGetCapability(ServerPlayer player) {
        LazyOptional<OnePunchPlayer> capability = player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY);
        return capability
                .filter(OnePunchPlayer::isSaitama);
    }

    static void hurtDraconicCrystals(ServerLevel level, AABB pArea, DamageSource damageSource) {
        level.getEntitiesOfClass(GuardianCrystalEntity.class, pArea).forEach(entity -> {
            entity.setInvulnerable(false);
            entity.setSecondsOnFire(60);
            boolean oldValue = DEConfig.chaoticBypassCrystalShield;
            DEConfig.chaoticBypassCrystalShield = true;
            entity.hurt(damageSource, 10_000_000_000_000_000f);
            DEConfig.chaoticBypassCrystalShield = oldValue;
        });
    }

    static boolean handleIfDraconicGuardian(LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof DraconicGuardianEntity draconicGuardian))
            return false;
        GuardianFightManager fightManager = draconicGuardian.getFightManager();
        if (fightManager == null)
            return false;
        try {
            Field fieldAliveCrystal = fightManager.getClass().getDeclaredField("aliveCrystals");
            Field fieldEntityData = Class.forName("net.minecraft.world.entity.Entity").getDeclaredField(FMLEnvironment.production ? "f_19804_" : "entityData");
            fieldAliveCrystal.setAccessible(true);
            fieldEntityData.setAccessible(true);

            int oldCrystalNum = (int) fieldAliveCrystal.get(fightManager);
            float oldShieldPower = draconicGuardian.getShieldPower();
            SynchedEntityData entityData = (SynchedEntityData) fieldEntityData.get(draconicGuardian);

            fieldAliveCrystal.set(fightManager, 0);
            entityData.set(SHIELD_POWER, 0f);

            entity.setInvulnerable(false);
            entity.setSecondsOnFire(60);
            entity.hurt(damageSource, 10_000_000_000_000_000f);

            fieldAliveCrystal.set(fightManager, oldCrystalNum);
            entityData.set(SHIELD_POWER, oldShieldPower);
            return true;

        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
            return false;
        }
    }

    public static void setAttributesToDefault(ServerPlayer player) {
        player.getAttribute(ForgeMod.ENTITY_GRAVITY.get()).setBaseValue(0.08);
        player.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(1.0D);
        player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1F);
        player.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(0);
        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4.0D);
    }

    public static void teleportPlayerToTarget(ServerPlayer sender) {
        Vec3 startVec = sender.getEyePosition();
        int distance = 300;
        Vec3 lookVec = sender.getLookAngle().scale(distance);
        Vec3 endVec = startVec.add(lookVec);
        Level level = sender.level();
        HitResult hitResult = level.clip(new ClipContext(startVec, endVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, sender));
        if (hitResult.getType() == HitResult.Type.MISS) {
            endVec = hitResult.getLocation();
        }
        AABB boundingBox = new AABB(startVec, endVec).inflate(1.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> entity != sender && entity.isAlive());
        LivingEntity closestEntity = null;
        double closestDistance = distance * distance;
        for (LivingEntity entity : entities) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
            EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(sender, startVec, endVec, entityBox, entity1 -> !entity1.isSpectator() && entity1.isPickable(), closestDistance);
            if (entityHitResult != null) {
                double distanceToEntity = startVec.distanceToSqr(entityHitResult.getLocation());
                if (distanceToEntity < closestDistance) {
                    closestEntity = entity;
                    closestDistance = distanceToEntity;
                }
            }
        }
        if (closestEntity != null)
            sender.teleportTo(closestEntity.getX(), closestEntity.getY(), closestEntity.getZ());
    }

    public static void applySaitamaEffectsSet(ServerPlayer player) {
        if (player.getEffect(MobEffects.NIGHT_VISION) == null) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 255));
        }
        if (player.getEffect(MobEffects.DIG_SPEED) == null) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, -1, 255));
        }
        if (player.getEffect(MobEffects.DOLPHINS_GRACE) == null) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, -1, 255));
        }
    }

    public static void removeSaitamaEffectsSet(ServerPlayer player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.DIG_SPEED);
        player.removeEffect(MobEffects.DOLPHINS_GRACE);
    }
}
