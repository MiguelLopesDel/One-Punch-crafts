package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.common.capability.OnePunchPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

public class PlayerSyncPacket {

    private final OnePunchPlayer data;

    public PlayerSyncPacket(final OnePunchPlayer data) {
        this.data = data;
    }

    public PlayerSyncPacket(final FriendlyByteBuf buffer) {
        (this.data = new OnePunchPlayer(false)).readNBT(buffer.readNbt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt((CompoundTag) data.writeNBT());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isServer()) {
            if (serverLogic(ctx)) return;

        } else clientLogic();
        ctx.get().setPacketHandled(true);
    }

    private void clientLogic() {
        LocalPlayer player = Minecraft.getInstance().player;
        player.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            cap.setSaitama(data.isSaitama());
            cap.setActualAbility(data.getActualAbility());
        });
    }

    private boolean serverLogic(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null)
            return true;
        player.getCapability(ONE_PLAYER_CAPABILITY, null).ifPresent(cap -> {
            ArrayList<String> differences = cap.compareTo(data);
            handleTheDifferences(differences, cap, this.data);
        });
        return false;
    }

    private void handleTheDifferences(ArrayList<String> differences, OnePunchPlayer serverData, OnePunchPlayer clientData) {
        differences.forEach(item -> {
            switch (item) {
                case "issaitama":
                    if (serverData.isSaitama())
                        //TO DO enviar uma mensagem para o cliente para avisar de que ele é saitama
                        break;
                case "actualability":
                    if (Math.abs(serverData.getActualAbility() - clientData.getActualAbility()) == 1)
                        serverData.setActualAbility(clientData.getActualAbility());
            }
        });
    }
}
