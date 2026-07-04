package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.ScreenEffectPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BorosCollapsingStarCannon implements Skill {
    private final BorosPack pack;

    public BorosCollapsingStarCannon(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        if (pack.getConfig().isExhausted()) {
            player.sendSystemMessage(Component.literal("§c§lSem Energia Vital!"));
            return SkillExecutionResult.CONTINUE;
        }

        if (!pack.isMeteoricBurstActive() || pack.getCurrentForm() != 2) {
            player.sendSystemMessage(Component.literal("§c§lCSRC exige Meteoric Burst!"));
            return SkillExecutionResult.CONTINUE;
        }

        if (!pack.consumeEnergy(BorosConfig.CSRC_COST)) {
            player.sendSystemMessage(Component.literal("§e§lEnergia Insuficiente para CSRC!"));
            return SkillExecutionResult.CONTINUE;
        }

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 targetPos = startPos.add(lookVec.scale(50));

        if (player.level() instanceof ServerLevel serverLevel) {
            player.sendSystemMessage(Component.literal("§5§l✦ COLLAPSING STAR ROARING CANNON! ✦"));

            if (player instanceof ServerPlayer serverPlayer) {
                NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(10.0f, 40, 0.8f));
            }

            // Som de carregamento/disparo massivo
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 1.0f, 0.5f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.5f);

            // Linha de partículas muito mais densa e variada
            for (int i = 0; i < 150; i++) {
                Vec3 particlePos = startPos.add(lookVec.scale(i * 0.4));
                
                // Núcleo roxo/azul
                serverLevel.sendParticles(
                        ParticleTypes.DRAGON_BREATH,
                        particlePos.x, particlePos.y, particlePos.z,
                        10, 0.2, 0.2, 0.2, 0.1
                );
                
                // Eletricidade ao redor
                if (i % 3 == 0) {
                    serverLevel.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            particlePos.x, particlePos.y, particlePos.z,
                            5, 0.5, 0.5, 0.5, 0.2
                    );
                }

                // Fumaça negra para contraste
                if (i % 5 == 0) {
                    serverLevel.sendParticles(
                            ParticleTypes.LARGE_SMOKE,
                            particlePos.x, particlePos.y, particlePos.z,
                            3, 0.3, 0.3, 0.3, 0.05
                    );
                }
            }

            // Explosão massiva escalonada
            float explosionPower = 15.0f * (pack.getPowerModifier() / 2.5f);
            if (explosionPower < 15.0f) explosionPower = 15.0f;
            if (explosionPower > 60.0f) explosionPower = 60.0f;

            serverLevel.explode(
                    player,
                    targetPos.x, targetPos.y, targetPos.z,
                    explosionPower,
                    true, // Adiciona fogo
                    Level.ExplosionInteraction.TNT
            );

            // Partículas de impacto gigantescas
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    targetPos.x, targetPos.y, targetPos.z,
                    10, 1.0, 1.0, 1.0, 0.5
            );

            for (int i = 0; i < 300; i++) {
                serverLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        targetPos.x + (Math.random() - 0.5) * 20,
                        targetPos.y + (Math.random() - 0.5) * 20,
                        targetPos.z + (Math.random() - 0.5) * 20,
                        1, 0.1, 0.1, 0.1, 0.1
                );
            }
        }

        return SkillExecutionResult.CONTINUE;
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.literal("§5§lCollapsing Star Roaring Cannon"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0xFF00FF, false);
    }
}
