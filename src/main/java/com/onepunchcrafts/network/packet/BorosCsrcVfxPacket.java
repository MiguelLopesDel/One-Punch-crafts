package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.render.BorosCsrcVfxRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BorosCsrcVfxPacket {
    private final int casterId;
    private final Vec3 start;
    private final Vec3 direction;
    private final double range;
    private final int chargeTicks;
    private final int fireTicks;
    private final Vec3 impact;
    private final double impactRadius;

    public BorosCsrcVfxPacket(int casterId, Vec3 start, Vec3 direction, double range,
                              int chargeTicks, int fireTicks, Vec3 impact, double impactRadius) {
        this.casterId = casterId;
        this.start = start;
        this.direction = direction;
        this.range = range;
        this.chargeTicks = chargeTicks;
        this.fireTicks = fireTicks;
        this.impact = impact;
        this.impactRadius = impactRadius;
    }

    public BorosCsrcVfxPacket(FriendlyByteBuf buf) {
        this.casterId = buf.readInt();
        this.start = readVec3(buf);
        this.direction = readVec3(buf);
        this.range = buf.readDouble();
        this.chargeTicks = buf.readInt();
        this.fireTicks = buf.readInt();
        this.impact = readVec3(buf);
        this.impactRadius = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(casterId);
        writeVec3(buf, start);
        writeVec3(buf, direction);
        buf.writeDouble(range);
        buf.writeInt(chargeTicks);
        buf.writeInt(fireTicks);
        writeVec3(buf, impact);
        buf.writeDouble(impactRadius);
    }

    public static void handle(BorosCsrcVfxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                BorosCsrcVfxRenderer.addEffect(msg.casterId, msg.start, msg.direction, msg.range,
                        msg.chargeTicks, msg.fireTicks, msg.impact, msg.impactRadius)));
        ctx.get().setPacketHandled(true);
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void writeVec3(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }
}
