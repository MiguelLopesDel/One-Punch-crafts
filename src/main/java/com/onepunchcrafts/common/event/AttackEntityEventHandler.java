package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.skills.saitama.SeriousPunch;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AttackEntityEventHandler {

    @SubscribeEvent
    public static void playerAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity entity1) {
            HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(cap -> {
                if (cap.getCurrentSkill() instanceof SeriousPunch) {
                    if (entity1 instanceof WitherBoss witherBoss)
                        witherBoss.setInvulnerableTicks(0);
                    entity1.setInvulnerable(false);
                }
            });
        }
    }
}
