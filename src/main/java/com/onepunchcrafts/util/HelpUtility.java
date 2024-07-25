package com.onepunchcrafts.util;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;

public class HelpUtility {

    /**
     * Verifica se o jogador tem a capacidade do mod e se ele é saitama.
     *
     * @param player
     * @return caso o jogador tenha a capacidade e seja um saitama é retorna a propria capacidade caso contrario
     * um optional empty;
     */
    public static Optional<OnePunchPlayer> verifyIsSaitamaAndGetCapability(ServerPlayer player) {
        LazyOptional<OnePunchPlayer> capability = player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY);
        return capability
                .filter(OnePunchPlayer::isSaitama);
    }
}
