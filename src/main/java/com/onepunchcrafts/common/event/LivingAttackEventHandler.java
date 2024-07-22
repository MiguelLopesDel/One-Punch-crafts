package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class LivingAttackEventHandler {

    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
    public static void onSaitamaAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            saitamaWasAttacked(event, player);
        }
    }

    private static void saitamaWasAttacked(LivingAttackEvent event, ServerPlayer player) {
        player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            if (cap.isSaitama())
                event.setCanceled(true);
        });
    }
}
