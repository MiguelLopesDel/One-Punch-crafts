package com.onepunchcrafts.common.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.*;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

public class DamagesRegistry {


    public static ResourceKey<DamageType> SERIOUS_PUNCH_SECOND = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "serious_punch_second_damage"));

    public static ResourceKey<DamageType> SERIOUS_PUNCH = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "serious_punch_damage"));
//
//    public static void bootstrap(BootstapContext<DamageType> context) {
//        context.register(SERIOUS_PUNCH, new DamageType("serious_punch_damage", DamageScaling.ALWAYS, 0.1F));
//        context.register(SERIOUS_PUNCH_SECOND, new DamageType("serious_punch_second_damage", DamageScaling.ALWAYS, 0.1F));
//    }
}
