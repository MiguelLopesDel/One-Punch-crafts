package com.onepunchcrafts.common.skills;

import net.minecraft.world.entity.player.Player;

public interface SkillPassive extends Skill {

    default void tick(Player player) {

    }
}
