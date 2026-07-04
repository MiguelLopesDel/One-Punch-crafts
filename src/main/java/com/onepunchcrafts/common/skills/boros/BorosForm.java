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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class BorosForm implements Skill {
    private final BorosPack pack;

    public BorosForm(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        short currentForm = pack.getCurrentForm();
        short nextForm = currentForm == 0 ? (short) 1 : (short) 0;

        if (currentForm == 2) {
            nextForm = 1;
            pack.setMeteoricBurstActive(false);
        }

        String[] formNames = {"Armadura", "Liberado", "Meteoric Burst"};
        pack.setCurrentForm(nextForm);

        player.sendSystemMessage(Component.literal(
                String.format("§6§lForma: %s", formNames[nextForm])
        ));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, nextForm == 1 ? 1.0f : 0.55f);

        if (player instanceof ServerPlayer serverPlayer) {
            HelpUtility.syncWithPlayer(serverPlayer, HelpUtility.getSkillData(serverPlayer));
            NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(nextForm == 1 ? 4.0f : 1.5f, 18, nextForm == 1 ? 0.92f : 1.0f));
        }
        
        return SkillExecutionResult.CONTINUE;
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        String[] formNames = {"Armadura", "Liberado", "Meteoric Burst"};
        String currentFormName = formNames[pack.getCurrentForm()];

        guiGraphics.drawString(font, Component.literal("Trocar Forma (" + currentFormName + ")"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0xFFAA00, false);
    }
}
