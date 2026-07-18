package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.api.Id;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One server-owned component field; normal play never sends PowerState back upstream. */
public record PowerComponentDeltaPacket(Kind kind, Id key, double number, boolean flag, int page) {
    public PowerComponentDeltaPacket(FriendlyByteBuf buffer) {
        this(buffer.readEnum(Kind.class), Id.parse(buffer.readUtf()), buffer.readDouble(), buffer.readBoolean(), buffer.readVarInt());
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(kind);
        buffer.writeUtf(key.toString());
        buffer.writeDouble(number);
        buffer.writeBoolean(flag);
        buffer.writeVarInt(page);
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().player == null) return;
            var state = HelpUtility.getSkillData(Minecraft.getInstance().player).getPowerState();
            switch (kind) {
                case SELECTION -> { state.abilities().selectedPage(page); state.abilities().select(key); }
                case ATTRIBUTE -> state.attributes().setBase(key, number);
                case TAG -> { if (flag && !state.tags().contains(key)) state.tags().add(key); else if (!flag && state.tags().contains(key)) state.tags().remove(key); }
            }
            state.consumeDirty();
        });
        supplier.get().setPacketHandled(true);
    }
    public enum Kind { SELECTION, ATTRIBUTE, TAG }
}
