package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.event.LivingDamageEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpecialSkillPacket {
    public SpecialSkillPacket() {
    }

    public SpecialSkillPacket(FriendlyByteBuf friendlyByteBuf) {

    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {

    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            context.setPacketHandled(true);
        } else {
            sender.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
                if (cap.isSaitama() && cap.getActualAbility() == 2)
                    LivingDamageEventHandler.seriousPunchWithoutSpecificTargetWithClientEffects(sender, sender.serverLevel());
            });
        }
    }
}
