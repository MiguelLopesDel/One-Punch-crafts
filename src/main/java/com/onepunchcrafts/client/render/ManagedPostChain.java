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
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A fullscreen {@link PostChain} with its whole lifecycle managed: resource
 * reload, framebuffer resize, access to the passes (reflection over the two
 * possible field names), and the Iris/Oculus dance — the chain is torn down
 * while a shaderpack is active and rebuilt when it is turned off. Owners are
 * left with only what is genuinely theirs: deciding when to run and feeding
 * uniforms via {@link #process}.
 *
 * Construct during client mod init (the reload listener must be registered
 * before the first resource load).
 */
public final class ManagedPostChain {

    private static final Field PASSES_FIELD = findPassesField();
    private static final boolean HAVE_IRIS = ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");

    static {
        if (PASSES_FIELD != null) {
            PASSES_FIELD.setAccessible(true);
        }
    }

    private final ResourceLocation chainId;
    private final String displayName;

    private PostChain chain;
    private boolean chainReady;
    private int chainWidth = -1;
    private int chainHeight = -1;
    private boolean shaderpackActive;

    public ManagedPostChain(ResourceLocation chainId, String displayName) {
        this.chainId = chainId;
        this.displayName = displayName;
        this.shaderpackActive = queryIrisShaderpackInUse();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }

    /** True when the shader path is available; quad fallbacks should stay quiet. */
    public boolean isActive() {
        return chainReady && !shaderpackActive;
    }

    /**
     * Runs the chain over the main render target: resizes if needed, hands
     * every pass effect to {@code uniforms}, processes and rebinds. No-op
     * while the chain is unavailable.
     */
    public void process(float partialTick, Consumer<EffectInstance> uniforms) {
        if (!isActive() || chain == null) return;
        Minecraft minecraft = Minecraft.getInstance();

        resizeChain(minecraft);

        for (PostPass pass : getPasses()) {
            EffectInstance effect = pass.getEffect();
            if (effect == null) continue;
            uniforms.accept(effect);
        }

        chain.process(minecraft.isPaused() ? 0.0f : partialTick);
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    private void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
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

    private void onClientTick(TickEvent.ClientTickEvent event) {
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

    private void reloadChain(ResourceManager resourceManager) {
        Minecraft minecraft = Minecraft.getInstance();
        closeChain();
        if (minecraft.getMainRenderTarget() == null) return;

        try {
            chain = new PostChain(minecraft.getTextureManager(), resourceManager,
                    minecraft.getMainRenderTarget(), chainId);
            chainReady = true;
            chainWidth = -1;
            chainHeight = -1;
            resizeChain(minecraft);
            com.onepunchcrafts.OnePunchCrafts.LOGGER.info("[onepunchcrafts] {} post chain built successfully.", displayName);
        } catch (IOException exception) {
            com.onepunchcrafts.OnePunchCrafts.LOGGER.error("Failed to load {} post chain", displayName, exception);
            closeChain();
        }
    }

    private void resizeChain(Minecraft minecraft) {
        if (chain == null) return;
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) return;
        if (mainTarget.width == chainWidth && mainTarget.height == chainHeight) return;

        chain.resize(mainTarget.width, mainTarget.height);
        chainWidth = mainTarget.width;
        chainHeight = mainTarget.height;
    }

    private List<PostPass> getPasses() {
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

    private void closeChain() {
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

    // Shared uniform helpers so owners don't each reinvent null-checked sets.

    public static void setMatrix(EffectInstance effect, String name, Matrix4f matrix) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(matrix);
    }

    public static void setVec3(EffectInstance effect, String name, Vec3 vec) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public static void setVec2(EffectInstance effect, String name, float x, float y) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }

    public static void setFloat(EffectInstance effect, String name, float value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(value);
    }
}
