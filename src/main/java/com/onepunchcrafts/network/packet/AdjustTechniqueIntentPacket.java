package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.runtime.state.PowerState;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AdjustTechniqueIntentPacket(Id techniqueId, int direction) {
    public AdjustTechniqueIntentPacket(FriendlyByteBuf buffer) { this(Id.parse(buffer.readUtf()), buffer.readByte()); }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(techniqueId.toString());
        buffer.writeByte(Integer.signum(direction));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && direction != 0) {
            PowerState state = HelpUtility.getSkillData(player).getPowerState();
            try {
                Technique technique = OnePunchRuntime.REGISTRIES.techniques.require(
                        OnePunchRuntime.REGISTRIES.powerSets.require(state.powerSetId())
                                .requireTechnique(techniqueId));
                double value = OnePunchRuntime.POWERS.adjust(state, techniqueId, direction);
                if (technique.activeAction() instanceof Technique.ActiveAction.Adjust adjustable
                        && !Double.isNaN(value))
                    NetworkRegister.sendToPlayer(player, new PowerComponentDeltaPacket(
                            PowerComponentDeltaPacket.Kind.ATTRIBUTE, adjustable.attribute(), value, false, 0));
            } catch (IllegalArgumentException ignored) {}
        }
        context.setPacketHandled(true);
    }
}
