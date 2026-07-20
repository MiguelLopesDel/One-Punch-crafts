package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.render.DimensionalPunchCinematic;
import com.onepunchcrafts.network.NetworkRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Starts the client-side Dimensional Punch cinematic: the fist tears space, a
 * jagged rift cracks open at the impact point and the world briefly refracts
 * through the shattered dimension (the screen half lives in the post chain).
 */
public class DimensionalPunchVfxPacket {

    private static final double BROADCAST_RANGE = 256.0D;

    private final int instanceId;
    private final Vec3 origin;
    private final Vec3 direction;
    private final int windupTicks;

    public DimensionalPunchVfxPacket(int instanceId, Vec3 origin, Vec3 direction, int windupTicks) {
        this.instanceId = instanceId;
        this.origin = origin;
        this.direction = direction;
        this.windupTicks = windupTicks;
    }

    public DimensionalPunchVfxPacket(FriendlyByteBuf buf) {
        this.instanceId = buf.readInt();
        this.origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.direction = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.windupTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(instanceId);
        buf.writeDouble(origin.x);
        buf.writeDouble(origin.y);
        buf.writeDouble(origin.z);
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
        buf.writeInt(windupTicks);
    }

    public static void handle(DimensionalPunchVfxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DimensionalPunchCinematic.start(msg.instanceId, msg.origin, msg.direction, msg.windupTicks)));
        ctx.get().setPacketHandled(true);
    }

    public static void broadcast(ServerLevel level, DimensionalPunchVfxPacket packet) {
        NetworkRegister.sendToNearby(level, packet.origin, BROADCAST_RANGE, packet);
    }
}
