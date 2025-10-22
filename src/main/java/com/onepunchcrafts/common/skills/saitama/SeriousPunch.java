package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.damage.DamagesRegistry;
import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import com.onepunchcrafts.util.TickUtilities;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;

import static com.onepunchcrafts.common.event.LivingDamageEventHandler.markBlocksToClear;
import static com.onepunchcrafts.util.HelpUtility.*;

@Mod.EventBusSubscriber
public class SeriousPunch implements Skill {

    @Override //contem logica de ray cast
    public SkillExecutionResult execute(Player p) {
        if (!(p.level() instanceof ServerLevel serverLevel) || !(p instanceof ServerPlayer player))
            return null;
        HelpUtility.clientEffects(player);
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        Vec3 cylinderStartPos = playerPos.add(lookVec.scale(3));

        ArrayList<BlockPos> blockPos = markBlocksToClear(serverLevel, 15, 1000, (int) Math.floor(cylinderStartPos.x), (int) Math.floor(cylinderStartPos.y), (int) Math.floor(cylinderStartPos.z), lookVec);
        final TickUtilities tickU = new TickUtilities();
        TickScheduler.scheduleWithCondition(Duration.of(50, ChronoUnit.MILLIS), () -> tickU.fillCylinderAndEmuleEffects(player, serverLevel, 1000, blockPos));
        return null;
    }

    @SubscribeEvent
    public static void combatEvents(LivingEvent event) {
        if (HelpUtility.getAttackerEntity(event) instanceof ServerPlayer player) {
            verifyIsSaitamaAndSkill(player, SeriousPunch.class).ifPresent(p -> seriosPunch(event));
        }
    }

    public static void seriosPunch(LivingEvent ev) {
        if (ev instanceof LivingDamageEvent damageEvent) {
            if (damageEvent.getSource().is(DamagesRegistry.SERIOUS_PUNCH_SECOND))
                applyDamageAndReactiveEvent(damageEvent, damageEvent.getEntity());
            else
                performSeriousPunch(damageEvent, (ServerPlayer) damageEvent.getSource().getEntity());
        } else if (ev instanceof LivingAttackEvent event) {
            event.setCanceled(false);
        } else if (ev instanceof LivingHurtEvent event) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent
    public static void deathEvent(LivingDeathEvent event) {
        boolean saitamaIsTarget = false;
        if (event.getEntity() instanceof ServerPlayer player) {
            saitamaIsTarget = cancelDeathSaitama(event, player);
        }
        if (!saitamaIsTarget) {
            DamageSource source = event.getSource();
            if (source.is(DamagesRegistry.SERIOUS_PUNCH_SECOND)) {
                if (source.getEntity() instanceof ServerPlayer player && HelpUtility.verifyIsSaitamaAndGetCapability(player).isPresent()) {
                    event.setCanceled(false);
                }
            } else if (source.getDirectEntity() instanceof ServerPlayer player) {
                Optional<SaitamaPack> saitamaPack = HelpUtility.verifyIsSaitamaAndGetCapability(player);
                saitamaPack.ifPresent(cap -> {
                    if (cap.getCurrentSkill() instanceof SeriousPunch)
                        event.setCanceled(false);
                });
            }
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.saitama.serious_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
    }

    private static boolean cancelDeathSaitama(LivingDeathEvent event, ServerPlayer player) {
        Optional<SaitamaPack> onePunchPlayer = HelpUtility.verifyIsSaitamaAndGetCapability(player);
        onePunchPlayer.ifPresent(cap -> {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
        });
        return onePunchPlayer.isPresent();
    }

    private static void performSeriousPunch(LivingDamageEvent event, ServerPlayer player) {
        clientEffects(player);
        ServerLevel serverLevel = player.serverLevel();
        LivingEntity target = event.getEntity();
        applyDamageAndReactiveEvent(event, target);
        seriousPunchWithSpecificTargetAndClientEffects(player, serverLevel);
    }

    private static void applyDamageAndReactiveEvent(LivingDamageEvent event, LivingEntity target) {
        target.setInvulnerable(false);
        target.setSecondsOnFire(60);
        target.invulnerableTime = 0;
        event.setAmount(event.getAmount() * 10_000_000_000_000_000f);
        event.setCanceled(false);
    }

    public static void seriousPunchWithSpecificTargetAndClientEffects(ServerPlayer player, ServerLevel serverLevel) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        Vec3 cylinderStartPos = playerPos.add(lookVec.scale(3));


        ArrayList<BlockPos> blockPos = markBlocksToClear(serverLevel, 15, 1000, (int) Math.floor(cylinderStartPos.x), (int) Math.floor(cylinderStartPos.y), (int) Math.floor(cylinderStartPos.z), lookVec);
        final TickUtilities tickU = new TickUtilities();
        TickScheduler.scheduleWithCondition(Duration.of(50, ChronoUnit.MILLIS), () -> tickU.fillCylinderAndEmuleEffects(player, serverLevel, 1000, blockPos));
    }
}
