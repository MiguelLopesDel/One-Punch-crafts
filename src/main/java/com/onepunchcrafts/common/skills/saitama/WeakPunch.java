package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

public class WeakPunch implements Skill {

    @Override
    public void execute(Player player) {

    }

    @Override
    public void flux(LivingEvent event) {
        if (event instanceof LivingDamageEvent damageEvent && damageEvent.getSource().getEntity() instanceof ServerPlayer) {
            damageEvent.setAmount(damageEvent.getAmount() * 100_000);
        }
    }
}
