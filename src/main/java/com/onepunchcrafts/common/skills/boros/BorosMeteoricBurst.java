package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.ScreenEffectPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class BorosMeteoricBurst implements Skill {
    private final BorosPack pack;

    public BorosMeteoricBurst(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        if (pack.getConfig().isExhausted()) {
            player.sendSystemMessage(Component.literal("§c§lSem Energia Vital!"));
            return SkillExecutionResult.CONTINUE;
        }

        if (pack.getCurrentForm() != 2) {
            pack.setCurrentForm((short) 2);
            pack.setMeteoricBurstActive(true);
            pack.setFlightActive(true);
            player.sendSystemMessage(Component.literal("§c§l⚡ METEORIC BURST ATIVADO! ⚡"));
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(10.0f, 30, 0.7f));
                HelpUtility.syncWithPlayer(serverPlayer, HelpUtility.getSkillData(serverPlayer));
            }
        } else {
            pack.setCurrentForm((short) 1);
            pack.setMeteoricBurstActive(false);
            player.sendSystemMessage(Component.literal("§6§lMeteoric Burst desativado"));
            if (player instanceof ServerPlayer serverPlayer) {
                HelpUtility.syncWithPlayer(serverPlayer, HelpUtility.getSkillData(serverPlayer));
            }
        }

        return SkillExecutionResult.CONTINUE;
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        String status = pack.isMeteoricBurstActive() ? "§c[ATIVO]" : "";
        guiGraphics.drawString(font, Component.literal("Meteoric Burst " + status),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0xFF0000, false);
    }
}
