package com.onepunchcrafts.client.event;

import com.onepunchcrafts.client.render.PortalBlockEntityRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.onepunchcrafts.OnePunchCrafts.MODID;
import static com.onepunchcrafts.OnePunchCrafts.PORTAL_BLOCK_ENTITY;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRenderersEventHandler {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PORTAL_BLOCK_ENTITY.get(), PortalBlockEntityRenderer::new);
    }
}
