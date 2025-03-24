package com.onepunchcrafts.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.onepunchcrafts.OnePunchCrafts.WORLD_RULES_CAPABILITY;

public class LevelSyncPacket {

    private final CompoundTag data;

    public LevelSyncPacket(final CompoundTag data) {
        this.data = data;
    }


    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(data);
    }

    public LevelSyncPacket(final FriendlyByteBuf buffer) {
        this.data = buffer.readNbt();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        Minecraft.getInstance().player.level().getCapability(WORLD_RULES_CAPABILITY).ifPresent(cap -> cap.readNBT(data));
        ctx.get().setPacketHandled(true);
    }
}
