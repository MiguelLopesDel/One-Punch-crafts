package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class LivingJumpEventHandler {

    @SubscribeEvent
    public static void saitamaJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
                MobEffectInstance effect = player.getEffect(MobEffects.JUMP);
                if (cap.isSaitama() && effect != null && effect.getAmplifier() >= 20) {
                    player.serverLevel().explode(null, player.getX(), player.getY(), player.getZ(), 10,
                            Level.ExplosionInteraction.MOB);
                }
            });
        }

    }
}
