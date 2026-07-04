package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.skills.saitama.NormalPunch;
import com.onepunchcrafts.common.skills.saitama.SeriousPunch;
import com.onepunchcrafts.common.skills.saitama.WeakPunch;
import com.onepunchcrafts.common.skills.saitama.WeakeningPunch;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event relay for the punch skills. The skill classes themselves cannot be
 * {@code @Mod.EventBusSubscriber}: registering them makes the EventBus reflect
 * over all their methods, and {@code Skill#renderName}'s client-only parameter
 * types (Font/GuiGraphics) then crash a dedicated server. This class keeps the
 * bus-facing signatures free of client classes.
 */
@Mod.EventBusSubscriber
public final class PunchSkillEvents {

    private PunchSkillEvents() {}

    @SubscribeEvent
    public static void seriousPunchCombat(LivingEvent event) {
        SeriousPunch.combatEvents(event);
    }

    @SubscribeEvent
    public static void seriousPunchDeath(LivingDeathEvent event) {
        SeriousPunch.deathEvent(event);
    }

    @SubscribeEvent
    public static void normalPunchFlux(LivingDamageEvent event) {
        NormalPunch.flux(event);
    }

    @SubscribeEvent
    public static void weakPunchFlux(LivingDamageEvent event) {
        WeakPunch.flux(event);
    }

    @SubscribeEvent
    public static void weakeningPunchFlux(LivingHurtEvent event) {
        WeakeningPunch.flux(event);
    }
}
