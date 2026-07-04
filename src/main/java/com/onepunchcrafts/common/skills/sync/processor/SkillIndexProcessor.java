package com.onepunchcrafts.common.skills.sync.processor;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.sync.SyncableField;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class SkillIndexProcessor implements SyncProcessor {

    @Override
    public void process(ServerPlayer player, SyncableField field,
                        SyncableSkillPack serverData, SyncableSkillPack clientData) {
        int serverSkillIndex = (int) field.getValue(serverData);
        int clientSkillIndex = (int) field.getValue(clientData);

        List<Skill> currentGroup = serverData.getSkills().get(serverData.getCurrentGroupIndex());
        int lastSkill = currentGroup.size() - 1;

        int diff = Math.abs(serverSkillIndex - clientSkillIndex);
        boolean validDiff = diff == 1 || diff == lastSkill;
        boolean groupChanged = serverData.getCurrentGroupIndex() != clientData.getCurrentGroupIndex();

        if (validDiff || groupChanged) {
            field.setValue(serverData, clientSkillIndex);
        }
    }
}