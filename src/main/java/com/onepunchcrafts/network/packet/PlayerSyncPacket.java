package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.packet.HandlerPlayerSyncPacket;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.network.NetworkRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
            serverLogic(ctx);
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HandlerPlayerSyncPacket.clientLogic(data));
        }
        ctx.get().setPacketHandled(true);
    }

    private void serverLogic(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null)
            return;
        player.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            ArrayList<String> differences = cap.compareTo(data);
            handleTheDifferences(player, differences, cap, this.data);
        });
    }

    private void handleTheDifferences(ServerPlayer player, ArrayList<String> differences, OnePunchPlayer serverData, OnePunchPlayer clientData) {
        differences.forEach(item -> {
            switch (item) {
                case "issaitama":
                    if (serverData.isSaitama())
                        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
                        break;
                case "actualability":
                    if (Math.abs(serverData.getActualAbility() - clientData.getActualAbility()) == 1)
                        serverData.setActualAbility(clientData.getActualAbility());
                    break;
                case "seriousfart":
                    if (serverData.isSaitama()) {
                        serverData.setSeriousFartActive(clientData.isSeriousFartActive());
                        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
                    }
                    break;
                case "superspeedsaitama":
                    if (serverData.isSaitama()) {
                        serverData.setSuperSpeed(clientData.isSuperSpeed());
                        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
                    }
                    break;
                case "breakblocksquickly":
                    if (serverData.isSaitama()) {
                        serverData.setBreakBlocksQuickly(clientData.isBreakBlocksQuickly());
                        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
                    }
                    break;
            }
        });
    }
}
