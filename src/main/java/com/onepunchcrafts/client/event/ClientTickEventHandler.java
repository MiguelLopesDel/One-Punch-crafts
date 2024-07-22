package com.onepunchcrafts.client.event;

import com.onepunchcrafts.client.Keybinding;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.network.packet.SpecialSkillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;


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
    }

    private static void onKeySpecialSkillPressed() {
        NetworkRegister.sendToServer(new SpecialSkillPacket());
    }

    private static void onKeyChangePressed() {
        LocalPlayer player = Minecraft.getInstance().player;
        OnePunchPlayer data = player.getCapability(ONE_PLAYER_CAPABILITY, null).orElse(new OnePunchPlayer(false));
        data.setActualAbility(data.getActualAbility() + (player.isShiftKeyDown() ? -1 : 1));
        NetworkRegister.sendToServer(new PlayerSyncPacket(data));
    }
}
