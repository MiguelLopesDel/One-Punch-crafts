package com.onepunchcrafts.util;

import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.entity.GuardianCrystalEntity;
import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity;
import com.brandon3055.draconicevolution.entity.guardian.GuardianFightManager;
import com.brandon3055.draconicevolution.entity.guardian.control.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLEnvironment;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.IPRegistry;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;

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

    //IMMERSIVE

    public static void placeByWayBiFacedPortal(ServerPlayer player, ResourceKey<Level> dimension) {
        Portal portal = new Portal(IPRegistry.PORTAL.get(), player.level());
        Vec3 frontPosition = getFrontPosition(player,2);
        portal.setOriginPos(frontPosition);
        portal.setDestinationDimension(dimension);
        portal.setDestination(frontPosition.add(frontPosition.x, 100 - frontPosition.y, frontPosition.z));
        portal.setOrientationAndSize(new Vec3(1, 0, 0), new Vec3(0, 1, 0),
                4, 4);
        McHelper.spawnServerEntity(portal);
        PortalManipulation.completeBiWayBiFacedPortal(portal, p -> {
        }, p -> {
        }, IPRegistry.PORTAL.get());
    }

    private static Vec3 getFrontPosition(ServerPlayer player, double distance, double yOffset) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(distance));
        if (yOffset != 0) {
            targetPos = new Vec3(targetPos.x, targetPos.y + yOffset, targetPos.z);
        }
        return targetPos;
    }

    private static Vec3 getFrontPosition(ServerPlayer player, double distance) {
        return getFrontPosition(player, distance, 0);
    }

    public static BlockPos getFrontBlockPosition(ServerPlayer player, double distance) {
        Vec3 frontPosition = getFrontPosition(player, distance);
        return new BlockPos((int) frontPosition.x, (int) frontPosition.y, (int) frontPosition.z);
    }
}
