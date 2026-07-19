package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.presentation.VfxProfile;
import com.onepunchcrafts.content.SaitamaContent;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client intent; the server owns the performer's per-Technique VFX selection. */
public record SetVfxPreferenceIntentPacket(Id techniqueId, VfxProfile profile) {
    public SetVfxPreferenceIntentPacket(FriendlyByteBuf buffer) {
        this(Id.parse(buffer.readUtf()), buffer.readEnum(VfxProfile.class));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(techniqueId.toString());
        buffer.writeEnum(profile);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && SaitamaContent.VFX_TECHNIQUES.contains(techniqueId)) {
            HelpUtility.getSkillData(player).getPowerState().vfxPreferences().set(techniqueId, profile);
            NetworkRegister.sendToPlayer(player, new VfxPreferenceDeltaPacket(techniqueId, profile));
        }
        context.setPacketHandled(true);
    }
}
