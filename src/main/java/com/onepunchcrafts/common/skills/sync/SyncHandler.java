package com.onepunchcrafts.common.skills.sync;

import com.onepunchcrafts.common.skills.sync.processor.*;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SyncHandler {
    private static final Map<SyncStrategy, SyncProcessor> processors = Map.of(
            SyncStrategy.SIMPLE, new SimpleProcessor(),
            SyncStrategy.VALIDATED, new ValidatedProcessor(),
            SyncStrategy.TOGGLE, new ToggleProcessor(),
            SyncStrategy.SKILL_INDEX, new SkillIndexProcessor(),
            SyncStrategy.GROUP_INDEX, new GroupIndexProcessor(),
            SyncStrategy.SERVER_AUTHORITY, new ServerAuthorityProcessor()
    );

    public static void handleDifferences(ServerPlayer player, ArrayList<String> differences,
                                         SyncableSkillPack serverData, SyncableSkillPack clientData) {

        List<SyncableField> fields = FieldCache.getCachedFields(serverData.getClass());
        Map<String, SyncableField> fieldMap = fields.stream()
                .collect(java.util.stream.Collectors.toMap(SyncableField::getKey, f -> f));

        boolean requiresSync = false;

        for (String fieldKey : differences) {
            SyncableField field = fieldMap.get(fieldKey);
            if (field != null) {
                // Se encontrarmos um campo onde o servidor tem autoridade, marcamos para sincronizar no final
                if (field.getStrategy() == SyncStrategy.SERVER_AUTHORITY) {
                    requiresSync = true;
                }

                SyncProcessor processor = processors.get(field.getStrategy());
                if (processor != null) {
                    processor.process(player, field, serverData, clientData);
                }
            }
        }

        // Enviamos o pacote APENAS depois de processar todos os campos (incluindo a troca de skill)
        if (requiresSync) {
            NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
        }
    }
}