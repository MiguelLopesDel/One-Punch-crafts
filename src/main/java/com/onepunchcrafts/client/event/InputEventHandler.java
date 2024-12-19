package com.onepunchcrafts.client.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.skills.SaitamaPack;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.network.packet.SeriousPunchPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.onepunchcrafts.util.HelpUtility.syncDataWithServer;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class InputEventHandler {

    @SubscribeEvent
    public static void playerAttack(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !event.isAttack()) return;
        OnePunchPlayer data = HelpUtility.getSkillData(player);
        if (data.getActualAbility() == 2 && data.getSkillPack() instanceof SaitamaPack)
            NetworkRegister.sendToServer(new SeriousPunchPacket());
    }

    @SubscribeEvent
    public static void playerScroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        double scrollDelta = event.getScrollDelta();
        if (player == null || scrollDelta == 0) return;
        HelpUtility.isSaitama(player).ifBothPresent((cap, sai) -> {
            switch (cap.getActualAbility()) {
                case 6 -> cap.adjustAbilityAndSyncWithServer(sai::setSpeed, sai.getSpeed(), scrollDelta);
                case 8 -> cap.adjustAbilityAndSyncWithServer(sai::setWeight, sai.getWeight(), scrollDelta);
                case 9 -> cap.adjustAbilityAndSyncWithServer(sai::setKnockbackResistance, sai.getKnockbackResistance(), scrollDelta);
                case 10 -> cap.adjustAbilityAndSyncWithServer(sai::setAttackKnockback, sai.getAttackKnockback(), scrollDelta);
                case 11 -> cap.adjustAbilityAndSyncWithServer(sai::setSwimSpeed, sai.getSwimSpeed(), scrollDelta);
            }
        });
    }
}
