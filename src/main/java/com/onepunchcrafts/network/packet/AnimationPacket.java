package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.packet.HandlerAnimationPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class AnimationPacket {

    private String idAnimation = "";
    private String playerExecuteAnimation = "";

    public AnimationPacket(String stringUUID, String punchAnimation) {
        this.idAnimation = punchAnimation;
        this.playerExecuteAnimation = stringUUID;
    }

    public AnimationPacket(FriendlyByteBuf friendlyByteBuf) {
        this.idAnimation = friendlyByteBuf.readCharSequence(friendlyByteBuf.readShort(), StandardCharsets.UTF_8).toString();
        this.playerExecuteAnimation = friendlyByteBuf.readCharSequence(friendlyByteBuf.readShort(), StandardCharsets.UTF_8).toString();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeShort(idAnimation.length());
        buffer.writeCharSequence(idAnimation, StandardCharsets.UTF_8);
        buffer.writeShort(playerExecuteAnimation.length());
        buffer.writeCharSequence(playerExecuteAnimation, StandardCharsets.UTF_8);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HandlerAnimationPacket.handleClient(playerExecuteAnimation, idAnimation));
        ctx.setPacketHandled(true);
    }
}
