package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.minecraft.PowerStateCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-authoritative component snapshot (login/identity changes/recovery). */
public record PowerStateSnapshotPacket(CompoundTag state) {
    public PowerStateSnapshotPacket(FriendlyByteBuf buffer) { this(buffer.readNbt()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeNbt(state); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().player != null && state != null)
                PowerStateCodec.decodeInto(state, HelpUtility.getSkillData(Minecraft.getInstance().player).getPowerState());
        });
        supplier.get().setPacketHandled(true);
    }
}
