package com.onepunchcrafts.client.packet;

import com.onepunchcrafts.client.gui.GuiDimension;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HandlerClientPacket {

    public static void playerSyncLogic(SkillPack data) {
        LocalPlayer player = Minecraft.getInstance().player;
        HelpUtility.getSkillData(player).setSkillPack(data);
    }

    public static void dimensionPacketResponse(List<ResourceKey<Level>> dimensions) {
        if(!(Minecraft.getInstance().screen instanceof GuiDimension dimension))
            return;
        GuiDimension.setDimensions(dimensions);
    }
}