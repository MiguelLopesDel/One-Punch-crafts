package com.onepunchcrafts.common.skills.sync.processor;

import com.onepunchcrafts.common.skills.sync.SyncableField;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import net.minecraft.server.level.ServerPlayer;

public class ServerAuthorityProcessor implements SyncProcessor {
    @Override
    public void process(ServerPlayer player, SyncableField field, SyncableSkillPack serverData, SyncableSkillPack clientData) {
        // Não fazemos nada aqui. O valor do cliente é ignorado.
        // A sincronização de volta para o cliente (correção) será tratada pelo SyncHandler após processar todos os campos.
    }
}