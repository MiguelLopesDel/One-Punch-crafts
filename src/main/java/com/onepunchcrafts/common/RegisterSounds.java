package com.onepunchcrafts.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

public class RegisterSounds {

    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final RegistryObject<SoundEvent> SERIOUS_PUNCH = registerSounds("seriouspunch");
    public static final RegistryObject<SoundEvent> CSRC_SHOUT_JP = registerSounds("csrc_shout_jp");
    public static final RegistryObject<SoundEvent> CSRC_SHOUT_EN = registerSounds("csrc_shout_en");
    public static final RegistryObject<SoundEvent> CSRC_SHOUT_PT = registerSounds("csrc_shout_pt");

    private static RegistryObject<SoundEvent> registerSounds(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, name)));
    }

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }
}
