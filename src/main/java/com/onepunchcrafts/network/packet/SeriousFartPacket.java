package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.content.SaitamaContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
        if (player == null) {
            ctx.get().setPacketHandled(true);
            return;
        }
        boolean active = false;
        if (player != null && HelpUtility.hasPowerTag(player, SaitamaContent.TAG_SERIOUS_FART)) {
            player.serverLevel().explode(null, player.getX(), player.getY(), player.getZ(), 5,
                    Level.ExplosionInteraction.MOB);
            active = true;
        }
        final boolean[] legacyActive = {false};
        HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(cap -> {
            if (cap.isSeriousFartActive()) {
                player.serverLevel().explode(null, player.getX(), player.getY(), player.getZ(), 5,
                        Level.ExplosionInteraction.MOB);
                legacyActive[0] = true;
            }
        });
        if (active || legacyActive[0]) {
            Vec3 direction = player.getLookAngle();
            SaitamaTechniqueVfxPacket.broadcast(player.serverLevel(), new SaitamaTechniqueVfxPacket(
                    player.getId(), player.position().add(0, 0.75, 0), direction, 1.0f,
                    SaitamaTechniqueVfxPacket.SERIOUS_FART, 10));
        }
        ctx.get().setPacketHandled(true);
    }
}
