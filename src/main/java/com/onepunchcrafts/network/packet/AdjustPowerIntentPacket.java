package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.OnePunchV3;
import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.PowerSetDefinition;
import com.onepunchcrafts.v3.core.state.PowerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AdjustPowerIntentPacket(Id entryId, int direction) {
    public AdjustPowerIntentPacket(FriendlyByteBuf buffer) { this(Id.parse(buffer.readUtf()), buffer.readByte()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeUtf(entryId.toString()); buffer.writeByte(Integer.signum(direction)); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && direction != 0) {
            PowerState state = HelpUtility.getSkillData(player).getPowerState();
            try {
                PowerSetDefinition.LoadoutEntry entry = OnePunchV3.REGISTRIES.powerSets.require(state.powerSetId()).requireEntry(entryId);
                double value = OnePunchV3.POWERS.adjust(state, entryId, direction);
                if (entry instanceof PowerSetDefinition.AdjustableEntry adjustable && !Double.isNaN(value))
                    NetworkRegister.sendToPlayer(player, new PowerComponentDeltaPacket(
                            PowerComponentDeltaPacket.Kind.ATTRIBUTE, adjustable.attribute(), value, false, 0));
            } catch (IllegalArgumentException ignored) {}
        }
        context.setPacketHandled(true);
    }
}
