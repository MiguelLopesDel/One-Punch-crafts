package com.onepunchcrafts.common.event;

import com.onepunchcrafts.content.SaitamaContent;
import com.onepunchcrafts.network.packet.SaitamaTechniqueVfxPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Turns actual Minecraft reactions into presentation events; it never changes combat outcome. */
@Mod.EventBusSubscriber
public final class SaitamaPresentationEventHandler {
    private SaitamaPresentationEventHandler() {}

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var state = HelpUtility.getSkillData(player).getPowerState();
        if (!state.powerSetId().equals(SaitamaContent.POWER_SET)) return;
        double resistance = state.attributes().value(SaitamaContent.ATTR_KNOCKBACK_RESISTANCE);
        if (resistance <= 0 || event.getOriginalStrength() <= 0) return;

        Vec3 direction = new Vec3(event.getOriginalRatioX(), 0, event.getOriginalRatioZ());
        if (direction.lengthSqr() < 1.0e-5) direction = player.getLookAngle().scale(-1);
        SaitamaTechniqueVfxPacket.broadcast(player.serverLevel(), new SaitamaTechniqueVfxPacket(
                player.getId(), player.position().add(0, player.getBbHeight() * 0.5, 0),
                direction.normalize(),
                (float) Math.min(4.0, 0.5 + event.getOriginalStrength() + resistance / 40.0),
                SaitamaTechniqueVfxPacket.KNOCKBACK_RESIST, 10));
    }
}
