package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.presentation.VfxProfile;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Authoritative acknowledgement of one presentation preference. */
public record VfxPreferenceDeltaPacket(Id techniqueId, VfxProfile profile) {
    public VfxPreferenceDeltaPacket(FriendlyByteBuf buffer) {
        this(Id.parse(buffer.readUtf()), buffer.readEnum(VfxProfile.class));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(techniqueId.toString());
        buffer.writeEnum(profile);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().player != null)
                HelpUtility.getSkillData(Minecraft.getInstance().player).getPowerState()
                        .vfxPreferences().set(techniqueId, profile);
        });
        supplier.get().setPacketHandled(true);
    }
}
