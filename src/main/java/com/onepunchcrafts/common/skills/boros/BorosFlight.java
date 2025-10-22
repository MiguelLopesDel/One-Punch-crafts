package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.awt.*;

public class BorosFlight implements Skill {

    @Override
    public SkillExecutionResult execute(Player p) {
        if (p instanceof ServerPlayer player) {
            BorosPack borosPack = HelpUtility.getSkillData(player, BorosPack.class);

            if (borosPack.getCurrentForm() >= 1) {
                boolean newFlightState = !borosPack.isFlightActive();
                borosPack.setFlightActive(newFlightState);

                player.getAbilities().mayfly = newFlightState;
                player.getAbilities().flying = newFlightState;
                player.onUpdateAbilities();

                String message = newFlightState ? "§a§lFLIGHT ACTIVATED" : "§c§lFLIGHT DEACTIVATED";
                player.sendSystemMessage(Component.literal(message));
            } else {
                player.sendSystemMessage(Component.literal("§c§lNeed Released Form to fly!"));
            }
        }
        return null;
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.boros.flight"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, Color.CYAN.getRGB(), false);
    }
}