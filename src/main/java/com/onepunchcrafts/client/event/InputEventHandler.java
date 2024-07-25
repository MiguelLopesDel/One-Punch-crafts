package com.onepunchcrafts.client.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.SeriousPunchPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class InputEventHandler {

    @SubscribeEvent
    public static void playerAttack(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !event.isAttack()) return;
        player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            if (cap.isSaitama() && cap.getActualAbility() == 2)
                NetworkRegister.sendToServer(new SeriousPunchPacket());
        });
    }
}
