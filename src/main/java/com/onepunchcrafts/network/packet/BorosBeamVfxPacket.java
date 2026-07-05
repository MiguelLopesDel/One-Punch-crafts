package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.render.BorosBeamVfxRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Lightweight beam VFX (quad geometry, no post chain) used by Boros' Energy
 * Projection and the normal Roaring Cannon. The style selects the palette and
 * proportions on the client.
 */
public class BorosBeamVfxPacket {

    public static final int STYLE_ENERGY_PROJECTION = 0;
    public static final int STYLE_ROARING_CANNON = 1;
    public static final int STYLE_ROARING_CANNON_METEORIC = 2;

    private final int casterId;
    private final Vec3 start;
    private final Vec3 direction;
    private final double range;
    private final int style;
    private final int lifeTicks;

    public BorosBeamVfxPacket(int casterId, Vec3 start, Vec3 direction, double range, int style, int lifeTicks) {
        this.casterId = casterId;
        this.start = start;
        this.direction = direction;
        this.range = range;
        this.style = style;
        this.lifeTicks = lifeTicks;
    }

    public BorosBeamVfxPacket(FriendlyByteBuf buf) {
        this.casterId = buf.readInt();
        this.start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.direction = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.range = buf.readDouble();
        this.style = buf.readByte();
        this.lifeTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(casterId);
        buf.writeDouble(start.x);
        buf.writeDouble(start.y);
        buf.writeDouble(start.z);
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
        buf.writeDouble(range);
        buf.writeByte(style);
        buf.writeInt(lifeTicks);
    }

    public static void handle(BorosBeamVfxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                BorosBeamVfxRenderer.addEffect(msg.casterId, msg.start, msg.direction, msg.range,
                        msg.style, msg.lifeTicks)));
        ctx.get().setPacketHandled(true);
    }
}
