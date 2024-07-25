package com.onepunchcrafts.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.Charset;
import java.util.function.Supplier;

public class SettingRenderPacket {

    private String id;

    public SettingRenderPacket(String id) {
        this.id = id;
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeCharSequence(this.id, Charset.defaultCharset());
    }

    public SettingRenderPacket(FriendlyByteBuf friendlyByteBuf) {
        this.id = friendlyByteBuf.readCharSequence(friendlyByteBuf.readShort(), Charset.defaultCharset()).toString();
    }


    public void handle(Supplier<NetworkEvent.Context> ctx) {
//        LocalPlayer player = Minecraft.getInstance().player;
//        if(player!=null)
//            Minecraft.getInstance().//renderTextureOverlay(guiGraphics, PUMPKIN_BLUR_LOCATION, 1.0F);
//        ctx.
    }
}
