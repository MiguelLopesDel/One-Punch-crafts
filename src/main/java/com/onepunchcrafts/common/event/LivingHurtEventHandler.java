package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.capability.OnePunchPlayer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

import static com.mojang.text2speech.Narrator.LOGGER;
import static com.onepunchcrafts.util.HelpUtility.verifyIsSaitamaAndGetCapability;

@Mod.EventBusSubscriber
public class LivingHurtEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void saitamaOnAttack(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            Optional<OnePunchPlayer> onePunchPlayer = verifyIsSaitamaAndGetCapability(player);
            onePunchPlayer.ifPresent(cap -> {
                if (cap.getActualAbility() == 4) {
                    LivingEntity target = event.getEntity();
                    float amount = target.getHealth() - 1;
                    event.setAmount(Math.min(amount, 100_000));
                }
            });
        }

    }
}
