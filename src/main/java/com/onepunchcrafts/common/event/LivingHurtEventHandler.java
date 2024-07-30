package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.capability.OnePunchPlayer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.ItemStack;
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
