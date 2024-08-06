package com.onepunchcrafts.client.event;

import com.onepunchcrafts.common.capability.OnePunchPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class RenderGuiEventHandler {

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Pre event) {
        Minecraft instance = Minecraft.getInstance();
        LocalPlayer player = instance.player;
        player.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            if (cap.isSaitama()) {
                renderSaitamaOptions(event, cap, instance, guiGraphics);
            }
        });
    }

    private static void renderSaitamaOptions(RenderGuiEvent.Pre event, OnePunchPlayer cap, Minecraft instance, GuiGraphics guiGraphics) {
        final int width = event.getWindow().getGuiScaledWidth();
        final int height = event.getWindow().getGuiScaledHeight();
        Font font = instance.font;
        int defaultReduce = (int) (width * 0.05);
        int defaultAdd = (int) (height * 0.25);
//        switch (cap.getActualAbility()) {
//            case 0:
//                guiGraphics.drawString(font, Component.translatable("skill.saitama.weak_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
//                break;
//            case 1:
//                guiGraphics.drawString(font, Component.translatable("skill.saitama.normal_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
//                break;
//            case 2:
//                guiGraphics.drawString(font, Component.translatable("skill.saitama.serious_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
//                break;
//            case 3:
//                guiGraphics.drawString(font, Component.translatable("skill.saitama.serious_fart"), width / 2 - defaultReduce, height / 2 + defaultAdd, cap.isSeriousFartActive() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
//                break;
//            case 4:
//                guiGraphics.drawString(font, Component.translatable("skill.saitama.weakening_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
//                break;
//            case 5:
//                guiGraphics.drawString(font, Component.translatable("skill.saitama.quick_backstab"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
//                break;
//            case 6:
//                guiGraphics.drawString(font, Component.translatable("skill.saitama.super_speed"), width / 2 - defaultReduce, height / 2 + defaultAdd, cap.isSuperSpeed() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
//                break;
//            case 7:
//                guiGraphics.drawString(font, Component.translatable("skill.saitama.break_blocks_quickly"), width / 2 - defaultReduce, height / 2 + defaultAdd, cap.isBreakBlocksQuickly() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
//                break;
//        }
        switch (cap.getActualAbility()) {
            case 0 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.weak_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 1 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.normal_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 2 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.serious_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 3 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.serious_fart"), width / 2 - defaultReduce, height / 2 + defaultAdd, cap.isSeriousFartActive() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
            case 4 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.weakening_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 5 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.quick_backstab"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 6 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.super_speed", cap.getSpeed()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 7 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.break_blocks_quickly"), width / 2 - defaultReduce, height / 2 + defaultAdd, cap.isBreakBlocksQuickly() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
            case 8 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.set_weight", cap.getWeight()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 9 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.knockback_resistance", cap.getKnockbackResistance()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 10 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.attack_knockback", cap.getAttackKnockback()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 11 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.swim_speed", cap.getSwimSpeed()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 12 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.teleport", cap.getSwimSpeed()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
        }
    }
}
