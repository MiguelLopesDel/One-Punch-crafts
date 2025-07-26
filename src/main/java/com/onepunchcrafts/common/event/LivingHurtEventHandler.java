package com.onepunchcrafts.common.event;

import com.onepunchcrafts.util.HelpUtility;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class LivingHurtEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void saitamaOnAttack(LivingHurtEvent event) {
        HelpUtility.passServerFluxToAllPlayers(event);
    }
}