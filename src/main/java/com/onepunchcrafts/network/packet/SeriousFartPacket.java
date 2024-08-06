package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SeriousFartPacket {


    public SeriousFartPacket() {
    }

    public SeriousFartPacket(FriendlyByteBuf friendlyByteBuf) {

    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {

    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(cap -> {
            if (cap.isSeriousFartActive())
                player.serverLevel().explode(null, player.getX(), player.getY(), player.getZ(), 5,
                        Level.ExplosionInteraction.MOB);
        });
        ctx.get().setPacketHandled(true);
    }
}
