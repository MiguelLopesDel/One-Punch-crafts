package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class SaitamaDash implements Skill {

    private static final double DASH_DISTANCE = 30.0;

    @Override
    public SkillExecutionResult execute(Player p) {
        if (p instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            performDash(player, level);
        }
        return null;
    }

    private void performDash(ServerPlayer player, ServerLevel level) {
        Vec3 startPos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = startPos.add(lookVec.scale(DASH_DISTANCE));

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0F, 1.0F);

//        AABB dashPathBox = new AABB(startPos, endPos).inflate(2.0);
//        level.getEntitiesOfClass(LivingEntity.class, dashPathBox, entity -> !entity.is(player)).forEach(target -> {
//            player.attack(target);
//            knockback(target, 4.0, lookVec.x, lookVec.z);
//        });

        BlockHitResult hitResult = level.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 finalTeleportPos = hitResult.getLocation().subtract(lookVec.scale(1));

        player.teleportTo(finalTeleportPos.x, finalTeleportPos.y - player.getEyeHeight(), finalTeleportPos.z);

        level.playSound(null, finalTeleportPos.x, finalTeleportPos.y, finalTeleportPos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.5F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, finalTeleportPos.x, finalTeleportPos.y, finalTeleportPos.z, 1, 0, 0, 0, 0);
    }

    private static void knockback(LivingEntity target, double strength, double pX, double pZ) {
        if (!(strength <= 0.0D)) {
            target.hasImpulse = true;
            Vec3 vec3 = target.getDeltaMovement();
            Vec3 vec31 = (new Vec3(pX, 0.0D, pZ)).normalize().scale(strength);
            double verticalStrength = Math.min(0.6D, strength * 0.2D);
            target.setDeltaMovement(vec3.x / 2.0D - vec31.x, target.onGround() ? verticalStrength : vec3.y + verticalStrength, vec3.z / 2.0D - vec31.z);
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.saitama.dash"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
    }
}