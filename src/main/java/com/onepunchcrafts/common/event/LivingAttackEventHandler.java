package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.skills.saitama.SeriousPunch;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class LivingAttackEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void saitamaAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            HelpUtility.getSkillData(player).manageFlux(event);
        }
    }
}