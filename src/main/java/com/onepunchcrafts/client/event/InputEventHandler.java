package com.onepunchcrafts.client.event;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AdjustPowerIntentPacket;
import com.onepunchcrafts.v3.core.state.PowerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class InputEventHandler {

    @SubscribeEvent
    public static void playerScroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        double scrollDelta = event.getScrollDelta();
        if (player == null || scrollDelta == 0) return;
        if (!HelpUtility.getSkillData(player).getPowerState().powerSetId().equals(PowerState.NONE)) {
            NetworkRegister.sendToServer(new AdjustPowerIntentPacket(
                    HelpUtility.getSkillData(player).getPowerState().abilities().selectedAbility(), scrollDelta > 0 ? 1 : -1));
            event.setCanceled(true);
            return;
        }
        HelpUtility.getSkillData(player).adjustAbilityAndSyncWithServer(scrollDelta);
    }
}
