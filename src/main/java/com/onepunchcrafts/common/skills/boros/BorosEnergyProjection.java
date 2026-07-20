package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.BorosBeamVfxPacket;
import com.onepunchcrafts.network.packet.ScreenEffectPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BorosEnergyProjection implements Skill {
    private final BorosPack pack;

    public BorosEnergyProjection(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        if (pack.getConfig().isExhausted()) {
            player.sendSystemMessage(Component.translatable("skill.boros.no_energy"));
            return SkillExecutionResult.CONTINUE;
        }

        if (!pack.consumeEnergy(BorosConfig.ENERGY_BLAST_COST)) {
            player.sendSystemMessage(Component.translatable("skill.boros.insufficient_energy"));
            return SkillExecutionResult.CONTINUE;
        }

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.2f);

            if (player instanceof ServerPlayer serverPlayer) {
                NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(2.0f, 5, 1.1f));
            }

            int range = (int) (50 * pack.getPowerModifier() * 1.6); // Escala range com poder
            if (range > 200) range = 200;
            Set<Integer> hitEntityIds = new HashSet<>();

            // Dedicated quad-beam render on every nearby client.
            sendBeamVfx(serverLevel, player, startPos, lookVec, range * 0.5);

            for (int i = 0; i < range; i++) {
                Vec3 pos = startPos.add(lookVec.scale(i * 0.5));

                // Partículas principais - cor muda se for Meteoric Burst
                serverLevel.sendParticles(
                        pack.getCurrentForm() == 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.DRAGON_BREATH,
                        pos.x, pos.y, pos.z,
                        5, 0.1, 0.1, 0.1, 0.05
                );

                // Partículas de brilho secundárias
                if (i % 2 == 0) {
                    serverLevel.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            pos.x, pos.y, pos.z,
                            2, 0.2, 0.2, 0.2, 0.1
                    );
                }

                // Explosão pequena no rastro periodicamente e quebra blocos
                if (pack.isDestructiveMode() && i % 10 == 0 && i > 0) {
                     serverLevel.sendParticles(
                            ParticleTypes.EXPLOSION,
                            pos.x, pos.y, pos.z,
                            1, 0, 0, 0, 0
                    );
                     
                     if (serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) {
                         BlockPos blockPos = BlockPos.containing(pos);
                         if (serverLevel.getBlockState(blockPos).getDestroySpeed(serverLevel, blockPos) >= 0) {
                             serverLevel.destroyBlock(blockPos, false);
                         }
                     }
                }

                float hitboxSize = 0.8f * (pack.getPowerModifier() / 2.0f);
                if (hitboxSize < 0.8f) hitboxSize = 0.8f;
                if (hitboxSize > 3.0f) hitboxSize = 3.0f;

                AABB hitbox = new AABB(pos.subtract(hitboxSize, hitboxSize, hitboxSize), pos.add(hitboxSize, hitboxSize, hitboxSize));
                List<Entity> entities = serverLevel.getEntities(player, hitbox);

                for (Entity entity : entities) {
                    if (entity instanceof LivingEntity living && hitEntityIds.add(entity.getId())) {
                        // Dano = 10x o dano do soco (Atributo de Ataque)
                        double baseAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                        float damage = (float) (baseAttack * 10.0);
                        
                        living.hurt(serverLevel.damageSources().playerAttack(player), damage);
                        // Empurrão escala com poder
                        Vec3 kb = lookVec.scale(0.5 * pack.getPowerModifier());
                        living.push(kb.x, 0.2 * (pack.getPowerModifier() / 2.0f), kb.z);
                    }
                }
            }
        }

        return SkillExecutionResult.CONTINUE;
    }

    private void sendBeamVfx(ServerLevel level, Player player, Vec3 start, Vec3 look, double beamLength) {
        BorosBeamVfxPacket packet = new BorosBeamVfxPacket(player.getId(), start, look, beamLength,
                BorosBeamVfxPacket.STYLE_ENERGY_PROJECTION, 14);
        NetworkRegister.sendToNearby(level, player.position(), 256.0D, packet);
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.boros.energy_projection"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0xFF00FF, false);
    }
}
