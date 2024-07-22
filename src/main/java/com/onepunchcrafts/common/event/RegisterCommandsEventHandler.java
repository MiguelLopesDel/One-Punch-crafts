package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.command.OneUtilCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class RegisterCommandsEventHandler {

    @SubscribeEvent
    public static void onServerStarting(RegisterCommandsEvent event) {
        OneUtilCommand.register(event.getDispatcher());
    }
}
