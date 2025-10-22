package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

@Mod.EventBusSubscriber
public class WeakeningPunch implements Skill {
    @Override
    public SkillExecutionResult execute(Player player) {

        return null;
    }

    @SubscribeEvent
    public static void flux(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            HelpUtility.verifyIsSaitamaAndSkill(player, WeakeningPunch.class).ifPresent(p ->
                    weakeningPunch(event));
        }
    }

    private static void weakeningPunch(LivingHurtEvent event) {
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

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.saitama.weakening_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
    }
}
