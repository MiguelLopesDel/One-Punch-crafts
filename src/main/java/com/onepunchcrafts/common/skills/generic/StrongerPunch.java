package com.onepunchcrafts.common.skills.generic;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.awt.*;

public class StrongerPunch implements Skill {

    @Override
    public SkillExecutionResult execute(Player player) {

        return null;
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.saitama.weak_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
    }
}
