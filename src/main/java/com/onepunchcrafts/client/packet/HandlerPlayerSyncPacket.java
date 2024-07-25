package com.onepunchcrafts.client.packet;

import com.onepunchcrafts.common.capability.OnePunchPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

@OnlyIn(Dist.CLIENT)
public class HandlerPlayerSyncPacket {

    public static void clientLogic(OnePunchPlayer data) {
        LocalPlayer player = Minecraft.getInstance().player;
        player.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            cap.setSaitama(data.isSaitama());
            cap.setActualAbility(data.getActualAbility());
            cap.setSeriousFartActive(data.isSeriousFartActive());
            cap.setSuperSpeed(data.isSuperSpeed());
            cap.setBreakBlocksQuickly(data.isBreakBlocksQuickly());
        });
    }
}
