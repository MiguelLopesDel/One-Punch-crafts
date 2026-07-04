package com.onepunchcrafts.common.skills.sync.processor;

import com.onepunchcrafts.common.skills.sync.SyncableField;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import net.minecraft.server.level.ServerPlayer;

public interface SyncProcessor {
    void process(ServerPlayer player, SyncableField field, SyncableSkillPack serverData, SyncableSkillPack clientData);
}