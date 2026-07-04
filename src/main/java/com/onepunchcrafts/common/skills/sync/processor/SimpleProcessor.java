package com.onepunchcrafts.common.skills.sync.processor;

import com.onepunchcrafts.common.skills.sync.SyncableField;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import net.minecraft.server.level.ServerPlayer;

public class SimpleProcessor implements SyncProcessor {
    @Override
    public void process(ServerPlayer player, SyncableField field,
                        SyncableSkillPack serverData, SyncableSkillPack clientData) {
        Object clientValue = field.getValue(clientData);
        field.setValue(serverData, clientValue);
    }
}