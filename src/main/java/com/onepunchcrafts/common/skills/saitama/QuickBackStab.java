package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;

public class QuickBackStab implements Skill {

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel) || !(player instanceof ServerPlayer sender))
            return;
        SaitamaPack saitamaPack = HelpUtility.verifyIsSaitamaAndGetCapability(sender).get();
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
        if (closestEntity != null) {
            sender.teleportTo(closestEntity.getX(), closestEntity.getY(), closestEntity.getZ());
            saitamaPack.setCurrentSkill(1);
            sender.attack(closestEntity);
            saitamaPack.setCurrentSkill(5);
            HelpUtility.syncWithPlayer(sender, HelpUtility.getSkillData(sender));
//            if (closestEntity instanceof ServerPlayer target)
//                NetworkRegister.sendToPlayer(target, new SettingRenderPacket("quick_backstab"));
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.saitama.quick_backstab"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
    }
}
