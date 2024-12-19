package com.onepunchcrafts.client.event;

import com.onepunchcrafts.common.skills.SkillPack;
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
        player.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            renderSaitamaOptions(cap.getSkillPack(), event, instance, guiGraphics);
        });
    }

    private static void renderSaitamaOptions(SkillPack skillPack, RenderGuiEvent.Pre event, Minecraft instance, GuiGraphics guiGraphics) {
        final int width = event.getWindow().getGuiScaledWidth();
        final int height = event.getWindow().getGuiScaledHeight();
        Font font = instance.font;
        skillPack.renderSkills(width, height, font, guiGraphics);
    }
}
