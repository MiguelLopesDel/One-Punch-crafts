package com.onepunchcrafts.common.event;

import com.onepunchcrafts.common.damage.OneDamageProvider;
import com.onepunchcrafts.common.damage.DamagesRegistry;
import com.onepunchcrafts.common.damage.RegistryDataGenerator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GatherDataEventHandler {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        DatapackBuiltinEntriesProvider datapackProvider = new RegistryDataGenerator(output, event.getLookupProvider());
        CompletableFuture<HolderLookup.Provider> registryProvider = datapackProvider.getRegistryProvider();

        generator.addProvider(event.includeServer(), datapackProvider);
        generator.addProvider(event.includeServer(), new OneDamageProvider(output, registryProvider, event.getExistingFileHelper()));
    }
}
