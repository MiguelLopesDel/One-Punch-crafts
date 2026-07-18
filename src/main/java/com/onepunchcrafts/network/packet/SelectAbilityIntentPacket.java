package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.OnePunchV3;
import com.onepunchcrafts.v3.core.state.PowerState;
import com.onepunchcrafts.v3.minecraft.PowerStateCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client sends direction only; the server owns and validates selection. */
public record SelectAbilityIntentPacket(int direction, boolean group) {
    public SelectAbilityIntentPacket(FriendlyByteBuf buffer) { this(buffer.readByte(), buffer.readBoolean()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeByte(Integer.signum(direction)); buffer.writeBoolean(group); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PowerState state = HelpUtility.getSkillData(player).getPowerState();
            if (!state.powerSetId().equals(PowerState.NONE) && direction != 0) {
                if (group) OnePunchV3.POWERS.selectGroup(state, direction);
                else OnePunchV3.POWERS.selectRelative(state, direction);
                NetworkRegister.sendToPlayer(player, new PowerComponentDeltaPacket(
                        PowerComponentDeltaPacket.Kind.SELECTION, state.abilities().selectedAbility(), 0, false,
                        state.abilities().selectedGroup()));
            }
        }
        context.setPacketHandled(true);
    }
}
