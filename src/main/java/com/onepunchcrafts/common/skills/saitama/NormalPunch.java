package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AnimationPacket;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.DraconicCompat;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.onepunchcrafts.OnePunchCrafts.DRACONIC_MOD;

@Mod.EventBusSubscriber
public class NormalPunch implements Skill {

    @Override
    public void execute(Player p) {
        consecutiveNormalPunches(p);
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.saitama.normal_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
    }

    @SubscribeEvent
    public static void flux(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            HelpUtility.verifyIsSaitamaAndSkill(player, NormalPunch.class).ifPresent(p -> normalPunch(event, player));
        }
    }

    private static void normalPunch(LivingDamageEvent event, ServerPlayer player) {
        LivingEntity target = event.getEntity();
        double d1;
        double d0 = player.getX() - target.getX();
        for (d1 = player.getZ() - target.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
            d0 = (Math.random() - Math.random()) * 0.01D;
        }
        knockback(target, 5, d0, d1);
        event.setAmount(event.getAmount() * 10_000_000);
        event.getEntity().addTag("targetnormalpunch");
        TickScheduler.scheduleFromHere(Duration.of(200, ChronoUnit.MILLIS), () -> event.getEntity().removeTag("targetnormalpunch"));
    }

    private static void knockback(LivingEntity target, double strength, double pX, double pZ) {
        if (!(strength <= 0.0D)) {
            target.hasImpulse = true;
            Vec3 vec3 = target.getDeltaMovement();
            Vec3 vec31 = (new Vec3(pX, 0.0D, pZ)).normalize().scale(strength);
            target.setDeltaMovement(vec3.x / 2.0D - vec31.x, target.onGround() ? Math.min(0.4D, vec3.y / 2.0D + strength) : vec3.y, vec3.z / 2.0D - vec31.z);
        }
    }

    private static void consecutiveNormalPunches(Player p) {
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
                if (DRACONIC_MOD.isPresent() && DraconicCompat.attackGuardian(player, entity, false))
                    return;
                player.attack(entity);
            });
            if (DRACONIC_MOD.isPresent())
                DraconicCompat.attackCrystals(player, serverLevel, pArea, false);
        });
        NetworkRegister.sendToAllClientsExcept(player, new AnimationPacket(player.getStringUUID(), "multiple_punches"));
        TickScheduler.scheduleFromHere(Duration.of(5, ChronoUnit.SECONDS), () -> NetworkRegister.sendToAllClientsExcept(player, new AnimationPacket(player.getStringUUID(), "stop")));
    }
}
