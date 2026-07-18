package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.runtime.state.PowerState;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The wheel sends an identity; the server validates ownership before selecting it. */
public record SelectTechniqueIntentPacket(Id techniqueId) {
    public SelectTechniqueIntentPacket(FriendlyByteBuf buffer) { this(Id.parse(buffer.readUtf())); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeUtf(techniqueId.toString()); }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PowerState state = HelpUtility.getSkillData(player).getPowerState();
            if (!state.powerSetId().equals(PowerState.NONE)) {
                try {
                    OnePunchRuntime.POWERS.select(state, techniqueId);
                    sendSelection(player, state);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        context.setPacketHandled(true);
    }

    static void sendSelection(ServerPlayer player, PowerState state) {
        NetworkRegister.sendToPlayer(player, new PowerComponentDeltaPacket(
                PowerComponentDeltaPacket.Kind.SELECTION, state.abilities().selectedTechnique(), 0, false,
                state.abilities().selectedPage()));
    }
}
