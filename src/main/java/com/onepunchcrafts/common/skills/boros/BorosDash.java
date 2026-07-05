package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.ScreenEffectPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BorosDash implements Skill {
    private final BorosPack pack;

    public BorosDash(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        if (pack.getConfig().isExhausted()) {
            player.sendSystemMessage(Component.translatable("skill.boros.no_energy"));
            return SkillExecutionResult.CONTINUE;
        }

        if (!pack.canUseBurstStep()) {
            player.sendSystemMessage(Component.translatable("skill.boros.dash.cooldown"));
            return SkillExecutionResult.CONTINUE;
        }

        if (!pack.consumeEnergy(BorosConfig.ENERGY_BLAST_COST * 1.25f)) {
            player.sendSystemMessage(Component.translatable("skill.boros.insufficient_energy"));
            return SkillExecutionResult.CONTINUE;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            performDash(serverPlayer);
            NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(0.9f + pack.getCurrentForm(), 6, 1.18f));
        }

        return SkillExecutionResult.CONTINUE;
    }

    private void performDash(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 lookVec = player.getLookAngle();
        
        // Tenta encontrar um alvo para circundar
        LivingEntity target = findTarget(player);
        
        double speed = switch (pack.getCurrentForm()) {
            case 0 -> 3.4;
            case 1 -> 6.2;
            case 2 -> 10.5;
            default -> 3.4;
        };

        if (target != null) {
            Vec3 targetPos = target.position();
            Vec3 toTarget = targetPos.subtract(player.position()).normalize();
            Vec3 side = new Vec3(-toTarget.z, 0, toTarget.x).scale(player.getRandom().nextBoolean() ? 0.9 : -0.9);
            Vec3 burst = toTarget.scale(speed).add(side).add(0, 0.3 + pack.getCurrentForm() * 0.15, 0);
            player.setDeltaMovement(burst);
        } else {
            Vec3 horizontalLook = lookVec.multiply(1, 0, 1);
            if (horizontalLook.lengthSqr() < 0.001) horizontalLook = lookVec;
            // Follows the aim more, so diagonal/aerial dashes actually go there.
            Vec3 burst = horizontalLook.normalize().scale(speed).add(0, lookVec.y * speed * 0.5, 0);
            player.setDeltaMovement(burst);
        }

        player.fallDistance = 0;
        player.hurtMarked = true;
        pack.markBurstStepUsed(pack.getCurrentForm() == 2 ? 3 : 6);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.7f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 30, 0.6, 0.8, 0.6, 0.65);
        level.sendParticles(pack.getCurrentForm() == 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.DRAGON_BREATH,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.4, 0.6, 0.4, 0.25);
    }

    private LivingEntity findTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(30.0));
        AABB box = player.getBoundingBox().inflate(30.0);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
        
        LivingEntity bestTarget = null;
        double minDistance = Double.MAX_VALUE;
        
        for (LivingEntity e : entities) {
            AABB hitbox = e.getBoundingBox().inflate(1.0);
            if (hitbox.clip(start, end).isPresent()) {
                double dist = player.distanceToSqr(e);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestTarget = e;
                }
            }
        }
        return bestTarget;
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.boros.dash"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0x00FFFF, false);
    }
}
