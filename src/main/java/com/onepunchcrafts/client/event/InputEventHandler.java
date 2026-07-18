package com.onepunchcrafts.client.event;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.client.gui.TechniqueWheelScreen;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AdjustTechniqueIntentPacket;
import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.runtime.state.PowerState;
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
        if (Minecraft.getInstance().screen instanceof TechniqueWheelScreen) return;
        if (!HelpUtility.getSkillData(player).getPowerState().powerSetId().equals(PowerState.NONE)) {
            var state = HelpUtility.getSkillData(player).getPowerState();
            Technique technique = OnePunchRuntime.REGISTRIES.techniques.require(
                    state.abilities().selectedTechnique());
            if (technique.activeAction() instanceof Technique.ActiveAction.Adjust) {
                NetworkRegister.sendToServer(new AdjustTechniqueIntentPacket(
                        state.abilities().selectedTechnique(), scrollDelta > 0 ? 1 : -1));
                event.setCanceled(true);
            }
            return;
        }
        HelpUtility.getSkillData(player).adjustAbilityAndSyncWithServer(scrollDelta);
    }
}
