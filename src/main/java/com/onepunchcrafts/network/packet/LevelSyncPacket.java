package com.onepunchcrafts.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

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

    public void handle(CustomPayloadEvent.Context ctx) {
        Minecraft.getInstance().player.level().getCapability(WORLD_RULES_CAPABILITY).ifPresent(cap -> cap.readNBT(data));
        ctx.setPacketHandled(true);
    }
}
