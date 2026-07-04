package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.network.NetworkRegister;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        if (player.level() instanceof ServerLevel serverLevel) {
            fireUltimate(serverLevel, player);
        }

        return SkillExecutionResult.CONTINUE;
    }

    private void fireUltimate(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(1.5));
        int samples = 230;
        double step = 0.9;
        double baseRadius = 4.2;
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 55.0);
        Set<Integer> hitEntityIds = new HashSet<>();

        player.sendSystemMessage(Component.literal("§5§l✦ COLLAPSING STAR ROARING CANNON! ✦"));
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(14.0f, 55, 0.65f));
        }

        playCharge(level, player, look);

        Vec3 impact = start;
        for (int i = 0; i < samples; i++) {
            double progress = (double) i / (samples - 1);
            double radius = baseRadius + progress * 5.8;
            Vec3 pos = start.add(look.scale(i * step));
            impact = pos;

            drawBeam(level, pos, radius, i);
            damageEntities(level, player, pos, radius, damage, look, hitEntityIds);

            if (pack.isDestructiveMode()) {
                carveBeam(level, player, pos, radius * 0.72, i);
            }
        }

        finishImpact(level, player, impact, look);
        Vec3 recoil = look.scale(-2.6).add(0, 0.35, 0);
        player.setDeltaMovement(player.getDeltaMovement().add(recoil));
        player.hurtMarked = true;
    }

    private void playCharge(ServerLevel level, Player player, Vec3 look) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.2f, 0.35f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 0.45f);

        Vec3 center = player.getEyePosition().add(look.scale(1.2));
        for (int i = 0; i < 80; i++) {
            double angle = i * 0.45;
            double radius = 0.35 + (i % 12) * 0.08;
            double y = (i % 18) * 0.055;
            level.sendParticles(i % 3 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.DRAGON_BREATH,
                    center.x + Math.cos(angle) * radius,
                    center.y - 0.5 + y,
                    center.z + Math.sin(angle) * radius,
                    2, 0.02, 0.02, 0.02, 0.04);
        }
    }

    private void drawBeam(ServerLevel level, Vec3 pos, double radius, int sampleIndex) {
        level.sendParticles(ParticleTypes.DRAGON_BREATH,
                pos.x, pos.y, pos.z,
                12, radius * 0.16, radius * 0.16, radius * 0.16, 0.12);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                pos.x, pos.y, pos.z,
                8, radius * 0.12, radius * 0.12, radius * 0.12, 0.1);

        if (sampleIndex % 2 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z,
                    8, radius * 0.22, radius * 0.22, radius * 0.22, 0.18);
        }

        if (sampleIndex % 8 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION,
                    pos.x, pos.y, pos.z,
                    1, radius * 0.05, radius * 0.05, radius * 0.05, 0.0);
        }
    }

    private void damageEntities(ServerLevel level, Player player, Vec3 center, double radius, float damage,
                                Vec3 look, Set<Integer> hitEntityIds) {
        AABB hitbox = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));
        List<Entity> entities = level.getEntities(player, hitbox);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && hitEntityIds.add(entity.getId())) {
                living.hurt(level.damageSources().playerAttack(player), damage);
                Vec3 knockback = look.scale(9.0);
                living.setDeltaMovement(knockback.x, 2.2, knockback.z);
                living.hurtMarked = true;
            }
        }
    }

    private void carveBeam(ServerLevel level, Player player, Vec3 center, double radius, int sampleIndex) {
        if (sampleIndex % 2 != 0) return;

        int maxBlocks = 115;
        int broken = 0;
        int minX = (int) Math.floor(center.x - radius);
        int minY = (int) Math.floor(center.y - radius);
        int minZ = (int) Math.floor(center.z - radius);
        int maxX = (int) Math.ceil(center.x + radius);
        int maxY = (int) Math.ceil(center.y + radius);
        int maxZ = (int) Math.ceil(center.z + radius);

        for (int x = minX; x <= maxX && broken < maxBlocks; x++) {
            for (int y = minY; y <= maxY && broken < maxBlocks; y++) {
                for (int z = minZ; z <= maxZ && broken < maxBlocks; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;

                    double dx = pos.getX() + 0.5 - center.x;
                    double dy = pos.getY() + 0.5 - center.y;
                    double dz = pos.getZ() + 0.5 - center.z;
                    if ((dx * dx + dy * dy + dz * dz) > radius * radius) continue;

                    level.destroyBlock(pos, false, player);
                    broken++;
                }
            }
        }
    }

    private void finishImpact(ServerLevel level, Player player, Vec3 impact, Vec3 look) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 2.0f, 0.35f);
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.45f);

        level.explode(player, impact.x, impact.y, impact.z,
                72.0f, true, Level.ExplosionInteraction.TNT);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                impact.x, impact.y, impact.z,
                18, 2.0, 2.0, 2.0, 0.6);

        for (int i = 0; i < 240; i++) {
            Vec3 burst = impact.add(look.scale(-i * 0.18));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    burst.x + (Math.random() - 0.5) * 18,
                    burst.y + (Math.random() - 0.5) * 18,
                    burst.z + (Math.random() - 0.5) * 18,
                    1, 0.1, 0.1, 0.1, 0.12);
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.literal("§5§lCollapsing Star Roaring Cannon"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0xFF00FF, false);
    }
}
