package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.render.SaitamaTechniqueVfxRenderer;
import com.onepunchcrafts.network.NetworkRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Semantic, presentation-only events for Saitama techniques other than the A/B punch family. */
public final class SaitamaTechniqueVfxPacket {
    public static final int WEAKENING = 0;
    public static final int QUICK_BACKSTAB = 1;
    public static final int AREA_ROUTE = 2;
    public static final int SERIOUS_FART = 3;
    public static final int WEIGHT = 4;
    public static final int KNOCKBACK_RESIST = 5;
    public static final int ATTACK_KNOCKBACK = 6;
    public static final int SWIM_WAKE = 7;
    public static final int BREAK_BLOCK = 8;
    public static final int EXTREME_SPEED = 9;
    public static final int EXTREME_JUMP = 10;
    public static final int DASH = 11;

    private static final double BROADCAST_RANGE = 192.0;

    public final int casterId;
    public final Vec3 origin;
    public final Vec3 direction;
    public final float scale;
    public final int style;
    public final int lifeTicks;

    public SaitamaTechniqueVfxPacket(int casterId, Vec3 origin, Vec3 direction,
                                     float scale, int style, int lifeTicks) {
        this.casterId = casterId;
        this.origin = origin;
        this.direction = direction;
        this.scale = scale;
        this.style = style;
        this.lifeTicks = lifeTicks;
    }

    public SaitamaTechniqueVfxPacket(FriendlyByteBuf buf) {
        casterId = buf.readVarInt();
        origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        direction = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        scale = buf.readFloat();
        style = buf.readVarInt();
        lifeTicks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(casterId);
        buf.writeDouble(origin.x);
        buf.writeDouble(origin.y);
        buf.writeDouble(origin.z);
        buf.writeFloat((float) direction.x);
        buf.writeFloat((float) direction.y);
        buf.writeFloat((float) direction.z);
        buf.writeFloat(scale);
        buf.writeVarInt(style);
        buf.writeVarInt(lifeTicks);
    }

    public static void handle(SaitamaTechniqueVfxPacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SaitamaTechniqueVfxRenderer.addEffect(message.casterId, message.origin,
                        message.direction, message.scale, message.style, message.lifeTicks)));
        context.get().setPacketHandled(true);
    }

    public static void broadcast(ServerLevel level, SaitamaTechniqueVfxPacket packet) {
        NetworkRegister.sendToNearby(level, packet.origin, BROADCAST_RANGE, packet);
    }
}
