package com.onepunchcrafts.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import static com.onepunchcrafts.OnePunchCrafts.MODID;
import static com.onepunchcrafts.client.render.ManagedPostChain.setFloat;
import static com.onepunchcrafts.client.render.ManagedPostChain.setMatrix;
import static com.onepunchcrafts.client.render.ManagedPostChain.setVec3;

/**
 * Drives the fullscreen CSRC post-processing chain (raymarched beam +
 * chromatic aberration). The chain reads the vanilla depth buffer through the
 * "minecraft:main:depth" aux target, so no mixins are required. Lifecycle
 * (reload, resize, Iris teardown) lives in {@link ManagedPostChain}; while a
 * shaderpack is active the quad-based fallback in
 * {@link BorosCsrcVfxRenderer} takes over.
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class CsrcPostChainHandler {
    private static final ManagedPostChain CHAIN = new ManagedPostChain(
            new ResourceLocation(MODID, "shaders/post/csrc.json"), "CSRC");

    private CsrcPostChainHandler() {}

    /** True when the shader path is available; the quad fallback should stay quiet. */
    public static boolean isChainActive() {
        return CHAIN.isActive();
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!CHAIN.isActive()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        BorosCsrcVfxRenderer.ActiveVfx vfx = BorosCsrcVfxRenderer.activeVfx(event.getPartialTick());
        if (vfx == null) return;

        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        Matrix4f modelView = new Matrix4f(event.getPoseStack().last().pose());
        Matrix4f invProjection = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f(modelView).invert();
        Matrix4f worldToClip = new Matrix4f(projection).mul(modelView);
        Vec3 cameraPos = event.getCamera().getPosition();

        // Live-togglable: when the local player is the caster and opted in,
        // the shaders open clean windows between the anime cuts so the caster
        // sees their own beam and the impact sphere.
        float casterView = minecraft.player != null
                && minecraft.player.getId() == vfx.casterId()
                && com.onepunchcrafts.client.ClientConfig.CSRC_CASTER_BEAM_VIEW.get() ? 1.0f : 0.0f;

        CHAIN.process(event.getPartialTick(), effect -> {
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
        });
    }
}
