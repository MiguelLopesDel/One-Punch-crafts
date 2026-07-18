package com.onepunchcrafts;

import com.mojang.logging.LogUtils;
import com.onepunchcrafts.common.RegisterSounds;
import com.onepunchcrafts.common.capability.OnePunchCraftsLevelProvider;
import com.onepunchcrafts.common.capability.OnePunchCraftsPlayerProvider;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.capability.WorldRules;
import com.onepunchcrafts.common.skills.WithoutPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.common.block.PortalBlock;
import com.onepunchcrafts.common.block.entity.PortalBlockEntity;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.minecraft.PowerStateCodec;
import com.onepunchcrafts.v3.OnePunchV3;
import com.onepunchcrafts.v3.content.SaitamaContent;
import com.onepunchcrafts.network.packet.PowerStateSnapshotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.OptionalMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(OnePunchCrafts.MODID)
public class OnePunchCrafts {

    public static final String MODID = "onepunchcrafts";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final WithoutPack WITHOUT_PACK = new WithoutPack();

    public static final Capability<OnePunchPlayer> ONE_PLAYER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<WorldRules> WORLD_RULES_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static final OptionalMod<?> DRACONIC_MOD = OptionalMod.of("draconicevolution");
    public static final OptionalMod<?> IMMERSIVE_PORTALS_MOD = OptionalMod.of("immersive_portals");
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
//    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
//    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
//    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
//    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
//    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
//    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
//    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
//    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));
//
//    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
//    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
//            .alwaysEat().nutrition(1).saturationMod(2f).build())));

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final RegistryObject<PortalBlock> PORTAL_BLOCK = BLOCKS.register("portal_block", () ->
            new PortalBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(-1.0F)
    ));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final RegistryObject<BlockEntityType<PortalBlockEntity>> PORTAL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("portal_block_entity", () ->
                    BlockEntityType.Builder.of(PortalBlockEntity::new, PORTAL_BLOCK.get()).build(null)
            );

    public OnePunchCrafts() {
        OnePunchV3.register(SaitamaContent::register);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Register the Deferred Register to the mod event bus so blocks get registered
//        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
//        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
//        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        RegisterSounds.register(modEventBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                com.onepunchcrafts.client.ClientConfig.SPEC);
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> com.onepunchcrafts.client.ClientModExtensions::registerConfigScreen);

        // Register the item to a creative tab
//        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
//        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onPlayerRespawned(final PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            OnePunchPlayer onePunchPlayer = player.getCapability(ONE_PLAYER_CAPABILITY).orElse(new OnePunchPlayer(WITHOUT_PACK));
            onePunchPlayer.playerRespawn(event);
            HelpUtility.syncWithPlayer((ServerPlayer) player, onePunchPlayer);
            NetworkRegister.sendToPlayer((ServerPlayer) player,
                    new PowerStateSnapshotPacket(PowerStateCodec.encode(onePunchPlayer.getPowerState())));
        }
    }

    @SubscribeEvent
    public void playerClone(PlayerEvent.Clone event) {
        ServerPlayer original = (ServerPlayer) event.getOriginal();
        ServerPlayer player = (ServerPlayer) event.getEntity();
        OnePunchPlayer onePunchPlayer = new OnePunchPlayer(WITHOUT_PACK);
        original.revive();
        OnePunchPlayer oldOnePunchPlayer = original.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY, null).orElse(onePunchPlayer);
        reinsertCapability(player, onePunchPlayer, oldOnePunchPlayer);
        original.invalidateCaps();
    }

    private static void reinsertCapability(ServerPlayer player, OnePunchPlayer onePunchPlayer, OnePunchPlayer oldOnePunchPlayer) {
        OnePunchPlayer playerCap = player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY, null).orElse(onePunchPlayer);
        playerCap.setSkillPack(oldOnePunchPlayer.getSkillPack());
        playerCap.setCurrentSkill(oldOnePunchPlayer.getActualAbility());
        PowerStateCodec.decodeInto(PowerStateCodec.encode(oldOnePunchPlayer.getPowerState()), playerCap.getPowerState());
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(OnePunchCraftsPlayerProvider.class);
        event.register(OnePunchCraftsLevelProvider.class);
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer)) {
            event.addCapability(new ResourceLocation(MODID, "oneplayerdata"), new OnePunchCraftsPlayerProvider());
        }
    }

    @SubscribeEvent
    public void onAttachLevelCapabilities(AttachCapabilitiesEvent<Level> event) {
        if (event.getObject() instanceof Level) {
            event.addCapability(new ResourceLocation(MODID, "oneleveldata"), new OnePunchCraftsLevelProvider());
        }
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            OnePunchV3.bootstrap();
            NetworkRegister.registerMessages();
        });
    }

    // Add the example block item to the building blocks tab
//    private void addCreative(BuildCreativeModeTabContentsEvent event)
//    {
//        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
//            event.accept(EXAMPLE_BLOCK_ITEM);
//}

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (!event.getServer().isDedicatedServer() || ModList.get().isLoaded("attributefix"))
            return;
        String message = String.format("%s (%s)", "It is recommended to download the mod attributes " +
                "fix,", "https://www.curseforge.com/minecraft/mc-mods/attributefix/files/all?page=1&pageSize=20");
        LOGGER.info(message);
    }
}
