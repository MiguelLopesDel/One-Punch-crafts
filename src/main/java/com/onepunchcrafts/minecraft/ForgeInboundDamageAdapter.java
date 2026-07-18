package com.onepunchcrafts.minecraft;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.runtime.combat.DamagePipeline;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** VanillaInbound Adapter for damage entering a runtime-owned target. */
@Mod.EventBusSubscriber
public final class ForgeInboundDamageAdapter {
    private ForgeInboundDamageAdapter() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void attack(LivingAttackEvent event) { protect(event.getEntity(), event); }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void hurt(LivingHurtEvent event) { protect(event.getEntity(), event); }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void damage(LivingDamageEvent event) { protect(event.getEntity(), event); }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isSaitama(player)) {
            player.setHealth(player.getMaxHealth());
            event.setCanceled(true);
        }
    }

    private static void protect(net.minecraft.world.entity.LivingEntity target, net.minecraftforge.eventbus.api.Event event) {
        if (target instanceof ServerPlayer player && isSaitama(player)) event.setCanceled(true);
    }

    private static boolean isSaitama(ServerPlayer player) {
        return HelpUtility.getSkillData(player).getPowerState().tags().contains(DamagePipeline.SAITAMA_TARGET);
    }
}
