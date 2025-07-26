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
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.List;
import java.util.function.Supplier;

public class SpecialSkillPacket {
    public SpecialSkillPacket() {
    }

    public SpecialSkillPacket(FriendlyByteBuf friendlyByteBuf) {

    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {

    }

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            sender.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> cap.getSkillPack().execute(sender));
        }
        context.setPacketHandled(true);
    }
}
