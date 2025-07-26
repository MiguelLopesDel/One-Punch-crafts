package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.packet.HandlerClientPacket;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DimensionsPacket {

    private List<ResourceKey<Level>> dimensions = new ArrayList<>();

    public DimensionsPacket() {
    }

    public DimensionsPacket(List<ResourceKey<Level>> dimensions) {
        this.dimensions = dimensions;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(dimensions.size());
        for (ResourceKey<Level> dimension : dimensions) {
            buf.writeResourceKey(dimension);
        }
    }

    public DimensionsPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            dimensions.add(buf.readResourceKey(Registries.DIMENSION));
        }
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (context.isServerSide()) {
            ServerPlayer sender = context.getSender();
            HelpUtility.verifyIsSaitamaAndGetCapability(sender).ifPresent(cap -> {
                dimensions.addAll(sender.level().getServer().levelKeys());
                NetworkRegister.sendToPlayer(sender, new DimensionsPacket(dimensions));
            });
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HandlerClientPacket.dimensionPacketResponse(dimensions));
        }
        context.setPacketHandled(true);
    }
}
