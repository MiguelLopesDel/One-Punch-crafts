package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.event.LivingDamageEventHandler;
import com.onepunchcrafts.network.NetworkRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class SpecialSkillPacket {
    public SpecialSkillPacket() {
    }

    public SpecialSkillPacket(FriendlyByteBuf friendlyByteBuf) {

    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {

    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            context.setPacketHandled(true);
        } else {
            sender.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
                if (cap.isSaitama()) {
                    switch (cap.getActualAbility()) {
                        case 2:
                            LivingDamageEventHandler.seriousPunchWithoutSpecificTargetWithClientEffects(sender, sender.serverLevel());
                            break;
                        case 3:
                            cap.setSeriousFartActive(!cap.isSeriousFartActive());
                            NetworkRegister.sendToPlayer(sender, new PlayerSyncPacket(cap));
                            break;
                        case 5:
                            quickBackstab(cap, sender);
                            break;
                        case 6:
                            cap.setSuperSpeed(!cap.isSuperSpeed());
                            NetworkRegister.sendToPlayer(sender, new PlayerSyncPacket(cap));
                            break;
                        case 7:
                            cap.setBreakBlocksQuickly(!cap.isBreakBlocksQuickly());
                            NetworkRegister.sendToPlayer(sender, new PlayerSyncPacket(cap));
                            break;
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    private static void quickBackstab(OnePunchPlayer cap, ServerPlayer sender) {
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
            cap.setActualAbility(1);
            sender.attack(closestEntity);
            cap.setActualAbility(5);
//            if (closestEntity instanceof ServerPlayer target)
//                NetworkRegister.sendToPlayer(target, new SettingRenderPacket("quick_backstab"));
        }
    }
}
