package com.onepunchcrafts.common.skills.sync.processor;

import com.onepunchcrafts.common.skills.sync.SyncableField;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import net.minecraft.server.level.ServerPlayer;

public class GroupIndexProcessor implements SyncProcessor {
    @Override
    public void process(ServerPlayer player, SyncableField field,
                        SyncableSkillPack serverData, SyncableSkillPack clientData) {
        int serverGroupIndex = serverData.getCurrentGroupIndex();
        int clientGroupIndex = clientData.getCurrentGroupIndex();

        int diff = Math.abs(serverGroupIndex - clientGroupIndex);
        int maxGroups = serverData.getSkills().size() - 1;

        if (diff == 1 || diff == maxGroups) {
            serverData.setCurrentGroupIndex(clientGroupIndex);
        }
    }
}