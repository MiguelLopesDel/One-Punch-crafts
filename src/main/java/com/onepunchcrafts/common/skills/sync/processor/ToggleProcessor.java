package com.onepunchcrafts.common.skills.sync.processor;

import com.onepunchcrafts.common.skills.sync.FieldRegistry;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import net.minecraft.server.level.ServerPlayer;

public class ToggleProcessor implements SyncProcessor {
    @Override
    public void process(ServerPlayer player, String fieldKey,
                        SyncableSkillPack serverData, SyncableSkillPack clientData,
                        FieldRegistry.FieldDescriptor field) {
        Object clientValue = field.getValue(clientData);
        field.setValue(serverData, clientValue);

        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
    }
}