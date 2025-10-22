package com.onepunchcrafts.common.skills;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingEvent;

public interface Skill {

    SkillExecutionResult execute(Player player);

    default void flux(LivingEvent event) {}

    @OnlyIn(Dist.CLIENT)
    void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd);
}
