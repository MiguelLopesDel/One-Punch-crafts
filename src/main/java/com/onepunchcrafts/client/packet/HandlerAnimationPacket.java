package com.onepunchcrafts.client.packet;

import com.onepunchcrafts.util.HelpUtility;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

public class HandlerAnimationPacket {

    public static void handleClient(String uuid, String idAnimation) {
        Player playerByUUID = Minecraft.getInstance().player.level().getPlayerByUUID(UUID.fromString(uuid));
        if (playerByUUID == null) return;
        HelpUtility.getOneCraftAnimationLayer((AbstractClientPlayer) playerByUUID).ifPresent(animation -> {
            if (idAnimation.equals("stop")) {
                animation.setAnimation(null);
            }
            KeyframeAnimationPlayer animation1 = new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation(MODID, idAnimation))).setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL).setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, false, false));
            animation.setAnimation(animation1);
        });
    }
}
