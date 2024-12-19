package com.onepunchcrafts.client.event;

import com.onepunchcrafts.client.Keybinding;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.SeriousFartPacket;
import com.onepunchcrafts.network.packet.SpecialSkillPacket;
import com.onepunchcrafts.network.packet.TeleportPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientTickEventHandler {

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean playerExist = player != null;
        if (Keybinding.INSTANCE.CHANGE_SKILL.consumeClick() && playerExist) {
            onKeyChangePressed();
        }
        if (Keybinding.INSTANCE.USE_SPECIAL_SKILL.consumeClick() && playerExist) {
            onKeySpecialSkillPressed();
        }
        if (Keybinding.INSTANCE.USE_FART.isDown() && playerExist) {
            NetworkRegister.sendToServer(new SeriousFartPacket());
        }
        if (Keybinding.INSTANCE.USE_TELEPORT.consumeClick() && playerExist) {
            NetworkRegister.sendToServer(new TeleportPacket());
        }
    }

    private static void onKeySpecialSkillPressed() {
        NetworkRegister.sendToServer(new SpecialSkillPacket());
    }

    private static void onKeyChangePressed() {
        LocalPlayer player = Minecraft.getInstance().player;
        OnePunchPlayer data = HelpUtility.getSkillData(player);
        data.decideCurrentSkill(player);
        HelpUtility.syncDataWithServer(data);
    }
}
