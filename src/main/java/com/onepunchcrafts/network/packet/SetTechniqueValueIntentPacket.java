package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Absolute value intent emitted by the radial wheel's adjustment slider. */
public record SetTechniqueValueIntentPacket(Id techniqueId, double value) {
    public SetTechniqueValueIntentPacket(FriendlyByteBuf buffer) {
        this(Id.parse(buffer.readUtf()), buffer.readDouble());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(techniqueId.toString());
        buffer.writeDouble(value);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && Double.isFinite(value)) {
            var state = HelpUtility.getSkillData(player).getPowerState();
            try {
                Technique technique = OnePunchRuntime.REGISTRIES.techniques.require(
                        OnePunchRuntime.REGISTRIES.powerSets.require(state.powerSetId())
                                .requireTechnique(techniqueId));
                double applied = OnePunchRuntime.POWERS.setAdjustment(state, techniqueId, value);
                if (technique.activeAction() instanceof Technique.ActiveAction.Adjust adjustable
                        && !Double.isNaN(applied))
                    NetworkRegister.sendToPlayer(player, new PowerComponentDeltaPacket(
                            PowerComponentDeltaPacket.Kind.ATTRIBUTE, adjustable.attribute(), applied, false, 0));
            } catch (IllegalArgumentException ignored) {}
        }
        context.setPacketHandled(true);
    }
}
