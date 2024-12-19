package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

import static com.onepunchcrafts.OnePunchCrafts.WITHOUT_PACK;
import static java.awt.PageAttributes.ColorType.COLOR;

@Mod.EventBusSubscriber
public class PlayerLoggedInEventHandler {

    @SubscribeEvent
    public static void onPlayerLogging(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (player.getServer().isSingleplayerOwner(player.getGameProfile()) && !ModList.get().isLoaded("attributefix")) {
            ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.curseforge.com/minecraft/mc-mods/attributefix/files/all?page=1&pageSize=20");
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Attributes fix site"));
            MutableComponent msn = Component.literal("It is recommended to download the mod attributes fix, click here");
            msn.setStyle(msn.getStyle().withClickEvent(clickEvent));
            msn.setStyle(msn.getStyle().withHoverEvent(hoverEvent).withColor(Color.CYAN.getRGB()));
            player.sendSystemMessage(msn);
        }
        if (!player.level().isClientSide()) {
            OnePunchPlayer onePunchPlayer = player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).orElse(new OnePunchPlayer(WITHOUT_PACK));
            NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(onePunchPlayer.getSkillPack()));
        }
    }
}
