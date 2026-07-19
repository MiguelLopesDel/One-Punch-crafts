package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class BorosRegeneration implements Skill {
    private final BorosPack pack;

    public BorosRegeneration(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        if (pack.getConfig().isExhausted()) {
            player.sendSystemMessage(Component.translatable("skill.boros.no_energy"));
            return SkillExecutionResult.CONTINUE;
        }

        if (pack.getTicksToUseUltraRegeneration() > 0) {
            player.sendSystemMessage(Component.translatable("skill.boros.regeneration.cooldown"));
            return SkillExecutionResult.CONTINUE;
        }

        if (pack.consumeEnergy(BorosConfig.ACTIVE_REGEN_COST)) {
            pack.startUltraRegeneration();
            // Define o cooldown de 5 minutos (6000 ticks)
            pack.setTicksToUseUltraRegeneration(6_000);
            player.sendSystemMessage(Component.translatable("skill.boros.regeneration.active"));
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1.0, 0.5, 0.1);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
            return SkillExecutionResult.CONTINUE;
        }

        player.sendSystemMessage(Component.translatable("skill.boros.insufficient_energy"));
        return SkillExecutionResult.CONTINUE;
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        String cooldown = pack.getTicksToUseUltraRegeneration() > 0 ?
                String.format(" (CD: %ds)", pack.getTicksToUseUltraRegeneration() / 20) : "";
        guiGraphics.drawString(font, Component.translatable("skill.boros.regeneration", cooldown),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0x00FF00, false);
    }
}
