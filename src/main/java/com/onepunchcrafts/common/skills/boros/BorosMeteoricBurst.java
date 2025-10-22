package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.awt.*;

public class BorosMeteoricBurst implements Skill {

    @Override
    public SkillExecutionResult execute(Player p) {
        if (p instanceof ServerPlayer player) {
            BorosPack borosPack = HelpUtility.getSkillData(player, BorosPack.class);

            if (borosPack.getCurrentForm() < 2) {
                borosPack.setCurrentForm((short) 2);
                borosPack.setMeteoricBurstActive(true);
                borosPack.setFlightActive(true);

                ServerLevel level = (ServerLevel) player.level();
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 0.8F);

                for (int i = 0; i < 50; i++) {
                    double x = player.getX() + (Math.random() - 0.5) * 10;
                    double y = player.getY() + Math.random() * 5;
                    double z = player.getZ() + (Math.random() - 0.5) * 10;
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                            x, y, z, 1, 0, 0, 0, 0.3);
                }

                borosPack.setSpeed((short) 50);
                borosPack.setAttackKnockback((short) 20);

                player.sendSystemMessage(Component.literal("§c§l[METEORIC BURST ACTIVATED!]"));

            } else {
                borosPack.setCurrentForm((short) 0);
                borosPack.setMeteoricBurstActive(false);
                borosPack.setFlightActive(false);
                resetStats(borosPack);
            }
        }
        return null;
    }

    private void resetStats(BorosPack pack) {
        pack.setSpeed((short) 10);
        pack.setAttackKnockback((short) 5);
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.boros.meteoric_burst"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, Color.RED.getRGB(), false);
    }
}