
package com.onepunchcrafts.common.skills.sync;

import com.onepunchcrafts.common.skills.AbstractSkillPack;
import com.onepunchcrafts.common.skills.SkillPack;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public abstract class SyncableSkillPack extends AbstractSkillPack {

    @Override
    public ArrayList<String> compareTo(SkillPack otherData) {
        if (!this.getClass().equals(otherData.getClass())) {
            return new ArrayList<>();
        }

        try {
            return FieldComparator.compareFields(this, (SyncableSkillPack) otherData);
        } catch (Exception e) {
            System.out.println(e);
            throw e;
        }
    }

    @Override
    public void handleTheDifferences(ServerPlayer player, ArrayList<String> differences,
                                     SkillPack serverLayer, SkillPack clientLayer) {
        if (!(serverLayer instanceof SyncableSkillPack serverData) ||
                !(clientLayer instanceof SyncableSkillPack clientData)) {
            return;
        }

        SyncHandler.handleDifferences(player, differences, serverData, clientData);
    }

    public List<List<com.onepunchcrafts.common.skills.Skill>> getSkills() {
        return this.skills;
    }

    public void setCurrentGroupIndex(int currentGroupIndex) {
        this.currentGroupIndex = currentGroupIndex;
    }
}