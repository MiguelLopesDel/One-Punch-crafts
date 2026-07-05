package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.common.skills.SkillPassive;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class BorosDestructiveMovement implements SkillPassive {
    private final BorosPack pack;

    public BorosDestructiveMovement(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            pack.setDestructiveMode(!pack.isDestructiveMode());
            serverPlayer.sendSystemMessage(Component.translatable(
                    pack.isDestructiveMode() ? "skill.boros.destructive_movement.on" : "skill.boros.destructive_movement.off"
            ));
        }
        return SkillExecutionResult.CONTINUE;
    }

    @Override
    public void tick(Player player) {
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        String status = pack.isDestructiveMode() ? "§c[ON]" : "§7[OFF]";
        guiGraphics.drawString(font, Component.translatable("skill.boros.destructive_movement", status),
                width / 2 - defaultReduce, height / 2 + defaultAdd, pack.isDestructiveMode() ? 0xFF3333 : 0x777777, false);
    }
}
