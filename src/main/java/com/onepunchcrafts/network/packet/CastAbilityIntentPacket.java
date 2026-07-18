package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.OnePunchV3;
import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.PowerSetDefinition;
import com.onepunchcrafts.v3.minecraft.MinecraftPowerDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CastAbilityIntentPacket(Id abilityId) {
    public CastAbilityIntentPacket(FriendlyByteBuf buffer) { this(Id.parse(buffer.readUtf())); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeUtf(abilityId.toString()); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            var state = HelpUtility.getSkillData(player).getPowerState();
            try {
                PowerSetDefinition.LoadoutEntry entry = OnePunchV3.REGISTRIES.powerSets
                        .require(state.powerSetId()).requireEntry(abilityId);
                MinecraftPowerDispatcher.cast(player, abilityId);
                if (entry instanceof PowerSetDefinition.ToggleEntry toggle)
                    NetworkRegister.sendToPlayer(player, new PowerComponentDeltaPacket(
                            PowerComponentDeltaPacket.Kind.TAG, toggle.tag(), 0,
                            state.tags().contains(toggle.tag()), 0));
            } catch (IllegalArgumentException ignored) {}
        }
        context.setPacketHandled(true);
    }
}
