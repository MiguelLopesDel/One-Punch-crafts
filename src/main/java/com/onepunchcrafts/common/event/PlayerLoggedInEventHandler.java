package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerLoggedInEventHandler {

    @SubscribeEvent
    public static void onPlayerLogging(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (!player.level().isClientSide()) {
            OnePunchPlayer onePunchPlayer = player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).orElse(new OnePunchPlayer(false));
            NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(onePunchPlayer));
        }
    }
}
