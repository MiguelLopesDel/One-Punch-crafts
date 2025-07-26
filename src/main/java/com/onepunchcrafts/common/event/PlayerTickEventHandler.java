package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

@Mod.EventBusSubscriber
public class PlayerTickEventHandler {

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        HelpUtility.getSkillData(player).getSkillPack().tick(event);
    }
}
