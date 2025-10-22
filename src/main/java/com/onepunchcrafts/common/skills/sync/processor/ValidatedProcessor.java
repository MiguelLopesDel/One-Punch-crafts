package com.onepunchcrafts.common.skills.sync.processor;

import com.onepunchcrafts.common.skills.sync.FieldRegistry;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import net.minecraft.server.level.ServerPlayer;

public class ValidatedProcessor implements SyncProcessor {
    @Override
    public void process(ServerPlayer player, String fieldKey,
                        SyncableSkillPack serverData, SyncableSkillPack clientData,
                        FieldRegistry.FieldDescriptor field) {
        Object clientValue = field.getValue(clientData);

        if (clientValue instanceof Short shortValue) {
            boolean isInvalid = shortValue < 0;
            field.setValue(serverData, isInvalid ? (short) 0 : shortValue);

            if (isInvalid) {
                NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
            }
        } else {
            field.setValue(serverData, clientValue);
        }
    }
}