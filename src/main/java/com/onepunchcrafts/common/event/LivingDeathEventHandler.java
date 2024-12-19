package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.damage.DamagesRegistry;
import com.onepunchcrafts.common.skills.SaitamaPack;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber
public class LivingDeathEventHandler {

    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        boolean saitamaIsTarget = false;
        if (event.getEntity() instanceof ServerPlayer player) {
            saitamaIsTarget = cancelDeathSaitama(event, player);
        }
        if (!saitamaIsTarget) {
            DamageSource source = event.getSource();
            if (source.is(DamagesRegistry.SERIOUS_PUNCH_SECOND)) {
                if (source.getEntity() instanceof ServerPlayer player && HelpUtility.verifyIsSaitamaAndGetCapability(player).isPresent()) {
                    event.setCanceled(false);
                }
            } else if (source.getDirectEntity() instanceof ServerPlayer player) {
                Optional<SaitamaPack> saitamaPack = HelpUtility.verifyIsSaitamaAndGetCapability(player);
                saitamaPack.ifPresent(cap -> {
                    if (cap.getCurrentSkill() == 2)
                        event.setCanceled(false);
                });
            }
        }
    }

    private static boolean cancelDeathSaitama(LivingDeathEvent event, ServerPlayer player) {
        Optional<SaitamaPack> onePunchPlayer = HelpUtility.verifyIsSaitamaAndGetCapability(player);
        onePunchPlayer.ifPresent(cap -> {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
        });
        return onePunchPlayer.isPresent();
    }
}
