package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.common.skills.saitama.SeriousPunch;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

import static com.onepunchcrafts.util.HelpUtility.verifyIsSaitamaAndGetCapability;

@Mod.EventBusSubscriber
public class LivingHurtEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void saitamaOnAttack(LivingHurtEvent event) {
        HelpUtility.isServerPlayer(event).ifPresent(p -> HelpUtility.getSkillData(p).manageFlux(event));
    }
}