package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.common.skills.saitama.SeriousPunch;
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
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            Optional<SaitamaPack> onePunchPlayer = verifyIsSaitamaAndGetCapability(player);
            onePunchPlayer.ifPresent(cap -> {
                if (cap.getCurrentSkill() instanceof SeriousPunch) {
                    event.setCanceled(false); //TODO REVEJA ESTA MERDA
                } else if (cap.getCurrentGroupIndex() == 0 && cap.getCurrentSkillIndex() == 4) {
                    LivingEntity target = event.getEntity();
                    target.spawnAtLocation(target.getMainHandItem());
                    target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    target.spawnAtLocation(target.getOffhandItem());
                    target.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

                    target.spawnAtLocation(target.getItemBySlot(EquipmentSlot.HEAD));
                    target.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    target.spawnAtLocation(target.getItemBySlot(EquipmentSlot.CHEST));
                    target.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                    target.spawnAtLocation(target.getItemBySlot(EquipmentSlot.FEET));
                    target.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                    target.spawnAtLocation(target.getItemBySlot(EquipmentSlot.LEGS));
                    target.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                    float amount = target.getHealth() - 0.0001F;
                    event.setAmount(Math.min(amount, 100_000));
                }
            });
        }

    }
}
