package com.onepunchcrafts.client.event;

import com.onepunchcrafts.client.Keybinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class RegisterKeyMappingsEventHandler {

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(Keybinding.INSTANCE.CHANGE_SKILL);
        event.register(Keybinding.INSTANCE.USE_SPECIAL_SKILL);
        event.register(Keybinding.INSTANCE.USE_FART);
    }
}
