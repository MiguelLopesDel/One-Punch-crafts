package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.skills.SaitamaPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.onepunchcrafts.OnePunchCrafts.WITHOUT_PACK;

@Mod.EventBusSubscriber
public class PlayerChangedDimensionEventHandler {

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OnePunchPlayer onePunchPlayer = player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).orElse(new OnePunchPlayer(WITHOUT_PACK));
            if (onePunchPlayer.getSkillPack() instanceof SaitamaPack) player.removeAllEffects();
            HelpUtility.syncWithPlayer(player, onePunchPlayer);
        }
    }
}
