package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.render.SeriousPunchCinematic;
import com.onepunchcrafts.network.NetworkRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Kicks off the client-side Serious Punch cinematic: the whole timeline
 * (windup hush, razor line, pressure cone, impact, aftermath) runs
 * deterministically on each client from this single packet.
 */
public class SeriousPunchVfxPacket {

    private static final double BROADCAST_RANGE = 384.0D;

    private final int casterId;
    private final Vec3 origin;
    private final Vec3 direction;
    private final int windupTicks;

    public SeriousPunchVfxPacket(int casterId, Vec3 origin, Vec3 direction, int windupTicks) {
        this.casterId = casterId;
        this.origin = origin;
        this.direction = direction;
        this.windupTicks = windupTicks;
    }

    public SeriousPunchVfxPacket(FriendlyByteBuf buf) {
        this.casterId = buf.readInt();
        this.origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.direction = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.windupTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(casterId);
        buf.writeDouble(origin.x);
        buf.writeDouble(origin.y);
        buf.writeDouble(origin.z);
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
        buf.writeInt(windupTicks);
    }

    public static void handle(SeriousPunchVfxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SeriousPunchCinematic.start(msg.casterId, msg.origin, msg.direction, msg.windupTicks)));
        ctx.get().setPacketHandled(true);
    }

    public static void broadcast(ServerLevel level, SeriousPunchVfxPacket packet) {
        NetworkRegister.sendToNearby(level, packet.origin, BROADCAST_RANGE, packet);
    }
}
