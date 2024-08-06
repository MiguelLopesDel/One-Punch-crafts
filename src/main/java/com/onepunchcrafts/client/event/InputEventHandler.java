package com.onepunchcrafts.client.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.network.packet.SeriousPunchPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class InputEventHandler {

    @SubscribeEvent
    public static void playerAttack(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !event.isAttack()) return;
        player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            if (cap.isSaitama() && cap.getActualAbility() == 2)
                NetworkRegister.sendToServer(new SeriousPunchPacket());
        });
    }

    @SubscribeEvent
    public static void playerScroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        double scrollDelta = event.getScrollDelta();
        if (player == null || scrollDelta == 0) return;

        player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            if (!cap.isSaitama())
                return;
            if (cap.getActualAbility() == 6) {
                short speed = (short) (cap.getSpeed() + scrollDelta);
                cap.setSpeed(speed < 0 ? 0 : speed);
                NetworkRegister.sendToServer(new PlayerSyncPacket(cap));
            } else if (cap.getActualAbility() == 8) {
                short weight = (short) (cap.getWeight() + scrollDelta);
                cap.setWeight(weight < 0 ? 0 : weight);
                NetworkRegister.sendToServer(new PlayerSyncPacket(cap));
            }
            else if (cap.getActualAbility() == 9) {
                short knockbackResistance = (short) (cap.getKnockbackResistance() + scrollDelta);
                cap.setKnockbackResistance(knockbackResistance < 0 ? 0 : knockbackResistance);
                NetworkRegister.sendToServer(new PlayerSyncPacket(cap));
            }
            else if (cap.getActualAbility() == 10) {
                short attackKnockback = (short) (cap.getAttackKnockback() + scrollDelta);
                cap.setAttackKnockback(attackKnockback < 0 ? 0 : attackKnockback);
                NetworkRegister.sendToServer(new PlayerSyncPacket(cap));
            }
            else if (cap.getActualAbility() == 11) {
                short swimSpeed = (short) (cap.getSwimSpeed() + scrollDelta);
                cap.setSwimSpeed(swimSpeed < 0 ? 0 : swimSpeed);
                NetworkRegister.sendToServer(new PlayerSyncPacket(cap));
            }
        });
    }
}
