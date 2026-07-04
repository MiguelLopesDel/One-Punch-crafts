package com.onepunchcrafts.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScreenEffectPacket {
    private final float intensity;
    private final int duration;
    private final float fovMultiplier;

    public ScreenEffectPacket(float intensity, int duration, float fovMultiplier) {
        this.intensity = intensity;
        this.duration = duration;
        this.fovMultiplier = fovMultiplier;
    }

    public ScreenEffectPacket(FriendlyByteBuf buf) {
        this.intensity = buf.readFloat();
        this.duration = buf.readInt();
        this.fovMultiplier = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(intensity);
        buf.writeInt(duration);
        buf.writeFloat(fovMultiplier);
    }

    public static void handle(ScreenEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.onepunchcrafts.client.event.ScreenEffectHandler.addEffect(msg.intensity, msg.duration, msg.fovMultiplier);
        });
        ctx.get().setPacketHandled(true);
    }
}
