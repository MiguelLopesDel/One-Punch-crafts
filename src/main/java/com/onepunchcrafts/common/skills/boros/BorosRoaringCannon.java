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

public class BorosRoaringCannon implements Skill {
    private final BorosPack pack;

    public BorosRoaringCannon(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        if (pack.getConfig().isExhausted()) {
            player.sendSystemMessage(Component.literal("§c§lSem Energia Vital!"));
            return SkillExecutionResult.CONTINUE;
        }

        if (pack.getCurrentForm() == 0) {
            player.sendSystemMessage(Component.literal("§e§lRoaring Cannon exige Forma Liberada!"));
            return SkillExecutionResult.CONTINUE;
        }

        if (!pack.consumeEnergy(BorosConfig.ROARING_CANNON_COST)) {
            player.sendSystemMessage(Component.literal("§e§lEnergia Insuficiente para Roaring Cannon!"));
            return SkillExecutionResult.CONTINUE;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            fire(serverLevel, player);
        }

        return SkillExecutionResult.CONTINUE;
    }

    private void fire(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(1.2));
        boolean meteoric = pack.getCurrentForm() == 2;
        int samples = meteoric ? 150 : 105;
        double step = 0.75;
        double beamRadius = meteoric ? 3.2 : 1.8;
        double breakRadius = meteoric ? 2.4 : 1.25;
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (meteoric ? 18.0 : 9.0));
        Set<Integer> hitEntityIds = new HashSet<>();

        player.sendSystemMessage(Component.literal("§d§lROARING CANNON!"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8f, meteoric ? 0.65f : 0.85f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.2f, meteoric ? 0.55f : 0.75f);

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(meteoric ? 8.0f : 4.0f, meteoric ? 24 : 14, meteoric ? 0.78f : 0.9f));
        }

        for (int i = 0; i < samples; i++) {
            Vec3 pos = start.add(look.scale(i * step));
            double progress = (double) i / samples;
            double currentRadius = beamRadius * (0.65 + progress * 0.7);

            level.sendParticles(meteoric ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.DRAGON_BREATH,
                    pos.x, pos.y, pos.z,
                    meteoric ? 9 : 5,
                    currentRadius * 0.18, currentRadius * 0.18, currentRadius * 0.18,
                    meteoric ? 0.14 : 0.08);

            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.x, pos.y, pos.z,
                        meteoric ? 5 : 3,
                        currentRadius * 0.22, currentRadius * 0.22, currentRadius * 0.22,
                        0.12);
            }

            AABB hitbox = new AABB(pos.subtract(currentRadius, currentRadius, currentRadius),
                    pos.add(currentRadius, currentRadius, currentRadius));
            List<Entity> entities = level.getEntities(player, hitbox);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living && hitEntityIds.add(entity.getId())) {
                    living.hurt(level.damageSources().playerAttack(player), damage);
                    Vec3 knockback = look.scale(meteoric ? 4.5 : 2.6);
                    living.setDeltaMovement(knockback.x, 0.7, knockback.z);
                    living.hurtMarked = true;
                }
            }

            if (pack.isDestructiveMode() && i % 3 == 0) {
                breakBeamBlocks(level, player, pos, breakRadius);
            }
        }

        Vec3 impact = start.add(look.scale(samples * step));
        level.explode(player, impact.x, impact.y, impact.z,
                meteoric ? 18.0f : 8.0f,
                meteoric,
                Level.ExplosionInteraction.TNT);
    }

    private void breakBeamBlocks(ServerLevel level, Player player, Vec3 center, double radius) {
        int minX = (int) Math.floor(center.x - radius);
        int minY = (int) Math.floor(center.y - radius);
        int minZ = (int) Math.floor(center.z - radius);
        int maxX = (int) Math.ceil(center.x + radius);
        int maxY = (int) Math.ceil(center.y + radius);
        int maxZ = (int) Math.ceil(center.z + radius);
        int broken = 0;
        int maxBlocks = pack.getCurrentForm() == 2 ? 45 : 18;

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

                    level.destroyBlock(pos, pack.getCurrentForm() < 2, player);
                    broken++;
                }
            }
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.literal("§d§lRoaring Cannon"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0xDD55FF, false);
    }
}
