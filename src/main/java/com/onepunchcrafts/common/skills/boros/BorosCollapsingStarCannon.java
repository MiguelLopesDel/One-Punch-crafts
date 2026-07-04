package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AnimationPacket;
import com.onepunchcrafts.network.packet.BorosCsrcVfxPacket;
import com.onepunchcrafts.network.packet.ScreenEffectPacket;
import com.onepunchcrafts.util.TickScheduler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class BorosCollapsingStarCannon implements Skill {
    private final BorosPack pack;
    private static final int BEAM_SAMPLES = 260;
    private static final double BEAM_STEP = 1.0;
    // Timeline shared with the client post shader: charge -> release.
    private static final int CHARGE_TICKS = 50;
    private static final int FIRE_TICKS = 44;
    // Destruction is processed by resumable stages with a per-tick time budget
    // (HBM style, minus its slowness). The budget is adaptive: we measure the
    // real interval between our destruction ticks — if the server is keeping
    // 20 TPS we ramp the budget up to eat blocks as fast as possible, and the
    // moment ticks start stretching we back off, so it never feels frozen.
    private static final long RELEASE_NANO_BUDGET = 30_000_000L;
    private static final long START_TICK_BUDGET = 14_000_000L;
    private static final long MIN_TICK_BUDGET = 4_000_000L;
    private static final long MAX_TICK_BUDGET = 32_000_000L;
    private static final long HEALTHY_TICK_NANOS = 53_000_000L;
    private static final long LAGGING_TICK_NANOS = 58_000_000L;
    private static final int BLOCK_UPDATE_FLAGS = 2 | 16; // clients only, no neighbor cascades
    private static final double DIRECT_DAMAGE_MULTIPLIER = 500.0;
    private static final double BACKBLAST_DAMAGE_MULTIPLIER = 220.0;
    private static final double SHOCKWAVE_DAMAGE_MULTIPLIER = 300.0;
    private static final float MIN_DIRECT_DAMAGE = 50_000_000.0f;
    private static final float MIN_BACKBLAST_DAMAGE = 60_000_000.0f;
    private static final float MIN_SHOCKWAVE_DAMAGE = 80_000_000.0f;

    public BorosCollapsingStarCannon(BorosPack pack) {
        this.pack = pack;
    }

    @Override
    public SkillExecutionResult execute(Player player) {
        if (pack.getConfig().isExhausted() && !player.isCreative()) {
            player.sendSystemMessage(Component.literal("§c§lSem Energia Vital!"));
            return SkillExecutionResult.CONTINUE;
        }

        if (!pack.isMeteoricBurstActive() || pack.getCurrentForm() != 2) {
            player.sendSystemMessage(Component.literal("§c§lCSRC exige Meteoric Burst!"));
            return SkillExecutionResult.CONTINUE;
        }

        if (!player.isCreative() && !pack.consumeEnergy(BorosConfig.CSRC_COST)) {
            player.sendSystemMessage(Component.literal("§e§lEnergia Insuficiente para CSRC!"));
            return SkillExecutionResult.CONTINUE;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            fireUltimate(serverLevel, player);
        }

        if (!player.isCreative()) {
            pack.setMeteoricBurstActive(false);
            pack.setCurrentForm((short) 1);
        }

        return SkillExecutionResult.CONTINUE;
    }

    private void fireUltimate(ServerLevel level, Player player) {
        // Lock aim and position at the start of the charge, like Boros planting
        // himself for the release; the client shader follows the same timeline.
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(1.5));
        Vec3 impact = start.add(look.scale((BEAM_SAMPLES - 1) * BEAM_STEP));

        player.sendSystemMessage(Component.literal("§5§l✦ COLLAPSING STAR ROARING CANNON! ✦"));
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkRegister.sendToPlayer(serverPlayer, new ScreenEffectPacket(18.0f, CHARGE_TICKS + FIRE_TICKS + 40, 0.58f));
        }

        playCharge(level, player, look);
        sendCinematicVfx(level, player, start, look, impact);

        TickScheduler.scheduleFromHere(Duration.ofMillis(CHARGE_TICKS * 50L),
                () -> releaseBeam(level, player, start, look, impact));
    }

    private void releaseBeam(ServerLevel level, Player player, Vec3 start, Vec3 look, Vec3 beamEnd) {
        if (player.isRemoved() || !player.isAlive() || player.level() != level) return;

        double baseRadius = 6.5;
        double beamLength = (BEAM_SAMPLES - 1) * BEAM_STEP;
        double baseAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) Math.max(MIN_DIRECT_DAMAGE, baseAttack * DIRECT_DAMAGE_MULTIPLIER);
        float backblastDamage = (float) Math.max(MIN_BACKBLAST_DAMAGE, baseAttack * BACKBLAST_DAMAGE_MULTIPLIER);
        float shockwaveDamage = (float) Math.max(MIN_SHOCKWAVE_DAMAGE, baseAttack * SHOCKWAVE_DAMAGE_MULTIPLIER);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0f, 0.55f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.6f, 0.4f);

        Vec3 core = player.position().add(0, player.getBbHeight() * 0.62, 0).add(look.scale(0.35));
        Vec3 muzzleCraterCenter = core.add(look.scale(-3.5)).subtract(0, player.getBbHeight() * 0.25, 0);
        Vec3 impact = beamEnd;

        // Craters must bite terrain even when Boros fires while flying or the
        // beam ends mid-air: anchor each blast center to the ground below it.
        Vec3 muzzleGround = anchorToGround(level, muzzleCraterCenter, 96);
        Vec3 coreGround = anchorToGround(level, core, 96);
        Vec3 impactGround = anchorToGround(level, impact, 96);

        // Sparse debris particles along the path; the shader owns the beam.
        for (int i = 0; i < BEAM_SAMPLES; i += 3) {
            double progress = (double) i / (BEAM_SAMPLES - 1);
            drawBeam(level, start.add(look.scale(i * BEAM_STEP)), baseRadius + progress * 11.5, i);
        }

        damageEntitiesAlongBeam(level, player, start, look, beamLength, baseRadius, damage);

        // Two destruction zones advance in parallel every tick, so the launch
        // site and the impact site erupt together instead of one waiting for
        // the other; within each zone stages still roll near -> far.
        List<DestructionStage> muzzleZone = List.of(
                new TsarCraterStage(muzzleGround, 96, 54, 72, 2800.0f),
                new RadialBlastStage(coreGround, 58, 30, 42, 2200.0f),
                new SurfaceScrapeStage(muzzleGround, 78, 135, 8)
        );
        List<DestructionStage> impactZone = List.of(
                new BeamTrenchStage(start, look, baseRadius),
                new TsarCraterStage(impactGround, 104, 62, 46, 2100.0f),
                new RadialBlastStage(impactGround, 88, 52, 36, 1700.0f),
                new SurfaceScrapeStage(impactGround, 100, 195, 13)
        );

        emitServerCinematicVfx(level, player, start, look, impact);
        scheduleDestructionPipeline(level, muzzleZone, impactZone);
        finishImpact(level, player, impact, look);
        damageCasterBackblast(level, player, core, 52.0, backblastDamage);
        damageShockwave(level, player, impact, 144.0, shockwaveDamage);
        Vec3 recoil = look.scale(-2.6).add(0, 0.35, 0);
        player.setDeltaMovement(player.getDeltaMovement().add(recoil));
        player.hurtMarked = true;
    }

    private void sendCinematicVfx(ServerLevel level, Player player, Vec3 start, Vec3 look, Vec3 impact) {
        double range = start.distanceTo(impact);
        BorosCsrcVfxPacket vfxPacket = new BorosCsrcVfxPacket(player.getId(), start, look, range,
                CHARGE_TICKS, FIRE_TICKS, impact, 104.0);

        for (ServerPlayer serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            if (serverPlayer.level() == level && serverPlayer.distanceToSqr(player) <= 512.0D * 512.0D) {
                NetworkRegister.sendToPlayer(serverPlayer, vfxPacket);
                NetworkRegister.sendToPlayer(serverPlayer, new AnimationPacket(player.getStringUUID(), "csrc_charge_release"));
            }
        }

        TickScheduler.scheduleFromHere(Duration.ofMillis((CHARGE_TICKS + FIRE_TICKS) * 50L + 600L),
                () -> NetworkRegister.sendToAllClients(new AnimationPacket(player.getStringUUID(), "stop")));
    }

    private void emitServerCinematicVfx(ServerLevel level, Player player, Vec3 start, Vec3 look, Vec3 impact) {
        Vec3 core = player.position().add(0, player.getBbHeight() * 0.62, 0).add(look.scale(0.35));
        level.sendParticles(ParticleTypes.FLASH, core.x, core.y, core.z, 5, 0.25, 0.25, 0.25, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, core.x, core.y, core.z, 220, 1.6, 1.2, 1.6, 0.38);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, core.x, core.y, core.z, 180, 1.4, 1.0, 1.4, 0.7);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, core.x, core.y, core.z, 160, 1.8, 1.2, 1.8, 0.16);

        emitParticleRing(level, core, look, 2.6, 72, ParticleTypes.END_ROD, 0.04);
        emitParticleRing(level, core.add(look.scale(0.6)), look, 5.2, 104, ParticleTypes.ELECTRIC_SPARK, 0.26);
        emitParticleRing(level, core.add(look.scale(1.2)), look, 8.5, 132, ParticleTypes.SOUL_FIRE_FLAME, 0.12);

        double range = start.distanceTo(impact);
        int samples = Math.max(24, Math.min(90, (int) (range / 3.0)));
        for (int i = 0; i <= samples; i++) {
            double progress = (double) i / samples;
            Vec3 pos = start.add(look.scale(range * progress));
            double radius = 0.9 + progress * 5.5;

            level.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z,
                    10, radius * 0.10, radius * 0.10, radius * 0.10, 0.18);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.x, pos.y, pos.z,
                    8, radius * 0.14, radius * 0.14, radius * 0.14, 0.12);

            if (i % 4 == 0) {
                emitParticleRing(level, pos, look, radius * 0.8, 36, ParticleTypes.ELECTRIC_SPARK, 0.18);
            }
        }
    }

    private void emitParticleRing(ServerLevel level, Vec3 center, Vec3 normal, double radius, int points,
                                  net.minecraft.core.particles.SimpleParticleType particle, double speed) {
        Vec3 side = normal.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.001) side = normal.cross(new Vec3(1, 0, 0));
        side = side.normalize();
        Vec3 up = side.cross(normal).normalize();

        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            Vec3 offset = side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
            level.sendParticles(particle,
                    center.x + offset.x, center.y + offset.y, center.z + offset.z,
                    1, 0.02, 0.02, 0.02, speed);
        }
    }

    private void playCharge(ServerLevel level, Player player, Vec3 look) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.2f, 0.35f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 0.45f);
        scheduleAfterTicks(CHARGE_TICKS / 2, () -> level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2.4f, 0.65f));

        // Sustained implosion vortex: energy motes stream inward for the whole
        // charge, tightening and accelerating as the star condenses.
        AtomicInteger pulses = new AtomicInteger();
        TickScheduler.scheduleWithCondition(Duration.ofMillis(100), () -> {
            int pulse = pulses.incrementAndGet();
            double progress = Math.min(1.0, pulse * 2.0 / CHARGE_TICKS);
            Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(1.2));

            int points = 10 + (int) (26 * progress);
            double radius = 7.5 - 5.8 * progress;
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0 * i) / points + pulse * 0.35;
                double yOffset = Math.sin(angle * 3.0 + pulse * 0.5) * 1.4;
                Vec3 spawn = center.add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
                Vec3 inward = center.subtract(spawn).normalize();
                level.sendParticles(i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.DRAGON_BREATH,
                        spawn.x, spawn.y, spawn.z,
                        0, inward.x, inward.y, inward.z, 0.55 + progress * 0.5);
            }

            if (pulse % 5 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        center.x, center.y, center.z,
                        8, 0.4 + progress, 0.4 + progress, 0.4 + progress, 0.05);
            }

            return pulse * 2 >= CHARGE_TICKS || !player.isAlive();
        });
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

    private void damageEntitiesAlongBeam(ServerLevel level, Player player, Vec3 start, Vec3 look,
                                         double beamLength, double baseRadius, float damage) {
        Vec3 end = start.add(look.scale(beamLength));
        AABB corridor = new AABB(start, end).inflate(baseRadius + 12.5);
        for (Entity entity : level.getEntities(player, corridor)) {
            if (!(entity instanceof LivingEntity living)) continue;

            Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            double along = Math.max(0.0, Math.min(beamLength, center.subtract(start).dot(look)));
            double radiusHere = baseRadius + (along / beamLength) * 11.5 + entity.getBbWidth() * 0.5;
            if (center.distanceTo(start.add(look.scale(along))) > radiusHere) continue;

            living.invulnerableTime = 0;
            living.hurt(level.damageSources().playerAttack(player), damage);
            Vec3 knockback = look.scale(9.0);
            living.setDeltaMovement(knockback.x, 2.2, knockback.z);
            living.hurtMarked = true;
        }
    }

    private double tsarProfileStrength(double horizontalDistance, int dy, double craterRadius,
                                       double topRadius, int depth, int topRemovalHeight) {
        if (dy <= 0) {
            double xz = horizontalDistance / Math.max(1.0, craterRadius);
            double y = Math.abs(dy) / Math.max(1.0, depth);
            double ellipsoid = xz * xz + y * y;
            return ellipsoid <= 1.0 ? 1.0 - ellipsoid * 0.72 : 0.0;
        }

        double yNorm = dy / Math.max(1.0, topRemovalHeight);
        double topFalloff = Math.max(0.0, 1.0 - yNorm * 0.68);
        double allowedRadius = topRadius * topFalloff;
        if (horizontalDistance > allowedRadius) return 0.0;

        double radialFalloff = 1.0 - horizontalDistance / Math.max(1.0, allowedRadius);
        return Math.max(0.0, 0.42 + radialFalloff * 0.58 - yNorm * 0.18);
    }

    private boolean shouldDisintegrateArea(ServerLevel level, BlockPos pos, BlockState state, double strength, float power) {
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) < 0) return false;
        if (strength > 0.58) return true;

        float resistance = Math.min(state.getBlock().getExplosionResistance(), 1200.0f);
        return power * strength > resistance * 0.06f;
    }

    private double areaNoise(int x, int z) {
        double wave1 = Math.sin(x * 0.19) * Math.cos(z * 0.13);
        double wave2 = Math.sin((x + z) * 0.07);
        double wave3 = Math.cos((x - z) * 0.11);
        return (wave1 + wave2 * 0.65 + wave3 * 0.45) / 2.1;
    }

    private boolean shouldDisintegrate(ServerLevel level, BlockPos pos, BlockState state, Vec3 center, double radius, float power) {
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) < 0) return false;

        double dx = pos.getX() + 0.5 - center.x;
        double dy = pos.getY() + 0.5 - center.y;
        double dz = pos.getZ() + 0.5 - center.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > radius * radius) return false;

        double falloff = 1.0 - Math.sqrt(distSq) / radius;
        if (falloff > 0.42 && power >= 400.0f) return true;

        float resistance = Math.min(state.getBlock().getExplosionResistance(), 1200.0f);
        return power * falloff > resistance * 0.08f;
    }

    private void damageCasterBackblast(ServerLevel level, Player player, Vec3 center, double radius, float damage) {
        AABB area = new AABB(center.subtract(radius, radius * 0.55, radius), center.add(radius, radius * 0.55, radius));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
        for (LivingEntity entity : entities) {
            double distance = entity.position().add(0, entity.getBbHeight() * 0.5, 0).distanceTo(center);
            if (distance > radius) continue;

            double falloff = 1.0 - distance / radius;
            entity.invulnerableTime = 0;
            entity.hurt(level.damageSources().playerAttack(player), (float) (damage * Math.max(0.45, falloff)));
            Vec3 away = entity.position().subtract(player.position());
            if (away.lengthSqr() < 0.001) away = player.getLookAngle().scale(-1);
            Vec3 knockback = away.normalize().scale(10.0 * Math.max(0.45, falloff));
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 4.0 * Math.max(0.45, falloff), knockback.z));
            entity.hurtMarked = true;
        }
    }

    /** Drops a mid-air blast center onto the terrain surface below it. */
    private Vec3 anchorToGround(ServerLevel level, Vec3 pos, int maxDrop) {
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);
        if (!level.hasChunk(x >> 4, z >> 4)) return pos;

        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (pos.y > surface + 4 && pos.y - surface <= maxDrop) {
            return new Vec3(pos.x, surface + 2, pos.z);
        }
        return pos;
    }

    private void scheduleDestructionPipeline(ServerLevel level, List<DestructionStage> muzzleStages,
                                             List<DestructionStage> impactStages) {
        Queue<DestructionStage> muzzleZone = new ArrayDeque<>(muzzleStages);
        Queue<DestructionStage> impactZone = new ArrayDeque<>(impactStages);

        // One violent bite the instant the beam fires, then budgeted ticks.
        advanceZone(level, muzzleZone, System.nanoTime(), RELEASE_NANO_BUDGET / 2);
        advanceZone(level, impactZone, System.nanoTime(), RELEASE_NANO_BUDGET / 2);
        if (muzzleZone.isEmpty() && impactZone.isEmpty()) return;

        AtomicInteger tick = new AtomicInteger();
        // [0] = current budget in nanos, [1] = nanoTime of the previous run.
        long[] pacing = {START_TICK_BUDGET, 0L};
        TickScheduler.scheduleWithCondition(Duration.ofMillis(50), () -> {
            long now = System.nanoTime();
            long budget = pacing[0];
            if (pacing[1] != 0L) {
                long interval = now - pacing[1];
                if (interval < HEALTHY_TICK_NANOS) {
                    // Server is holding 20 TPS with room to spare: push harder.
                    budget = Math.min((long) (budget * 1.25), MAX_TICK_BUDGET);
                } else if (interval > LAGGING_TICK_NANOS) {
                    // Ticks are stretching: we are the likely culprit, back off.
                    budget = Math.max((long) (budget * 0.55), MIN_TICK_BUDGET);
                }
                pacing[0] = budget;
            }
            pacing[1] = now;

            // Both ends of the beam erupt at once; whatever one zone leaves
            // unused, the other consumes.
            long half = budget / 2;
            long muzzleStart = System.nanoTime();
            advanceZone(level, muzzleZone, muzzleStart, half);
            long impactStart = System.nanoTime();
            advanceZone(level, impactZone, impactStart, budget - (impactStart - muzzleStart));

            if (tick.getAndIncrement() % 2 == 0 && lastDestroyed != null) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        lastDestroyed.getX() + 0.5, lastDestroyed.getY() + 0.5, lastDestroyed.getZ() + 0.5,
                        2, 2.6, 2.6, 2.6, 0.08);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        lastDestroyed.getX() + 0.5, lastDestroyed.getY() + 0.5, lastDestroyed.getZ() + 0.5,
                        36, 4.0, 4.0, 4.0, 0.35);
            }

            return muzzleZone.isEmpty() && impactZone.isEmpty();
        });
    }

    private void advanceZone(ServerLevel level, Queue<DestructionStage> zone, long start, long nanoBudget) {
        while (!zone.isEmpty() && System.nanoTime() - start < nanoBudget) {
            if (zone.peek().advance(level, start, nanoBudget)) {
                zone.poll();
            }
        }
    }

    private void destroyBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (destroyCounter++ % 300 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.35, 0.35, 0.35, 0.18);
            lastDestroyed = pos.immutable();
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
    }

    private int destroyCounter;
    private BlockPos lastDestroyed;

    /** A resumable slice of the blast; returns true once exhausted. */
    private interface DestructionStage {
        boolean advance(ServerLevel level, long budgetStart, long nanoBudget);
    }

    /**
     * Walks square rings outward from a center column so destruction spreads
     * as an expanding wave, and can pause/resume at any column.
     */
    private abstract class RingStage implements DestructionStage {
        protected final int centerX;
        protected final int centerY;
        protected final int centerZ;
        private final int maxRing;
        private int ring;
        private int cursor;

        RingStage(Vec3 center, int startRing, int maxRing) {
            this.centerX = (int) Math.floor(center.x);
            this.centerY = (int) Math.floor(center.y);
            this.centerZ = (int) Math.floor(center.z);
            this.ring = startRing;
            this.maxRing = maxRing;
        }

        @Override
        public boolean advance(ServerLevel level, long budgetStart, long nanoBudget) {
            while (System.nanoTime() - budgetStart < nanoBudget) {
                if (ring > maxRing) return true;

                int perimeter = ring == 0 ? 1 : 8 * ring;
                if (cursor >= perimeter) {
                    ring++;
                    cursor = 0;
                    continue;
                }

                int dx;
                int dz;
                if (ring == 0) {
                    dx = 0;
                    dz = 0;
                } else {
                    int side = cursor / (2 * ring);
                    int off = (cursor % (2 * ring)) - ring;
                    switch (side) {
                        case 0 -> { dx = off; dz = -ring; }
                        case 1 -> { dx = ring; dz = off; }
                        case 2 -> { dx = -off; dz = ring; }
                        default -> { dx = -ring; dz = -off; }
                    }
                }
                cursor++;
                processColumn(level, dx, dz, ring);
            }
            return ring > maxRing;
        }

        protected abstract void processColumn(ServerLevel level, int dx, int dz, int ring);
    }

    /** The deep annihilation crater: ellipsoid bowl + flared top removal. */
    private class TsarCraterStage extends RingStage {
        private final int craterRadius;
        private final int depth;
        private final int topRemovalHeight;
        private final float power;
        private final int topRadius;
        private final Vec3 center;

        TsarCraterStage(Vec3 center, int horizontalRadius, int depth, int topRemovalHeight, float power) {
            super(center, 0, (int) Math.ceil(horizontalRadius * 1.35));
            this.center = center;
            this.craterRadius = horizontalRadius;
            this.depth = depth;
            this.topRemovalHeight = topRemovalHeight;
            this.power = power;
            this.topRadius = (int) Math.ceil(horizontalRadius * 1.35);
        }

        @Override
        protected void processColumn(ServerLevel level, int dx, int dz, int ring) {
            // Never force-load/generate chunks; blasting only the loaded world.
            if (!level.hasChunk((centerX + dx) >> 4, (centerZ + dz) >> 4)) return;

            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            double noise = areaNoise(centerX + dx, centerZ + dz);
            double noisyCraterRadius = craterRadius * (0.9 + noise * 0.16);
            double noisyTopRadius = topRadius * (0.88 + noise * 0.14);
            if (horizontalDistance > noisyTopRadius) return;

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dy = -depth; dy <= topRemovalHeight; dy++) {
                double strength = tsarProfileStrength(horizontalDistance, dy, noisyCraterRadius, noisyTopRadius, depth, topRemovalHeight);
                if (strength <= 0.0) continue;

                pos.set(centerX + dx, centerY + dy, centerZ + dz);
                BlockState state = level.getBlockState(pos);
                if (shouldDisintegrateArea(level, pos, state, strength, power)) {
                    destroyBlock(level, pos, state);
                }
            }
        }
    }

    /** Spherical blast pocket with noisy edges (muzzle core / impact core). */
    private class RadialBlastStage extends RingStage {
        private final Vec3 center;
        private final int horizontalRadius;
        private final int downRadius;
        private final int upRadius;
        private final float power;

        RadialBlastStage(Vec3 center, int horizontalRadius, int downRadius, int upRadius, float power) {
            super(center, 0, horizontalRadius);
            this.center = center;
            this.horizontalRadius = horizontalRadius;
            this.downRadius = downRadius;
            this.upRadius = upRadius;
            this.power = power;
        }

        @Override
        protected void processColumn(ServerLevel level, int dx, int dz, int ring) {
            if (!level.hasChunk((centerX + dx) >> 4, (centerZ + dz) >> 4)) return;

            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            double noisyRadius = horizontalRadius * (0.92 + 0.13 * areaNoise(centerX + dx, centerZ + dz));
            if (horizontalDistance > noisyRadius) return;

            double normalized = horizontalDistance / Math.max(1.0, noisyRadius);
            double verticalFactor = Math.sqrt(Math.max(0.0, 1.0 - normalized * normalized));
            int minY = centerY - Math.max(2, (int) Math.ceil(downRadius * verticalFactor));
            int maxY = centerY + Math.max(2, (int) Math.ceil(upRadius * verticalFactor));

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = minY; y <= maxY; y++) {
                pos.set(centerX + dx, y, centerZ + dz);
                BlockState state = level.getBlockState(pos);
                if (shouldDisintegrate(level, pos, state, center, noisyRadius, power)) {
                    destroyBlock(level, pos, state);
                }
            }
        }
    }

    /** Carves the widening tunnel the beam drills through the world. */
    private class BeamTrenchStage implements DestructionStage {
        private final Vec3 start;
        private final Vec3 look;
        private final double baseRadius;
        private int sample;

        BeamTrenchStage(Vec3 start, Vec3 look, double baseRadius) {
            this.start = start;
            this.look = look;
            this.baseRadius = baseRadius;
        }

        @Override
        public boolean advance(ServerLevel level, long budgetStart, long nanoBudget) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            while (System.nanoTime() - budgetStart < nanoBudget) {
                if (sample >= BEAM_SAMPLES) return true;

                double progress = (double) sample / (BEAM_SAMPLES - 1);
                double radius = baseRadius + progress * 11.5;
                Vec3 center = start.add(look.scale(sample * BEAM_STEP));
                if (!level.hasChunk((int) center.x >> 4, (int) center.z >> 4)) {
                    sample += 2;
                    continue;
                }

                int minX = (int) Math.floor(center.x - radius);
                int minY = (int) Math.floor(center.y - radius);
                int minZ = (int) Math.floor(center.z - radius);
                int maxX = (int) Math.ceil(center.x + radius);
                int maxY = (int) Math.ceil(center.y + radius);
                int maxZ = (int) Math.ceil(center.z + radius);

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            pos.set(x, y, z);
                            BlockState state = level.getBlockState(pos);
                            if (shouldDisintegrate(level, pos, state, center, radius, 900.0f)) {
                                destroyBlock(level, pos, state);
                            }
                        }
                    }
                }
                sample += 2;
            }
            return sample >= BEAM_SAMPLES;
        }
    }

    /**
     * HBM Tsar-style surface leveling: outside the main crater the ground is
     * scraped a few blocks deep, shallower with distance (plus noise for a
     * ragged edge), until only surface soil and trees are gone and the terrain
     * fades back to untouched Minecraft.
     */
    private class SurfaceScrapeStage extends RingStage {
        private final int innerRadius;
        private final int outerRadius;
        private final int maxDepth;

        SurfaceScrapeStage(Vec3 center, int innerRadius, int outerRadius, int maxDepth) {
            super(center, innerRadius, outerRadius);
            this.innerRadius = innerRadius;
            this.outerRadius = outerRadius;
            this.maxDepth = maxDepth;
        }

        @Override
        protected void processColumn(ServerLevel level, int dx, int dz, int ring) {
            int x = centerX + dx;
            int z = centerZ + dz;
            if (!level.hasChunk(x >> 4, z >> 4)) return;

            // Euclidean distance keeps the leveling circular; the square ring
            // walk is only an iteration order, so its corners must be trimmed.
            double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
            if (dist > outerRadius) return;

            double t = Math.min(1.0, Math.max(0.0, (dist - innerRadius) / Math.max(1, outerRadius - innerRadius)));
            double noise = areaNoise(x, z);

            // Ragged edge: with distance, more and more columns are spared.
            if ((noise * 0.5 + 0.5) < (t - 0.55) * 1.9) return;

            double falloff = Math.pow(1.0 - t, 1.6);
            int depth = (int) Math.round(maxDepth * falloff * (0.8 + 0.4 * noise));
            // Near the crater rim even stone is shaved; farther out only soft
            // surface material (dirt, sand, logs, leaves) is stripped.
            float maxResistance = t < 0.25 ? 9.0f : 2.5f;

            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);

            // Strip tree canopies completely so no floating leaves remain.
            int guard = 0;
            while (y > level.getMinBuildHeight() && guard++ < 48) {
                pos.setY(y);
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    y--;
                    continue;
                }
                if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                    destroyBlock(level, pos, state);
                    y--;
                    continue;
                }
                break;
            }

            // Shave the ground itself.
            int removed = 0;
            while (removed < depth && y > level.getMinBuildHeight()) {
                pos.setY(y);
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    if (state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) < 0) break;
                    if (state.getBlock().getExplosionResistance() > maxResistance) break;
                    destroyBlock(level, pos, state);
                }
                removed++;
                y--;
            }
        }
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
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                impact.x, impact.y, impact.z,
                240, 10.0, 7.0, 10.0, 0.8);

        for (int i = 0; i < 360; i++) {
            Vec3 burst = impact.add(look.scale(-i * 0.18));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    burst.x + (Math.random() - 0.5) * 26,
                    burst.y + (Math.random() - 0.5) * 22,
                    burst.z + (Math.random() - 0.5) * 26,
                    1, 0.1, 0.1, 0.1, 0.12);
        }

        scheduleImpactVfx(level, impact, look);
    }

    private void scheduleImpactVfx(ServerLevel level, Vec3 impact, Vec3 look) {
        scheduleAfterTicks(3, () -> {
            spawnShockwaveRing(level, impact, 16.0, 96);
            level.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.4f, 0.55f);
        });
        scheduleAfterTicks(6, () -> spawnShockwaveRing(level, impact, 30.0, 144));
        scheduleAfterTicks(10, () -> spawnShockwaveRing(level, impact, 46.0, 192));
        scheduleAfterTicks(14, () -> spawnCollapseColumn(level, impact, look));
    }

    private void scheduleAfterTicks(int ticks, Runnable task) {
        TickScheduler.scheduleFromHere(Duration.ofMillis(Math.max(1, ticks) * 50L), task);
    }

    private void spawnShockwaveRing(ServerLevel level, Vec3 center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.END_ROD,
                    x, center.y + 0.35, z,
                    1, 0.05, 0.05, 0.05, 0.08);

            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        x, center.y + 0.15, z,
                        2, 0.35, 0.18, 0.35, 0.03);
            }
        }
    }

    private void spawnCollapseColumn(ServerLevel level, Vec3 impact, Vec3 look) {
        for (int i = 0; i < 180; i++) {
            double angle = i * 0.38;
            double radius = 2.0 + (i % 24) * 0.72;
            double y = (i % 50) * 0.32;
            Vec3 base = impact.add(look.scale(-(i % 30) * 0.28));

            level.sendParticles(i % 3 == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.DRAGON_BREATH,
                    base.x + Math.cos(angle) * radius,
                    base.y + y,
                    base.z + Math.sin(angle) * radius,
                    2, 0.4, 0.55, 0.4, 0.04);

            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.EXPLOSION,
                        base.x + Math.cos(angle) * radius * 0.55,
                        base.y + y * 0.45,
                        base.z + Math.sin(angle) * radius * 0.55,
                        1, 0.15, 0.15, 0.15, 0.0);
            }
        }
    }

    private void damageShockwave(ServerLevel level, Player player, Vec3 impact, double radius, float damage) {
        AABB area = new AABB(impact.subtract(radius, radius * 0.55, radius), impact.add(radius, radius * 0.55, radius));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
        for (LivingEntity entity : entities) {
            double distance = entity.position().distanceTo(impact);
            if (distance > radius) continue;

            double falloff = 1.0 - distance / radius;
            entity.invulnerableTime = 0;
            entity.hurt(level.damageSources().playerAttack(player), (float) (damage * Math.max(0.25, falloff)));
            Vec3 away = entity.position().subtract(impact);
            if (away.lengthSqr() < 0.001) away = player.getLookAngle();
            Vec3 knockback = away.normalize().scale(8.0 * Math.max(0.25, falloff));
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 2.5 * Math.max(0.25, falloff), knockback.z));
            entity.hurtMarked = true;
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.literal("§5§lCollapsing Star Roaring Cannon"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, 0xFF00FF, false);
    }
}
