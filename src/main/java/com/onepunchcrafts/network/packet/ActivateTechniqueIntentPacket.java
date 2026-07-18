package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.minecraft.MinecraftPowerDispatcher;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ActivateTechniqueIntentPacket(Id techniqueId) {
    public ActivateTechniqueIntentPacket(FriendlyByteBuf buffer) { this(Id.parse(buffer.readUtf())); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeUtf(techniqueId.toString()); }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            var state = HelpUtility.getSkillData(player).getPowerState();
            try {
                OnePunchRuntime.REGISTRIES.powerSets.require(state.powerSetId()).requireTechnique(techniqueId);
                Technique technique = OnePunchRuntime.REGISTRIES.techniques.require(techniqueId);
                MinecraftPowerDispatcher.activate(player, techniqueId);
                if (technique.activeAction() instanceof Technique.ActiveAction.Toggle toggle)
                    NetworkRegister.sendToPlayer(player, new PowerComponentDeltaPacket(
                            PowerComponentDeltaPacket.Kind.TAG, toggle.tag(), 0,
                            state.tags().contains(toggle.tag()), 0));
            } catch (IllegalArgumentException ignored) {}
        }
        context.setPacketHandled(true);
    }
}
