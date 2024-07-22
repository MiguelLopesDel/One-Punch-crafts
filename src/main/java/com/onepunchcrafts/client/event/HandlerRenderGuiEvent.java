package com.onepunchcrafts.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class HandlerRenderGuiEvent {

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft instance = Minecraft.getInstance();
        LocalPlayer player = instance.player;
        player.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, "isSai" + cap.isSaitama(), 6, 71, -205, false);
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, "NUmero" + cap.getActualAbility(), 6, 90, -205, false);
        });
    }
}
