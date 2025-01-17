package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

public class SeriousPunchPacket {
    public SeriousPunchPacket(FriendlyByteBuf friendlyByteBuf) {
    }

    public SeriousPunchPacket() {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        sender.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            if (!(cap.getSkillPack() instanceof SaitamaPack) || cap.getActualAbility() != 2)
                return;
            Vec3 position = sender.position();
            Vec3 add = position.add(sender.getLookAngle().scale(5));
            Optional<Entity> entity = sender.serverLevel().getEntities(sender, new AABB(position, add)).stream().findFirst();
            entity.ifPresent(entity1 -> {
                if (entity1 instanceof WitherBoss witherBoss)
                    witherBoss.setInvulnerableTicks(0);
                entity1.setInvulnerable(false);
                sender.attack(entity1);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
    }
}
