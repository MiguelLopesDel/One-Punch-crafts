package com.onepunchcrafts.common.skills.sync.processor;

import com.onepunchcrafts.common.skills.sync.FieldRegistry;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import net.minecraft.server.level.ServerPlayer;

public class GroupIndexProcessor implements SyncProcessor {
    @Override
    public void process(ServerPlayer player, String fieldKey,
                        SyncableSkillPack serverData, SyncableSkillPack clientData,
                        FieldRegistry.FieldDescriptor field) {
        int serverGroupIndex = (int) field.getValue(serverData);
        int clientGroupIndex = (int) field.getValue(clientData);

        int diff = Math.abs(serverGroupIndex - clientGroupIndex);
        int maxGroups = serverData.getSkills().size() - 1;

        if (diff == 1 || diff == maxGroups) {
            field.setValue(serverData, clientGroupIndex);
        }
    }
}