package com.onepunchcrafts.common.dimension;

import com.onepunchcrafts.common.vfx.SeriousPunchFront;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AnimationPacket;
import com.onepunchcrafts.network.packet.DimensionalPunchVfxPacket;
import com.onepunchcrafts.util.TickScheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/** Owns the presentation beats emitted when a Dimensional Punch starts. */
public final class DimensionalPunchPresentation {
    static final int ANIMATION_TICKS = 24;

    private DimensionalPunchPresentation() {}

    record Cast(int instanceId, Vec3 fist, Vec3 direction, int windupTicks) {
        Vec3 rift() {
            return fist.add(direction.scale(2.0));
        }
    }

    interface Sink {
        void playPunchAnimation();
        void startCinematic(Cast cast);
        void showServerFallback(Cast cast);
        void showImpactFallback(Cast cast);
    }

    static void begin(Sink sink, Cast cast) {
        sink.playPunchAnimation();
        sink.startCinematic(cast);
        sink.showServerFallback(cast);
    }

    static void impact(Sink sink, Cast cast) {
        sink.showImpactFallback(cast);
    }

    public static void begin(ServerPlayer player, Vec3 fist, Vec3 direction, int windupTicks) {
        Vec3 normalized = direction.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : direction.normalize();
        begin(new MinecraftSink(player), new Cast(
                SeriousPunchFront.nextInstanceId(), fist, normalized, windupTicks));
    }

    public static void impact(ServerPlayer player, Vec3 fist, Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : direction.normalize();
        impact(new MinecraftSink(player), new Cast(0, fist, normalized, 1));
    }

    private record MinecraftSink(ServerPlayer player) implements Sink {
        @Override
        public void playPunchAnimation() {
            NetworkRegister.sendToAllClients(new AnimationPacket(player.getStringUUID(), "punch_animation"));
            TickScheduler.scheduleFromHere(Duration.of(ANIMATION_TICKS * 50L, ChronoUnit.MILLIS),
                    () -> NetworkRegister.sendToAllClients(new AnimationPacket(player.getStringUUID(), "stop")));
        }

        @Override
        public void startCinematic(Cast cast) {
            DimensionalPunchVfxPacket.broadcast(player.serverLevel(), new DimensionalPunchVfxPacket(
                    cast.instanceId(), cast.fist(), cast.direction(), cast.windupTicks()));
        }

        @Override
        public void showServerFallback(Cast cast) {
            ServerLevel level = player.serverLevel();
            Vec3 rift = cast.rift();
            level.sendParticles(ParticleTypes.PORTAL, rift.x, rift.y, rift.z,
                    36, 1.2, 1.2, 1.2, 0.18);
            level.sendParticles(ParticleTypes.END_ROD, cast.fist().x, cast.fist().y, cast.fist().z,
                    16, 0.25, 0.25, 0.25, 0.08);
        }

        @Override
        public void showImpactFallback(Cast cast) {
            ServerLevel level = player.serverLevel();
            Vec3 rift = cast.rift();
            level.sendParticles(ParticleTypes.FLASH, rift.x, rift.y, rift.z,
                    1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SONIC_BOOM, rift.x, rift.y, rift.z,
                    1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, rift.x, rift.y, rift.z,
                    96, 1.7, 2.2, 1.7, 0.35);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, rift.x, rift.y, rift.z,
                    52, 1.5, 1.8, 1.5, 0.28);
            level.sendParticles(ParticleTypes.END_ROD, rift.x, rift.y, rift.z,
                    30, 1.1, 1.4, 1.1, 0.2);
        }
    }
}
