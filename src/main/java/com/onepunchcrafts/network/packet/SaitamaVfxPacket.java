package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.render.SaitamaVfxRenderer;
import com.onepunchcrafts.network.NetworkRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Quad-based VFX for Saitama's punches and movement. The style selects the
 * shape and palette on the client; scale stretches the effect (impact size,
 * dash length or trail speed depending on the style).
 */
public class SaitamaVfxPacket {

    public static final int STYLE_PUNCH_IMPACT = 0;
    public static final int STYLE_BARRAGE = 1;
    // style 2 was the old Serious Punch quad burst, superseded by SeriousPunchCinematic
    public static final int STYLE_DASH = 3;
    public static final int STYLE_SPEED_TRAIL = 4;

    private static final double BROADCAST_RANGE = 256.0D;

    private final int casterId;
    private final Vec3 pos;
    private final Vec3 direction;
    private final float scale;
    private final int style;
    private final int lifeTicks;

    public SaitamaVfxPacket(int casterId, Vec3 pos, Vec3 direction, float scale, int style, int lifeTicks) {
        this.casterId = casterId;
        this.pos = pos;
        this.direction = direction;
        this.scale = scale;
        this.style = style;
        this.lifeTicks = lifeTicks;
    }

    public SaitamaVfxPacket(FriendlyByteBuf buf) {
        this.casterId = buf.readInt();
        this.pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.direction = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.scale = buf.readFloat();
        this.style = buf.readByte();
        this.lifeTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(casterId);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
        buf.writeFloat(scale);
        buf.writeByte(style);
        buf.writeInt(lifeTicks);
    }

    public static void handle(SaitamaVfxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SaitamaVfxRenderer.addEffect(msg.casterId, msg.pos, msg.direction, msg.scale,
                        msg.style, msg.lifeTicks)));
        ctx.get().setPacketHandled(true);
    }

    /** Send to every client of the level close enough to see the effect. */
    public static void broadcast(ServerLevel level, SaitamaVfxPacket packet) {
        NetworkRegister.sendToNearby(level, packet.pos, BROADCAST_RANGE, packet);
    }
}
