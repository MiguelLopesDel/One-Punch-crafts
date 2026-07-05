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

        pack.setCurrentForm(nextForm);

        player.sendSystemMessage(Component.translatable("skill.boros.form.set",
                Component.translatable(FORM_KEYS[nextForm])));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, nextForm == 1 ? 1.0f : 0.55f);

        if (player instanceof ServerPlayer serverPlayer) {
            HelpUtility.syncWithPlayer(serverPlayer, HelpUtility.getSkillData(serverPlayer));
            NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(nextForm == 1 ? 4.0f : 1.5f, 18, nextForm == 1 ? 0.92f : 1.0f));
        }
        
        return SkillExecutionResult.CONTINUE;
    }

    private static final String[] FORM_KEYS = {
            "skill.boros.form.armor", "skill.boros.form.released", "skill.boros.form.meteoric"
    };

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.boros.form",
                        Component.translatable(FORM_KEYS[pack.getCurrentForm()])),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0xFFAA00, false);
    }
}
