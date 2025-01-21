package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class WeakeningPunch implements Skill {
    @Override
    public void execute(Player player) {

    }

    @Override
    public void flux(LivingEvent ev) {
        if (ev instanceof LivingHurtEvent event && event.getSource().getEntity() instanceof ServerPlayer) {
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
    }
}
