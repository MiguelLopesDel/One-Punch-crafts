package com.onepunchcrafts.client.event;

import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class RenderGuiEventHandler {

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Pre event) {
        Minecraft instance = Minecraft.getInstance();
        LocalPlayer player = instance.player;
        final int width = event.getWindow().getGuiScaledWidth();
        final int height = event.getWindow().getGuiScaledHeight();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = instance.font;
        HelpUtility.getSkillData(player).getSkillPack().renderSkills(width, height, font, guiGraphics);
    }
}
