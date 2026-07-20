package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.minecraft.PowerStateCodec;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PowerStateSnapshotPacket;
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
            // The capability survives the trip server-side (Forge keeps caps on
            // normal dimension travel), but the client rebuilds a fresh, empty
            // one — so re-push both the legacy pack AND the v3 power state, the
            // same pair the login/respawn paths send. Without the snapshot the
            // client "stops being Saitama" even though the server still is.
            HelpUtility.syncWithPlayer(player, onePunchPlayer);
            NetworkRegister.sendToPlayer(player,
                    new PowerStateSnapshotPacket(PowerStateCodec.encode(onePunchPlayer.getPowerState())));
        }
    }
}
