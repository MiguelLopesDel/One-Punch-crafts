package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.SkillPassive;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.awt.*;
import java.math.BigInteger;
import java.time.Duration;

public class BorosRegeneration implements SkillPassive {

    @Override
    public void tick(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.heal(100_000);
        }
    }

    @Override
    public void execute(Player player) {
        if (player instanceof ServerPlayer serverPlayer && HelpUtility.getSkillData(serverPlayer, BorosPack.class).getTicksToUseUltraRegeneration() == 0) {
            TickScheduler.scheduleDuringAndWithInterval(Duration.ofSeconds(20), Duration.ofMillis(50), () -> serverPlayer.heal(15_000_000));
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("Regeneração"), width / 2 - defaultReduce, height / 2 + defaultAdd, true ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
    }
}
