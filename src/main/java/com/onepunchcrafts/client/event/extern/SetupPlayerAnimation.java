package com.onepunchcrafts.client.event.extern;

import com.onepunchcrafts.client.event.FMLClientSetupEventHandler;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

public class SetupPlayerAnimation {

    public static void setup() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(new ResourceLocation(MODID, "onecraftsanimation"), 10, SetupPlayerAnimation::registerPlayerAnimations);
    }

    private static IAnimation registerPlayerAnimations(final AbstractClientPlayer player) {
        return new ModifierLayer<>();
    }
}
