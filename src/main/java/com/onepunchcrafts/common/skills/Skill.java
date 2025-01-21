package com.onepunchcrafts.common.skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;

public interface Skill {

    void execute(Player player);

    default void flux(LivingEvent event) {}
}
