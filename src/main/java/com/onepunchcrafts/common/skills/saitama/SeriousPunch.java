package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.damage.DamagesRegistry;
import com.onepunchcrafts.network.packet.SeriousPunchVfxPacket;
import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import com.onepunchcrafts.util.TickUtilities;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;

import java.awt.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.onepunchcrafts.common.event.LivingDamageEventHandler.markBlocksToClear;
import static com.onepunchcrafts.util.HelpUtility.*;

public class SeriousPunch implements Skill {

    /** Ticks of held-back tension before the punch actually lands. */
    public static final int WINDUP_TICKS = 14;

    @Override //contem logica de ray cast
    public SkillExecutionResult execute(Player p) {
        if (!(p.level() instanceof ServerLevel serverLevel) || !(p instanceof ServerPlayer player))
            return null;
        HelpUtility.clientEffects(player);
        seriousPunchWithSpecificTargetAndClientEffects(player, serverLevel);
        return null;
    }

    public static void combatEvents(LivingEvent event) {
        if (HelpUtility.getAttackerEntity(event) instanceof ServerPlayer player) {
            verifyIsSaitamaAndSkill(player, SeriousPunch.class).ifPresent(p -> seriosPunch(event));
        }
    }

    public static void seriosPunch(LivingEvent ev) {
        if (ev instanceof LivingDamageEvent damageEvent) {
            if (damageEvent.getSource().is(DamagesRegistry.SERIOUS_PUNCH_SECOND))
                applyDamageAndReactiveEvent(damageEvent, damageEvent.getEntity());
            else
                performSeriousPunch(damageEvent, (ServerPlayer) damageEvent.getSource().getEntity());
        } else if (ev instanceof LivingAttackEvent event) {
            event.setCanceled(false);
        } else if (ev instanceof LivingHurtEvent event) {
            event.setCanceled(false);
        }
    }

    public static void deathEvent(LivingDeathEvent event) {
        boolean saitamaIsTarget = false;
        if (event.getEntity() instanceof ServerPlayer player) {
            saitamaIsTarget = cancelDeathSaitama(event, player);
        }
        if (!saitamaIsTarget) {
            DamageSource source = event.getSource();
            if (source.is(DamagesRegistry.SERIOUS_PUNCH_SECOND)) {
                if (source.getEntity() instanceof ServerPlayer player && HelpUtility.verifyIsSaitamaAndGetCapability(player).isPresent()) {
                    event.setCanceled(false);
                }
            } else if (source.getDirectEntity() instanceof ServerPlayer player) {
                Optional<SaitamaPack> saitamaPack = HelpUtility.verifyIsSaitamaAndGetCapability(player);
                saitamaPack.ifPresent(cap -> {
                    if (cap.getCurrentSkill() instanceof SeriousPunch)
                        event.setCanceled(false);
                });
            }
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.saitama.serious_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
    }

    private static boolean cancelDeathSaitama(LivingDeathEvent event, ServerPlayer player) {
        Optional<SaitamaPack> onePunchPlayer = HelpUtility.verifyIsSaitamaAndGetCapability(player);
        onePunchPlayer.ifPresent(cap -> {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
        });
        return onePunchPlayer.isPresent();
    }

    private static void performSeriousPunch(LivingDamageEvent event, ServerPlayer player) {
        clientEffects(player);
        ServerLevel serverLevel = player.serverLevel();
        LivingEntity target = event.getEntity();
        applyDamageAndReactiveEvent(event, target);
        seriousPunchWithSpecificTargetAndClientEffects(player, serverLevel);
    }

    private static void applyDamageAndReactiveEvent(LivingDamageEvent event, LivingEntity target) {
        target.setInvulnerable(false);
        target.setSecondsOnFire(60);
        target.invulnerableTime = 0;
        event.setAmount(event.getAmount() * 10_000_000_000_000_000f);
        event.setCanceled(false);
    }

    public static void seriousPunchWithSpecificTargetAndClientEffects(ServerPlayer player, ServerLevel serverLevel) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        Vec3 fist = player.getEyePosition().add(lookVec.scale(1.2));

        // The cinematic timeline (hush, razor line, cone, impact) runs client
        // side; the actual destruction is held back until the windup ends.
        SeriousPunchVfxPacket.broadcast(serverLevel, new SeriousPunchVfxPacket(
                player.getId(), fist, lookVec, WINDUP_TICKS));

        Vec3 cylinderStartPos = playerPos.add(lookVec.scale(3));
        ArrayList<BlockPos> blockPos = markBlocksToClear(serverLevel, 15, 1000, (int) Math.floor(cylinderStartPos.x), (int) Math.floor(cylinderStartPos.y), (int) Math.floor(cylinderStartPos.z), lookVec);

        TickScheduler.scheduleFromHere(Duration.of((WINDUP_TICKS - 4) * 50L, ChronoUnit.MILLIS),
                () -> dragDebris(serverLevel, player, fist, lookVec, true));

        TickScheduler.scheduleFromHere(Duration.of(WINDUP_TICKS * 50L, ChronoUnit.MILLIS), () -> {
            releaseSeriousPunch(player, serverLevel, lookVec, fist, cylinderStartPos, blockPos);
        });
    }

