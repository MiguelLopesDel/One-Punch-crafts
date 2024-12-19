package com.onepunchcrafts.client.packet;

import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HandlerPlayerSyncPacket {

    public static void clientLogic(SkillPack data) {
        LocalPlayer player = Minecraft.getInstance().player;
        HelpUtility.getSkillData(player).setSkillPack(data);
    }
}