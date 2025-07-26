package com.onepunchcrafts.client.event;

import com.onepunchcrafts.client.event.extern.SetupPlayerAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class FMLClientSetupEventHandler {

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        SetupPlayerAnimation.setup();
    }
}
