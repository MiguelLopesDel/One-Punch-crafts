package com.onepunchcrafts.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static com.onepunchcrafts.OnePunchCrafts.MODID;
import static com.onepunchcrafts.client.render.ManagedPostChain.setFloat;
import static com.onepunchcrafts.client.render.ManagedPostChain.setVec2;

/**
 * Fullscreen half of the Serious Punch cinematic: windup desaturation and
 * space compression toward the fist, the few-frame white flash, the lens
 * ripple racing outward and a chromatic spike on impact. The shader is dumb —
 * the whole timeline is computed here and fed through uniforms. Lifecycle
 * (reload, resize, Iris teardown) lives in {@link ManagedPostChain}.
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class SeriousPostChainHandler {
    private static final ManagedPostChain CHAIN = new ManagedPostChain(
            new ResourceLocation(MODID, "shaders/post/serious_punch.json"), "Serious Punch");

    private SeriousPostChainHandler() {}

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!CHAIN.isActive()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        SeriousPunchCinematic.State state = SeriousPunchCinematic.state(event.getPartialTick());
        if (state == null) return;

        float age = state.age();
        int windup = state.windupTicks();
        float sinceImpact = age - windup;

        // The screen work is over well before the world aftermath ends.
        if (sinceImpact > 40.0f) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        float distance = (float) cameraPos.distanceTo(state.origin());
        float proximity = 40.0f / (distance + 40.0f);

        float desaturate;
        float compress;
        float flash;
        float rippleProgress;
        float rippleAmp;
        float aberration;
        float vignette;

        if (age < windup) {
            float p = Mth.clamp(age / windup, 0.0f, 1.0f);
            float eased = p * p * (3.0f - 2.0f * p);
            desaturate = 0.7f * eased;
            compress = 0.020f * eased * proximity;
            flash = 0.0f;
            rippleProgress = 0.0f;
            rippleAmp = 0.0f;
            aberration = 2.0f * eased * proximity;
            vignette = 0.38f * eased;
        } else {
            desaturate = 0.7f * (float) Math.exp(-sinceImpact * 0.8);
            compress = 0.0f;
            flash = Mth.clamp(1.15f * (float) Math.exp(-sinceImpact * 1.6), 0.0f, 1.0f) * (0.35f + 0.65f * proximity);
            rippleProgress = Math.min(sinceImpact / 10.0f, 1.4f);
            rippleAmp = 0.05f * (float) Math.exp(-sinceImpact * 0.35) * proximity;
            aberration = 10.0f * (float) Math.exp(-sinceImpact * 0.9) * proximity;
            vignette = 0.25f * (float) Math.exp(-sinceImpact * 0.12);
        }

        if (desaturate + flash + rippleAmp + aberration + vignette + compress < 0.01f) return;

        // Project the fist into screen UV space. The stage pose stack is
        // camera-rotation only, so the projection runs on camera-relative
        // coordinates.
        Matrix4f worldToClip = new Matrix4f(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());
        Vec3 focusWorld = state.origin().add(state.direction().scale(2.0));
        Vector4f clip = worldToClip.transform(new Vector4f(
                (float) (focusWorld.x - cameraPos.x),
                (float) (focusWorld.y - cameraPos.y),
                (float) (focusWorld.z - cameraPos.z), 1.0f));
        float focusValid = clip.w > 0.05f ? 1.0f : 0.0f;
        float focusX = focusValid > 0.0f ? Mth.clamp(clip.x / clip.w * 0.5f + 0.5f, -0.4f, 1.4f) : 0.5f;
        float focusY = focusValid > 0.0f ? Mth.clamp(clip.y / clip.w * 0.5f + 0.5f, -0.4f, 1.4f) : 0.5f;

        float finalDesaturate = desaturate;
        float finalCompress = compress;
        float finalFlash = flash;
        float finalRippleProgress = rippleProgress;
        float finalRippleAmp = rippleAmp;
        float finalAberration = aberration;
        float finalVignette = vignette;
        CHAIN.process(event.getPartialTick(), effect -> {
            setVec2(effect, "FocusPoint", focusX, focusY);
            setFloat(effect, "FocusValid", focusValid);
            setFloat(effect, "Desaturate", finalDesaturate);
            setFloat(effect, "CompressAmount", finalCompress);
            setFloat(effect, "FlashAmount", finalFlash);
            setFloat(effect, "RippleProgress", finalRippleProgress);
            setFloat(effect, "RippleAmp", finalRippleAmp);
            setFloat(effect, "Aberration", finalAberration);
            setFloat(effect, "VignetteAmount", finalVignette);
        });
    }
}
