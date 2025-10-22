package com.onepunchcrafts.common.skills.sync.processor;

import com.onepunchcrafts.common.skills.sync.FieldRegistry;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import net.minecraft.server.level.ServerPlayer;

public class SimpleProcessor implements SyncProcessor {
    @Override
    public void process(ServerPlayer player, String fieldKey,
                        SyncableSkillPack serverData, SyncableSkillPack clientData,
                        FieldRegistry.FieldDescriptor field) {
        Object clientValue = field.getValue(clientData);
        field.setValue(serverData, clientValue);
    }
}