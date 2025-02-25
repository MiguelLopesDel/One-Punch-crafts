package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ExtremeJump implements Skill {

    @Override
    public void execute(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            HelpUtility.verifyIsSaitamaAndGetCapability(serverPlayer).ifPresent(sai -> {
                sai.setExtremeJump(!sai.isExtremeJump());
                NetworkRegister.sendToPlayer(serverPlayer, new PlayerSyncPacket(sai));
            });
        }
    }
}
