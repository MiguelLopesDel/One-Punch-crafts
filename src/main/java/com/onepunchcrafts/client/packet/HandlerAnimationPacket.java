package com.onepunchcrafts.client.packet;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

public class HandlerAnimationPacket {

    public static void handleClient(String uuid, String idAnimation) {
        Player playerByUUID = Minecraft.getInstance().player.level().getPlayerByUUID(UUID.fromString(uuid));
        if(playerByUUID==null) return;
        ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData((AbstractClientPlayer) playerByUUID).get(new ResourceLocation(MODID, "onecraftsanimation"));
        if (animation != null)
            animation.setAnimation(new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation(MODID, idAnimation))));
    }
}
