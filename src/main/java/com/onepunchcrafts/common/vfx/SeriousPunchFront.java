package com.onepunchcrafts.common.vfx;

import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.SaitamaVfxPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server-side presentation of the Serious Punch destruction front. The
 * cylinder eats through the world over many seconds; this keeps the punch
 * *visible* wherever the destruction actually is: the front position is
 * broadcast so clients can render a travelling shockwave, and block dust,
 * rim bursts and a rolling boom are emitted at the front — even hundreds
 * of blocks away from the caster.
 */
public final class SeriousPunchFront {

    private static final double BROADCAST_RANGE = 512.0;

    private SeriousPunchFront() {}

    /**
     * Called once per destruction tick with the index of the first block that
     * has not been cleared yet; {@code axisOrigin} must be the same point the
     * block list was generated from so the front lands on the cylinder axis.
     */
    public static void advance(ServerLevel level, ServerPlayer caster, List<BlockPos> blocks, int index,
                               Vec3 axisOrigin, Vec3 direction, float radius) {
        if (blocks.isEmpty() || index >= blocks.size()) return;
        Vec3 front = frontPosition(blocks.get(index), axisOrigin, direction);
        long time = level.getGameTime();
        RandomSource random = level.random;

        if (time % 2 == 0) {
            NetworkRegister.sendToNearby(level, front, BROADCAST_RANGE, new SaitamaVfxPacket(
                    caster.getId(), front, direction, radius,
                    SaitamaVfxPacket.STYLE_SERIOUS_FRONT, 4));
        }

        // Dust ripped out of the terrain the front is about to swallow.
        int window = Math.min(blocks.size() - index, 700);
        for (int i = 0; i < 6; i++) {
            BlockPos pos = blocks.get(index + random.nextInt(window));
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            Vec3 out = Vec3.atCenterOf(pos).subtract(front);
            Vec3 radial = out.subtract(direction.scale(out.dot(direction)));
            radial = radial.lengthSqr() < 1.0e-3 ? new Vec3(0, 1, 0) : radial.normalize();
            Vec3 velocity = direction.scale(0.6 + random.nextDouble() * 0.6)
                    .add(radial.scale(0.4 + random.nextDouble() * 0.5))
                    .add(0, 0.15 + random.nextDouble() * 0.25, 0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    0, velocity.x, velocity.y, velocity.z, 1.0);
        }

        // Big puffs walking along the rim of the advancing disc.
        if (time % 3 == 0) {
            Vec3 side = perpendicular(direction);
            Vec3 up = side.cross(direction).normalize();
            double angle = random.nextDouble() * Math.PI * 2.0;
            Vec3 rim = front.add(side.scale(Math.cos(angle) * radius * 0.9))
                    .add(up.scale(Math.sin(angle) * radius * 0.9));
            level.sendParticles(ParticleTypes.EXPLOSION, rim.x, rim.y, rim.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.POOF, rim.x, rim.y, rim.z,
                    4, radius * 0.15, radius * 0.15, radius * 0.15, 0.05);
        }

        // Rolling boom that travels with the destruction.
        if (time % 8 == 0) {
            level.playSound(null, front.x, front.y, front.z, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 5.0f, 0.42f + random.nextFloat() * 0.2f);
        }
    }

    /** Terminal burst where the punch finally spends itself. */
    public static void finish(ServerLevel level, ServerPlayer caster, List<BlockPos> blocks,
                              Vec3 axisOrigin, Vec3 direction, float radius) {
        if (blocks.isEmpty()) return;
        Vec3 front = frontPosition(blocks.get(blocks.size() - 1), axisOrigin, direction);
        NetworkRegister.sendToNearby(level, front, BROADCAST_RANGE, new SaitamaVfxPacket(
                caster.getId(), front, direction, radius,
                SaitamaVfxPacket.STYLE_SERIOUS_FRONT_END, 0));
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, front.x, front.y, front.z,
                3, radius * 0.4, radius * 0.4, radius * 0.4, 0.0);
        level.playSound(null, front.x, front.y, front.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 7.0f, 0.38f);
        level.playSound(null, front.x, front.y, front.z, SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 2.5f, 0.6f);
    }

    /** Projects the block onto the cylinder axis so the front never wobbles sideways. */
    private static Vec3 frontPosition(BlockPos block, Vec3 axisOrigin, Vec3 direction) {
        double along = Math.max(0.0, Vec3.atCenterOf(block).subtract(axisOrigin).dot(direction));
        return axisOrigin.add(direction.scale(along));
    }

    private static Vec3 perpendicular(Vec3 direction) {
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.001) side = direction.cross(new Vec3(1, 0, 0));
        return side.normalize();
    }
}
