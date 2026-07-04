package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.ScreenEffectPacket;
import com.onepunchcrafts.util.TickScheduler;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class BorosCollapsingStarCannon implements Skill {
    private final BorosPack pack;
    private static final int BEAM_SAMPLES = 210;
    private static final double BEAM_STEP = 1.0;
    private static final int DESTRUCTION_BUDGET_PER_TICK = 900;

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

        pack.setMeteoricBurstActive(false);
        pack.setCurrentForm((short) 1);

        return SkillExecutionResult.CONTINUE;
    }

    private void fireUltimate(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(1.5));
        double baseRadius = 5.0;
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 55.0);
        Set<Integer> hitEntityIds = new HashSet<>();
        Set<BlockPos> queuedDestruction = new HashSet<>();

        player.sendSystemMessage(Component.literal("§5§l✦ COLLAPSING STAR ROARING CANNON! ✦"));
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(18.0f, 70, 0.58f));
        }

        playCharge(level, player, look);

        Vec3 impact = start;
        for (int i = 0; i < BEAM_SAMPLES; i++) {
            double progress = (double) i / (BEAM_SAMPLES - 1);
            double radius = baseRadius + progress * 7.5;
            Vec3 pos = start.add(look.scale(i * BEAM_STEP));
            impact = pos;

            drawBeam(level, pos, radius, i);
            damageEntities(level, player, pos, radius, damage, look, hitEntityIds);

            if (pack.isDestructiveMode()) {
                collectBeamDestruction(level, pos, radius * 0.78, i, queuedDestruction);
            }
        }

        collectImpactCrater(level, impact, 34.0, queuedDestruction);
        scheduleBatchedDestruction(level, player, queuedDestruction);
        finishImpact(level, player, impact, look);
        damageShockwave(level, player, impact, 96.0, damage * 0.35f);
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

    private void collectBeamDestruction(ServerLevel level, Vec3 center, double radius, int sampleIndex, Set<BlockPos> queued) {
        if (sampleIndex % 3 != 0) return;

        int maxBlocks = 180;
        int collected = 0;
        int minX = (int) Math.floor(center.x - radius);
        int minY = (int) Math.floor(center.y - radius);
        int minZ = (int) Math.floor(center.z - radius);
        int maxX = (int) Math.ceil(center.x + radius);
        int maxY = (int) Math.ceil(center.y + radius);
        int maxZ = (int) Math.ceil(center.z + radius);

        for (int x = minX; x <= maxX && collected < maxBlocks; x++) {
            for (int y = minY; y <= maxY && collected < maxBlocks; y++) {
                for (int z = minZ; z <= maxZ && collected < maxBlocks; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!shouldDisintegrate(level, pos, state, center, radius, 180.0f)) continue;

                    if (queued.add(pos)) {
                        collected++;
                    }
                }
            }
        }
    }

    private void collectImpactCrater(ServerLevel level, Vec3 impact, double radius, Set<BlockPos> queued) {
        int minX = (int) Math.floor(impact.x - radius);
        int minY = (int) Math.floor(impact.y - radius * 0.65);
        int minZ = (int) Math.floor(impact.z - radius);
        int maxX = (int) Math.ceil(impact.x + radius);
        int maxY = (int) Math.ceil(impact.y + radius * 0.45);
        int maxZ = (int) Math.ceil(impact.z + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (shouldDisintegrate(level, pos, state, impact, radius, 260.0f)) {
                        queued.add(pos);
                    }
                }
            }
        }
    }

    private boolean shouldDisintegrate(ServerLevel level, BlockPos pos, BlockState state, Vec3 center, double radius, float power) {
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) < 0) return false;

        double dx = pos.getX() + 0.5 - center.x;
        double dy = pos.getY() + 0.5 - center.y;
        double dz = pos.getZ() + 0.5 - center.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > radius * radius) return false;

        double falloff = 1.0 - Math.sqrt(distSq) / radius;
        float resistance = Math.min(state.getBlock().getExplosionResistance(), 1200.0f);
        return power * falloff > resistance * 0.22f;
    }

    private void scheduleBatchedDestruction(ServerLevel level, Player player, Set<BlockPos> blocks) {
        Queue<BlockPos> queue = new ArrayDeque<>(blocks);
        if (queue.isEmpty()) return;

        AtomicInteger tick = new AtomicInteger();
        TickScheduler.scheduleWithCondition(Duration.ofMillis(50), () -> {
            int processed = 0;
            while (processed < DESTRUCTION_BUDGET_PER_TICK && !queue.isEmpty()) {
                BlockPos pos = queue.poll();
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && !state.is(Blocks.BEDROCK) && state.getDestroySpeed(level, pos) >= 0) {
                    level.destroyBlock(pos, false, player);
                }
                processed++;
            }

            int currentTick = tick.getAndIncrement();
            if (currentTick % 2 == 0 && !queue.isEmpty()) {
                BlockPos next = queue.peek();
                level.sendParticles(ParticleTypes.EXPLOSION,
                        next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5,
                        8, 3.0, 3.0, 3.0, 0.12);
            }

            return queue.isEmpty();
        });
    }

    private void finishImpact(ServerLevel level, Player player, Vec3 impact, Vec3 look) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 2.0f, 0.35f);
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.45f);

        level.explode(player, impact.x, impact.y, impact.z,
                28.0f, true, Level.ExplosionInteraction.NONE);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                impact.x, impact.y, impact.z,
                28, 3.0, 3.0, 3.0, 0.85);

        for (int i = 0; i < 360; i++) {
            Vec3 burst = impact.add(look.scale(-i * 0.18));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    burst.x + (Math.random() - 0.5) * 26,
                    burst.y + (Math.random() - 0.5) * 22,
                    burst.z + (Math.random() - 0.5) * 26,
                    1, 0.1, 0.1, 0.1, 0.12);
        }
    }

    private void damageShockwave(ServerLevel level, Player player, Vec3 impact, double radius, float damage) {
        AABB area = new AABB(impact.subtract(radius, radius * 0.55, radius), impact.add(radius, radius * 0.55, radius));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
        for (LivingEntity entity : entities) {
            double distance = entity.position().distanceTo(impact);
            if (distance > radius) continue;

            double falloff = 1.0 - distance / radius;
            entity.hurt(level.damageSources().playerAttack(player), (float) (damage * falloff));
            Vec3 away = entity.position().subtract(impact);
            if (away.lengthSqr() < 0.001) away = player.getLookAngle();
            Vec3 knockback = away.normalize().scale(5.0 * falloff);
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 1.5 * falloff, knockback.z));
            entity.hurtMarked = true;
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.literal("§5§lCollapsing Star Roaring Cannon"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0xFF00FF, false);
    }
}
