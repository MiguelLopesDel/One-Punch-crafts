package com.onepunchcrafts.util;

import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.entity.GuardianCrystalEntity;
import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity;
import com.brandon3055.draconicevolution.entity.guardian.GuardianFightManager;
import com.brandon3055.draconicevolution.entity.guardian.control.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.Field;

import static com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity.SHIELD_POWER;

public class HelpUtilityMod {

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
            managerPhases(draconicGuardian);
            entity.hurt(damageSource, 10_000_000_000_000_000f);

            fieldAliveCrystal.set(fightManager, oldCrystalNum);
            entityData.set(SHIELD_POWER, oldShieldPower);
            return true;

        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
            return false;
        }
    }

    private static void managerPhases(DraconicGuardianEntity draconicGuardian) {
        PhaseManager manager = draconicGuardian.getPhaseManager();
        IPhase currentPhase = manager.getCurrentPhase();
        if (currentPhase == null)
            return;
        Field phase;
        try {
            phase = manager.getClass().getDeclaredField("phase");
            phase.setAccessible(true);
            if (currentPhase instanceof ArialBombardPhase)
                phase.set(manager, new ArialBombardPhase(draconicGuardian) {
                    @Override
                    public boolean isInvulnerable() {
                        return false;
                    }
                });
            else if (currentPhase instanceof GroundEffectPhase)
                phase.set(manager, new GroundEffectPhase(draconicGuardian) {
                    @Override
                    public boolean isInvulnerable() {
                        return false;
                    }
                });
            else if (currentPhase instanceof LaserBeamPhase)
                phase.set(manager, new LaserBeamPhase(draconicGuardian) {
                    @Override
                    public boolean isInvulnerable() {
                        return false;
                    }
                });
            else if (currentPhase instanceof ShockwavePhase)
                phase.set(manager, new ShockwavePhase(draconicGuardian) {
                    @Override
                    public boolean isInvulnerable() {
                        return false;
                    }
                });
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println();
        }
    }

    public static boolean attackGuardian(ServerPlayer player, LivingEntity entity, boolean isWeakPunch) {
        if (entity instanceof DraconicGuardianEntity guardian) {
            if (isWeakPunch) {
                guardian.hurt(player.damageSources().playerAttack(player), (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 100_000));
            } else {
                guardian.setShieldPower(0);
                guardian.hurt(player.damageSources().playerAttack(player), (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 10_000_000));
            }
            return true;
        }
        return false;
    }

    public static void attackCrystals(ServerPlayer player, ServerLevel serverLevel, AABB pArea, boolean isWeakPunch) {
        serverLevel.getEntitiesOfClass(GuardianCrystalEntity.class, pArea).forEach(crystal -> {
            if (isWeakPunch) {
                crystal.setInvulnerable(false);
                crystal.setUnstableTime(1);
                player.attack(crystal);
            } else {
                crystal.setInvulnerable(false);
                crystal.setShieldPower(0);
                player.attack(crystal);
            }
        });
    }

    static boolean noIsLivingEntityAndChaosCrystalEntity(Entity entity) {
        return !(entity instanceof LivingEntity) && !(entity instanceof GuardianCrystalEntity);
    }
}