    /** Physical release used by both the legacy entry and the power Timeline Adapter. */
    public static void releaseSeriousPunch(ServerPlayer player, ServerLevel serverLevel, Vec3 lookVec) {
        Vec3 fist = player.getEyePosition().add(lookVec.scale(1.2));
        Vec3 cylinderStartPos = player.position().add(lookVec.scale(3));
        ArrayList<BlockPos> blockPos = markBlocksToClear(serverLevel, 15, 1000,
                (int) Math.floor(cylinderStartPos.x), (int) Math.floor(cylinderStartPos.y),
                (int) Math.floor(cylinderStartPos.z), lookVec);
        releaseSeriousPunch(player, serverLevel, lookVec, fist, cylinderStartPos, blockPos);
    }

    /** Release-only presentation; the power runtime owns destruction/damage as explicit jobs. */
    public static void releaseSeriousVisuals(ServerPlayer player, ServerLevel serverLevel, Vec3 lookVec) {
        Vec3 fist = player.getEyePosition().add(lookVec.scale(1.2));
        carveGroundCracks(serverLevel, player, lookVec);
        dragDebris(serverLevel, player, fist, lookVec, false);
    }

    private static void releaseSeriousPunch(ServerPlayer player, ServerLevel serverLevel, Vec3 lookVec,
                                            Vec3 fist, Vec3 cylinderStartPos, ArrayList<BlockPos> blockPos) {
        final TickUtilities tickU = new TickUtilities();
        TickScheduler.scheduleWithCondition(Duration.of(50, ChronoUnit.MILLIS),
                () -> tickU.fillCylinderAndEmuleEffects(player, serverLevel, 1000, blockPos,
                        cylinderStartPos, lookVec, 15.0f));
        carveGroundCracks(serverLevel, player, lookVec);
        dragDebris(serverLevel, player, fist, lookVec, false);
    }

    /**
     * Real crack overlays on the ground fanning out around the caster, via
     * block-destruction progress (no blocks are actually broken). Points that
     * would fall inside the punch cylinder are skipped — that ground is about
     * to disappear anyway.
     */
    private static void carveGroundCracks(ServerLevel level, ServerPlayer player, Vec3 lookVec) {
        Vec3 flat = new Vec3(lookVec.x, 0, lookVec.z);
        if (flat.lengthSqr() < 1.0e-4) return;
        flat = flat.normalize();
        Vec3 eye = player.getEyePosition();
        RandomSource random = level.random;

        Map<Integer, BlockPos> cracks = new HashMap<>();
        int nextId = -(player.getId() * 1024 + 100_000);
        double[] rayAngles = {-150, -105, -60, -30, 30, 60, 105, 150};

        for (double angleDeg : rayAngles) {
            double angle = Math.toRadians(angleDeg + (random.nextDouble() - 0.5) * 14.0);
            Vec3 ray = new Vec3(
                    flat.x * Math.cos(angle) - flat.z * Math.sin(angle), 0,
                    flat.x * Math.sin(angle) + flat.z * Math.cos(angle));
            double wobble = 0;
            int length = 14 + random.nextInt(22);
            for (int i = 3; i <= length; i++) {
                wobble = wobble * 0.8 + (random.nextDouble() - 0.5) * 1.6;
                Vec3 point = player.position().add(ray.scale(i))
                        .add(-ray.z * wobble, 0, ray.x * wobble);

                // Skip ground the cylinder is about to obliterate.
                Vec3 rel = point.subtract(eye);
                double along = rel.dot(lookVec);
                if (along > 0 && rel.subtract(lookVec.scale(along)).length() < 16.5) continue;

                BlockPos ground = findGround(level, BlockPos.containing(point), player.blockPosition().getY());
                if (ground == null) continue;
                int id = nextId--;
                cracks.put(id, ground);
                level.destroyBlockProgress(id, ground, 3 + random.nextInt(6));
            }
        }

        if (!cracks.isEmpty()) {
            TickScheduler.scheduleFromHere(Duration.of(6, ChronoUnit.SECONDS),
                    () -> cracks.forEach((id, pos) -> level.destroyBlockProgress(id, pos, -1)));
        }
    }

    private static BlockPos findGround(ServerLevel level, BlockPos column, int aroundY) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(column.getX(), aroundY + 8, column.getZ());
        for (int i = 0; i < 20; i++) {
            cursor.move(0, -1, 0);
            if (!level.getBlockState(cursor).isAir()) return cursor.immutable();
        }
        return null;
    }

    /**
     * Block-shard particles around the punch line: sucked toward the fist
     * during the last windup frames, then hurled away with the release.
     */
    private static void dragDebris(ServerLevel level, ServerPlayer player, Vec3 fist, Vec3 lookVec, boolean inward) {
        RandomSource random = level.random;
        int samples = inward ? 34 : 46;
        for (int i = 0; i < samples; i++) {
            Vec3 spot = fist.add(lookVec.scale(2.0 + random.nextDouble() * 12.0))
                    .add(random.nextGaussian() * 4.0, random.nextGaussian() * 2.0, random.nextGaussian() * 4.0);
            BlockPos ground = findGround(level, BlockPos.containing(spot), player.blockPosition().getY());
            if (ground == null) continue;
            BlockState state = level.getBlockState(ground);

            Vec3 pos = new Vec3(ground.getX() + 0.5, ground.getY() + 1.1, ground.getZ() + 0.5);
            Vec3 velocity = inward
                    ? fist.subtract(pos).normalize().scale(0.5).add(0, 0.18, 0)
                    : lookVec.scale(1.1 + random.nextDouble() * 0.7)
                        .add(random.nextGaussian() * 0.25, 0.3 + random.nextDouble() * 0.4, random.nextGaussian() * 0.25);

            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.x, pos.y, pos.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
        }
    }
}
