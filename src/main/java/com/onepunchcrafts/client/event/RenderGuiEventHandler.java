package com.onepunchcrafts.client.event;

import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.common.skills.boros.BorosConfig;
import com.onepunchcrafts.common.skills.boros.BorosPack;
import com.onepunchcrafts.client.power.TechniquePresentation;
import com.onepunchcrafts.client.gui.TechniqueIconRenderer;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.runtime.state.PowerState;
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

        PowerState state = HelpUtility.getSkillData(player).getPowerState();
        if (!state.powerSetId().equals(PowerState.NONE)) {
            renderTechnique(state, width, height, font, guiGraphics);
            return;
        }
        SkillPack pack = HelpUtility.getSkillData(player).getSkillPack();
        pack.renderSkills(width, height, font, guiGraphics);

        if (pack instanceof BorosPack boros) {
            renderBorosEnergy(boros, width, height, font, guiGraphics);
        }
    }

    private static void renderTechnique(PowerState state, int width, int height, Font font, GuiGraphics graphics) {
        Id selected = state.abilities().selectedTechnique();
        if (selected == null) return;
        Technique technique;
        try { technique = OnePunchRuntime.REGISTRIES.techniques.require(selected); }
        catch (IllegalArgumentException ignored) { return; }

        Component name = TechniquePresentation.name(technique, state);
        var primary = TechniquePresentation.primary(technique);
        var active = TechniquePresentation.active(technique);
        int lines = 1 + (primary.isPresent() ? 1 : 0) + (active.isPresent() ? 1 : 0);
        int textWidth = font.width(name);
        if (primary.isPresent()) textWidth = Math.max(textWidth, font.width(primary.orElseThrow()));
        if (active.isPresent()) textWidth = Math.max(textWidth, font.width(active.orElseThrow()));

        int iconBox = 32;
        int contentWidth = iconBox + 6 + textWidth;
        int contentHeight = Math.max(iconBox, lines * (font.lineHeight + 1));
        int x = width / 2 - contentWidth / 2;
        int y = height / 2 + (int) (height * 0.21);

        Id icon = technique.presentation().icon();
        int frame = TechniquePresentation.disabledToggle(technique, state) ? 0xFFB94B4B : 0xFFF1B75E;
        graphics.fill(x - 1, y - 1, x + iconBox + 1, y + iconBox + 1, 0xD0000000);
        graphics.fill(x, y, x + iconBox, y + iconBox, frame);
        TechniqueIconRenderer.draw(graphics, icon, x + 2, y + 2, iconBox - 4);
        int textX = x + iconBox + 6;
        int lineY = y + (contentHeight - lines * (font.lineHeight + 1)) / 2;
        graphics.drawString(font, name, textX, lineY, 0xFFFFD27A, true);
        lineY += font.lineHeight + 1;
        if (primary.isPresent()) {
            graphics.drawString(font, primary.orElseThrow(), textX, lineY, 0xFFFFFFFF, true);
            lineY += font.lineHeight + 1;
        }
        if (active.isPresent()) graphics.drawString(font, active.orElseThrow(), textX, lineY, 0xFFD8D8D8, true);
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
