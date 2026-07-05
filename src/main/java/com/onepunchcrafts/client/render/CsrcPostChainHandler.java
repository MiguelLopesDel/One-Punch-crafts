package com.onepunchcrafts.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

/**
 * Drives the fullscreen CSRC post-processing chain (raymarched beam +
 * chromatic aberration). The chain reads the vanilla depth buffer through the
 * "minecraft:main:depth" aux target, so no mixins are required. While an
 * Iris/Oculus shaderpack is active the chain is torn down and the quad-based
 * fallback in {@link BorosCsrcVfxRenderer} takes over.
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class CsrcPostChainHandler {
    private static final ResourceLocation CHAIN_ID = new ResourceLocation(MODID, "shaders/post/csrc.json");
    private static final Field PASSES_FIELD = findPassesField();

    private static PostChain chain;
    private static boolean chainReady;
    private static int chainWidth = -1;
    private static int chainHeight = -1;

    private static final boolean HAVE_IRIS = ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");
    private static boolean shaderpackActive;

    static {
        if (PASSES_FIELD != null) {
            PASSES_FIELD.setAccessible(true);
        }
        shaderpackActive = queryIrisShaderpackInUse();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CsrcPostChainHandler::onRegisterReloadListeners);
    }

    private CsrcPostChainHandler() {}

    /** True when the shader path is available; the quad fallback should stay quiet. */
    public static boolean isChainActive() {
        return chainReady && !shaderpackActive;
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                if (shaderpackActive) {
                    closeChain();
                } else {
                    reloadChain(resourceManager);
                }
            }
        });
    }

    private static void reloadChain(ResourceManager resourceManager) {
        Minecraft minecraft = Minecraft.getInstance();
        closeChain();
        if (minecraft.getMainRenderTarget() == null) return;

        try {
            chain = new PostChain(minecraft.getTextureManager(), resourceManager,
                    minecraft.getMainRenderTarget(), CHAIN_ID);
            chainReady = true;
            chainWidth = -1;
            chainHeight = -1;
            resizeChain(minecraft);
            com.onepunchcrafts.OnePunchCrafts.LOGGER.info("[onepunchcrafts] CSRC post chain built successfully.");
        } catch (IOException exception) {
            com.onepunchcrafts.OnePunchCrafts.LOGGER.error("Failed to load CSRC post chain", exception);
            closeChain();
        }
    }

    private static void resizeChain(Minecraft minecraft) {
        if (chain == null) return;
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) return;
        if (mainTarget.width == chainWidth && mainTarget.height == chainHeight) return;

        chain.resize(mainTarget.width, mainTarget.height);
        chainWidth = mainTarget.width;
        chainHeight = mainTarget.height;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !HAVE_IRIS) return;

        boolean nowActive = queryIrisShaderpackInUse();
        if (nowActive != shaderpackActive) {
            shaderpackActive = nowActive;
            if (shaderpackActive) {
                closeChain();
            } else {
                reloadChain(Minecraft.getInstance().getResourceManager());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!isChainActive() || chain == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        BorosCsrcVfxRenderer.ActiveVfx vfx = BorosCsrcVfxRenderer.activeVfx(event.getPartialTick());
        if (vfx == null) return;

        resizeChain(minecraft);

        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        Matrix4f modelView = new Matrix4f(event.getPoseStack().last().pose());
        Matrix4f invProjection = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f(modelView).invert();
        Matrix4f worldToClip = new Matrix4f(projection).mul(modelView);
        Vec3 cameraPos = event.getCamera().getPosition();

        List<PostPass> passes = getPasses();
        if (passes.isEmpty()) return;

        // Live-togglable: when the local player is the caster and opted in,
        // the shaders open clean windows between the anime cuts so the caster
        // sees their own beam and the impact sphere.
        float casterView = minecraft.player != null
                && minecraft.player.getId() == vfx.casterId()
                && com.onepunchcrafts.client.ClientConfig.CSRC_CASTER_BEAM_VIEW.get() ? 1.0f : 0.0f;

        for (PostPass pass : passes) {
            EffectInstance effect = pass.getEffect();
            if (effect == null) continue;

            setMatrix(effect, "InvProjMat", invProjection);
            setMatrix(effect, "InvViewMat", invView);
            setMatrix(effect, "WorldToClip", worldToClip);
            setVec3(effect, "CameraPosition", cameraPos);
            setVec3(effect, "StartPosition", vfx.origin());
            setVec3(effect, "BeamDirection", vfx.direction());
            setVec3(effect, "ImpactPosition", vfx.impact());
            setFloat(effect, "BeamRange", (float) vfx.range());
            setFloat(effect, "ImpactRadius", (float) vfx.impactRadius());
            setFloat(effect, "iTime", vfx.ageTicks() / 20.0f);
            setFloat(effect, "ChargeTime", vfx.chargeTicks() / 20.0f);
            setFloat(effect, "FireTime", vfx.fireTicks() / 20.0f);
            setFloat(effect, "CasterView", casterView);
            setFloat(effect, "ReducedFlash",
                    com.onepunchcrafts.client.ClientConfig.CSRC_REDUCED_FLASHES.get() ? 1.0f : 0.0f);
        }

        chain.process(minecraft.isPaused() ? 0.0f : event.getPartialTick());
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    private static List<PostPass> getPasses() {
        if (chain == null || PASSES_FIELD == null) return Collections.emptyList();
        try {
            Object value = PASSES_FIELD.get(chain);
            if (value instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<PostPass> passes = (List<PostPass>) list;
                return passes;
            }
        } catch (IllegalAccessException ignored) {
        }
        return Collections.emptyList();
    }

    private static Field findPassesField() {
        try {
            return ObfuscationReflectionHelper.findField(PostChain.class, "passes");
        } catch (ObfuscationReflectionHelper.UnableToFindFieldException ignored) {
            try {
                return ObfuscationReflectionHelper.findField(PostChain.class, "f_110009_");
            } catch (ObfuscationReflectionHelper.UnableToFindFieldException exception) {
                com.onepunchcrafts.OnePunchCrafts.LOGGER.error("Unable to find PostChain passes field", exception);
                return null;
            }
        }
    }

    private static void setMatrix(EffectInstance effect, String name, Matrix4f matrix) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(matrix);
    }

    private static void setVec3(EffectInstance effect, String name, Vec3 vec) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set((float) vec.x, (float) vec.y, (float) vec.z);
    }

    private static void setFloat(EffectInstance effect, String name, float value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void closeChain() {
        if (chain != null) {
            try {
                chain.close();
            } catch (Exception ignored) {
            }
            chain = null;
        }
        chainReady = false;
        chainWidth = -1;
        chainHeight = -1;
    }

    private static boolean queryIrisShaderpackInUse() {
        if (!HAVE_IRIS) return false;
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("isShaderPackInUse").invoke(api);
            return result instanceof Boolean bool && bool;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
