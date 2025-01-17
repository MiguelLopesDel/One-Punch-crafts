package com.onepunchcrafts.common.skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface Skill {

    void execute(Player player);
}
