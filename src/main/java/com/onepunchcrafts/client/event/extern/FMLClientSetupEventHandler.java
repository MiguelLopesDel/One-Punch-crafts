package com.onepunchcrafts.client.event.extern;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
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
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(new ResourceLocation(MODID, "onecraftsanimation"), 10, FMLClientSetupEventHandler::registerPlayerAnimations);
    }

    private static IAnimation registerPlayerAnimations(final AbstractClientPlayer player) {
        return new ModifierLayer<>();
    }
}
