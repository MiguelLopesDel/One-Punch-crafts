package com.onepunchcrafts.util;

import com.onepunchcrafts.common.RegisterSounds;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AnimationPacket;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static com.onepunchcrafts.OnePunchCrafts.*;
import static net.minecraft.world.level.GameRules.RULE_MOB_EXPLOSION_DROP_DECAY;

public class HelpUtility {

    /**
     * Verifica se o jogador tem a capacidade do mod e se ele é saitama.
     *
     * @param player
     * @return caso o jogador tenha a capacidade e seja um saitama é retorna a propria capacidade caso contrario
     * um optional empty;
     */
    public static Optional<SaitamaPack> verifyIsSaitamaAndGetCapability(Player player) {
        LazyOptional<OnePunchPlayer> capability = player.getCapability(ONE_PLAYER_CAPABILITY);
        return capability.resolve()
                .map(OnePunchPlayer::getSkillPack)
                .filter(SaitamaPack.class::isInstance)
                .map(SaitamaPack.class::cast);
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
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, boundingBox, entity -> entity != sender && entity.isAlive());
        Entity closestEntity = null;
        double closestDistance = distance * distance;
        for (Entity entity : entities) {
            if (DRACONIC_MOD.isPresent() && HelpUtilityMod.noIsLivingEntityAndChaosCrystalEntity(entity))
                continue;
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

    public static @NotNull OnePunchPlayer getSkillData(Player player) {
        return player.getCapability(ONE_PLAYER_CAPABILITY).orElse(new OnePunchPlayer(WITHOUT_PACK));
    }

    public static void syncDataWithServer(OnePunchPlayer data) {
        NetworkRegister.sendToServer(new PlayerSyncPacket(data.getSkillPack()));
    }

    public static void syncWithPlayer(ServerPlayer player, OnePunchPlayer cap) {
        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(cap.getSkillPack()));
    }

    public static @NotNull OnePunchPlayer getSkillDataOr(Player player, SkillPack skillPack) {
        return player.getCapability(ONE_PLAYER_CAPABILITY).orElse(new OnePunchPlayer(skillPack));
    }

    public static Optional<OnePunchPlayer> getSaitamaPack(Player player) {
        return player.getCapability(ONE_PLAYER_CAPABILITY).filter(cap -> cap.getSkillPack() instanceof SaitamaPack);
    }

    public static BiOptional<OnePunchPlayer, SaitamaPack> getCapAndSaitamaSkillData(Player player) {
        return player.getCapability(ONE_PLAYER_CAPABILITY).filter(cap -> cap.getSkillPack() instanceof SaitamaPack)
                .map(cap -> BiOptional.of(cap, (SaitamaPack) cap.getSkillPack())).orElse(BiOptional.empty());
    }

    @OnlyIn(Dist.CLIENT)
    public static Optional<ModifierLayer<IAnimation>> getOneCraftAnimationLayer(AbstractClientPlayer playerByUUID) {
        return Optional.ofNullable((ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(playerByUUID).get(new ResourceLocation(MODID, "onecraftsanimation")));
    }

    public static boolean extremeSpeedActivated(Player player) {
        Optional<OnePunchPlayer> saitamaPack = getSaitamaPack(player);
        if (saitamaPack.isPresent() && saitamaPack.get().getSkillPack() instanceof SaitamaPack sai) {
            return sai.isExtremeSpeedActive();
        }
        return false;
    }

    public static boolean canWalk(Player player, FluidState fluid) {
        if (!extremeSpeedActivated(player))
            return false;
        if (fluid.is(FluidTags.WATER)) {
            return true;
        }
        return fluid.is(FluidTags.LAVA);
    }

    public static void passServerFluxToAllPlayers(LivingEvent ev) {
        if (ev.getEntity() instanceof ServerPlayer player)
            HelpUtility.getSkillData(player).manageFlux(ev);

        if (ev instanceof LivingDeathEvent event && event.getSource().getEntity() instanceof ServerPlayer player) {
            HelpUtility.getSkillData(player).manageFlux(ev);
        } else if (ev instanceof LivingDamageEvent event && event.getSource().getEntity() instanceof ServerPlayer player) {
            HelpUtility.getSkillData(player).manageFlux(ev);
        } else if (ev instanceof LivingHurtEvent event && event.getSource().getEntity() instanceof ServerPlayer player) {
            HelpUtility.getSkillData(player).manageFlux(ev);
        }
    }

    public static boolean isServer(Entity entity) {
        return entity != null && !entity.level().isClientSide();
    }

    public static void clientEffects(ServerPlayer player) {
        player.serverLevel().playSound(null, player.getOnPos(), RegisterSounds.SERIOUS_PUNCH.get(), SoundSource.PLAYERS, 1, 1);
        NetworkRegister.sendToAllClients(new AnimationPacket(player.getStringUUID(), "punch_animation"));
    }

    public static boolean isSaitamaServerSide(Entity entity) {
        return entity instanceof ServerPlayer player && getSkillData(player).getSkillPack() instanceof SaitamaPack;
    }

//    public static Optional<SaitamaPack> isSaitamaServerSide(Entity entity) {
//        return entity instanceof ServerPlayer player ? verifyIsSaitamaAndGetCapability(player) : Optional.empty();
//    }

    public static Explosion explodeWithoutKnockBackFor(@NotNull Entity entity, double x1, double v, double z1, float v1) {
        Level level = entity.level();
        Explosion.BlockInteraction explosion$blockinteraction1 = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, null) ?
                level.getGameRules().getBoolean(RULE_MOB_EXPLOSION_DROP_DECAY) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY
                : Explosion.BlockInteraction.KEEP;
        Explosion.BlockInteraction explosion$blockinteraction = explosion$blockinteraction1;
        ExplosionWithoutKnockBack explosion = new ExplosionWithoutKnockBack(level, null, null, null, x1, v, z1, v1, false, explosion$blockinteraction);
        if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(level, explosion))
            return explosion;
        explosion.addEntityWithoutKnockBack(entity);
        explosion.explode();
        explosion.finalizeExplosion(true);
        return explosion;
    }
}
