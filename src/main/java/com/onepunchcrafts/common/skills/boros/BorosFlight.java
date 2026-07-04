package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class BorosFlight implements Skill {
    private final BorosPack pack;

    public BorosFlight(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        boolean newState = !pack.isFlightActive();

        pack.setFlightActive(newState);

        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                newState ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 1.0f, 2.0f);

        player.sendSystemMessage(Component.literal(
                newState ? "§b§lPropulsão de Energia Ativada" : "§7Propulsão Desativada"
        ));

        if (player instanceof ServerPlayer serverPlayer) {
            HelpUtility.syncWithPlayer(serverPlayer, HelpUtility.getSkillData(serverPlayer));
        }

        return SkillExecutionResult.CONTINUE;
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        String status = pack.isFlightActive() ? "§b[ON]" : "§7[OFF]";
        guiGraphics.drawString(font, Component.literal("Propulsão " + status),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0x00FFFF, false);
    }
}
