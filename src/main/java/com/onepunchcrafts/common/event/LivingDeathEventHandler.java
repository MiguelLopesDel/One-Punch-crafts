package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.damage.DamagesRegistry;
import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.common.skills.saitama.SeriousPunch;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber
public class LivingDeathEventHandler {

    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        HelpUtility.passServerFluxToAllPlayers(event);
    }
}
