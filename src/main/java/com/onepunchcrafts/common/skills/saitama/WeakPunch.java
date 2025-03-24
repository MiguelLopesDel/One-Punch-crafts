package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AnimationPacket;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.onepunchcrafts.OnePunchCrafts.DRACONIC_MOD;
import static com.onepunchcrafts.util.DraconicCompat.attackCrystals;
import static com.onepunchcrafts.util.DraconicCompat.attackGuardian;

@Mod.EventBusSubscriber
public class WeakPunch implements Skill {

    @Override
    public void execute(Player player) {
        consecutivePunches(player);
    }

    @SubscribeEvent
    public static void flux(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            HelpUtility.verifyIsSaitamaAndSkill(player, WeakPunch.class).ifPresent(p ->
                    event.setAmount(event.getAmount() * 100_000));
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.saitama.weak_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
    }

    private static void consecutivePunches(Player p) {
        if (!(p.level() instanceof ServerLevel serverLevel) || !(p instanceof ServerPlayer player))
            return;
        TickScheduler.scheduleDuringAndWithInterval(Duration.of(5, ChronoUnit.SECONDS), Duration.of(50, ChronoUnit.MILLIS), () -> {
            BlockPos pStart = player.blockPosition();
            int i = 2;
            AABB pArea = new AABB(new BlockPos(pStart.getX(), pStart.getY(), pStart.getZ()),
                    new BlockPos(pStart.getX() + i, pStart.getY() + i, pStart.getZ() + i));

            serverLevel.getEntitiesOfClass(LivingEntity.class, pArea).forEach(entity -> {
                if (player.equals(entity))
                    return;
                entity.setInvulnerable(false);
                if (DRACONIC_MOD.isPresent() && attackGuardian(player, entity, true))
                    return;
                player.attack(entity);
            });
            if (DRACONIC_MOD.isPresent())
                attackCrystals(player, serverLevel, pArea, true);
        });
        NetworkRegister.sendToAllClientsExcept(player, new AnimationPacket(player.getStringUUID(), "multiple_punches"));
        TickScheduler.scheduleFromHere(Duration.of(5, ChronoUnit.SECONDS), () -> NetworkRegister.sendToAllClientsExcept(player, new AnimationPacket(player.getStringUUID(), "stop")));
    }
}
