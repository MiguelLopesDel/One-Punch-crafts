package com.onepunchcrafts.client.event;

import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.common.skills.boros.BorosConfig;
import com.onepunchcrafts.common.skills.boros.BorosPack;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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
        if (player == null) return;

        final int width = event.getWindow().getGuiScaledWidth();
        final int height = event.getWindow().getGuiScaledHeight();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = instance.font;

        SkillPack pack = HelpUtility.getSkillData(player).getSkillPack();
        pack.renderSkills(width, height, font, guiGraphics);

        if (pack instanceof BorosPack boros) {
            renderBorosEnergy(boros, width, height, font, guiGraphics);
        }
    }

    private static void renderBorosEnergy(BorosPack boros, int width, int height, Font font, GuiGraphics guiGraphics) {
        float energy = boros.getEnergy();
        float percentage = (energy / BorosConfig.MAX_ENERGY) * 100f;
        int color = percentage > 50 ? 0x00FF00
                : percentage > 20 ? 0xFFFF00
                : 0xFF0000;

        String[] formNames = {"Armadura", "Liberado", "Meteoric Burst"};
        String status = boros.getConfig().isExhausted() ? " §c§l[EXAUSTO]" : "";

        String text = String.format("Energia: %.1f%% | Forma: %s%s", percentage, formNames[boros.getCurrentForm()], status);

        int textWidth = font.width(text);
        int x = (width - textWidth) / 2;
        int y = 5;

        guiGraphics.fill(x - 2, y - 1, x + textWidth + 2, y + 10, 0x88000000);
        guiGraphics.drawString(font, Component.literal(text), x, y, color, false);
    }
}
