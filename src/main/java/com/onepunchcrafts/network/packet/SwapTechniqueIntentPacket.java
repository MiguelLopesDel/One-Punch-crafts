package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.runtime.state.PowerState;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A quick tap swaps the two most recently selected techniques. */
public final class SwapTechniqueIntentPacket {
    public SwapTechniqueIntentPacket() {}
    public SwapTechniqueIntentPacket(FriendlyByteBuf ignored) {}
    public void encode(FriendlyByteBuf ignored) {}

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PowerState state = HelpUtility.getSkillData(player).getPowerState();
            if (!state.powerSetId().equals(PowerState.NONE) && state.abilities().previousTechnique() != null) {
                try {
                    OnePunchRuntime.POWERS.swapPrevious(state);
                    SelectTechniqueIntentPacket.sendSelection(player, state);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        context.setPacketHandled(true);
    }
}
