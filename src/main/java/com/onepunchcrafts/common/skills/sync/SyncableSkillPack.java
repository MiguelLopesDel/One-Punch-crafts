
package com.onepunchcrafts.common.skills.sync;

import com.onepunchcrafts.common.skills.AbstractSkillPack;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public abstract class SyncableSkillPack extends AbstractSkillPack {
    private static final Logger LOGGER = LogUtils.getLogger();
    private SyncableSkillPack lastSyncedState;

    @Override
    public ArrayList<String> compareTo(SkillPack otherData) {
        if (!this.getClass().equals(otherData.getClass())) {
            return new ArrayList<>();
        }

        try {
            return FieldComparator.compareFields(this, (SyncableSkillPack) otherData);
        } catch (Exception e) {
            LOGGER.error("Error comparing SyncableSkillPack fields", e);
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
        // Atualiza o snapshot para evitar reenviar o que acabamos de receber do cliente
        updateLastSyncedState();
    }

    public List<List<com.onepunchcrafts.common.skills.Skill>> getSkills() {
        return this.skills;
    }

    public void setCurrentGroupIndex(int currentGroupIndex) {
        this.currentGroupIndex = currentGroupIndex;
    }

    /**
     * Chamado ao final do tick do servidor para verificar se algo mudou e precisa ser enviado ao cliente.
     */
    public void tickSync(ServerPlayer player) {
        // 1. Se não tivermos um estado anterior gravado, gravamos agora e paramos.
        if (lastSyncedState == null) {
            updateLastSyncedState();
            return;
        }

        // 2. Compara o estado ATUAL do servidor com o ÚLTIMO enviado ao cliente
        ArrayList<String> differences = this.compareTo(lastSyncedState);

        // 3. Se houver diferenças, envia pacote
        if (!differences.isEmpty()) {
            NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(this));
            updateLastSyncedState();
        }
    }

    private void updateLastSyncedState() {
        try {
            this.lastSyncedState = this.getClass().getConstructor().newInstance();
            this.lastSyncedState.readNBT(this.writeNBT());
        } catch (Exception e) {
            LOGGER.error("Falha ao criar snapshot de sincronização para " + this.getClass().getSimpleName(), e);
        }
    }
}