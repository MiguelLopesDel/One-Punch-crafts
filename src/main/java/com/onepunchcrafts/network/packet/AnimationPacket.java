package com.onepunchcrafts.network.packet;

import com.google.common.base.Charsets;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static com.onepunchcrafts.OnePunchCrafts.MODID;
import static net.minecraftforge.common.util.JsonUtils.readNBT;

public class AnimationPacket {

    private String idAnimation = "";

    public AnimationPacket(String idAnimation) {
        this.idAnimation = idAnimation;
    }

    public AnimationPacket(FriendlyByteBuf friendlyByteBuf) {
        this.idAnimation = friendlyByteBuf.readCharSequence(friendlyByteBuf.readShort(), StandardCharsets.UTF_8).toString();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeShort(idAnimation.length());
        buffer.writeCharSequence(idAnimation, StandardCharsets.UTF_8);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        LocalPlayer player = Minecraft.getInstance().player;
        ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(new ResourceLocation(MODID, "onecraftsanimation"));
        if (animation != null)
            animation.setAnimation(new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation(MODID, idAnimation))));
        ctx.get().setPacketHandled(true);
    }
}
