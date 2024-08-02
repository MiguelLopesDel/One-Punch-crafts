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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.LazyOptional;

import java.lang.reflect.Field;
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

    public static int allowDamageInDraconicGuardian(LivingEntity entity) {
        if (!(entity instanceof DraconicGuardianEntity draconicGuardian))
            return -1;
        GuardianFightManager fightManager = draconicGuardian.getFightManager();
        if (fightManager == null)
            return -1;
        try {
            Field fieldAliveCrystal = fightManager.getClass().getDeclaredField("aliveCrystals");
            Field fieldEntityData = Class.forName("net.minecraft.world.entity.Entity").getDeclaredField("entityData");
            fieldAliveCrystal.setAccessible(true);
            fieldEntityData.setAccessible(true);

            int aliveCrystal = (int) fieldAliveCrystal.get(fightManager);
            int oldValue = aliveCrystal;

            fieldAliveCrystal.set(fightManager, 0);
            ((SynchedEntityData) fieldEntityData.get(draconicGuardian)).set(SHIELD_POWER, 0f);

            return oldValue;
        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
        }
        return -1;
    }

    public static void setCrystalToDefault(LivingEntity entity, int crystalNum) {
        DraconicGuardianEntity draconicGuardian = (DraconicGuardianEntity) entity;
        GuardianFightManager fightManager = draconicGuardian.getFightManager();
        if (fightManager == null)
            return;
        try {
            Field fieldAliveCrystal = fightManager.getClass().getDeclaredField("aliveCrystals");
            fieldAliveCrystal.setAccessible(true);
            fieldAliveCrystal.set(fightManager, crystalNum);
        } catch (NoSuchFieldException | IllegalAccessException e) {
        }
    }
}
