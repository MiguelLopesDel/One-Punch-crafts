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
 * Fullscreen half of the Dimensional Punch: cracks forming during the windup,
 * then the frame shattering — refraction, chromatic split and a violet/cyan
 * rift bleeding through — as space breaks. The shader is dumb; the timeline is
 * computed here and pushed through uniforms. Lifecycle lives in
 * {@link ManagedPostChain}.
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class DimensionalPostChainHandler {
    private static final ManagedPostChain CHAIN = new ManagedPostChain(
            new ResourceLocation(MODID, "shaders/post/dimensional_punch.json"), "Dimensional Punch");

    private DimensionalPostChainHandler() {}

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!CHAIN.isActive()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        DimensionalPunchCinematic.State state = DimensionalPunchCinematic.state(event.getPartialTick());
        if (state == null) return;

        float age = state.age();
        int windup = state.windupTicks();
        float sinceImpact = age - windup;
        if (sinceImpact > DimensionalPunchCinematic.TAIL_TICKS) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        float distance = (float) cameraPos.distanceTo(state.rift());
        float proximity = 40.0f / (distance + 40.0f);

        float crackAmount;
        float riftOpen;
        float distort;
        float aberration;
        float flash;

        if (age < windup) {
            float p = Mth.clamp(age / windup, 0.0f, 1.0f);
            float eased = p * p * (3.0f - 2.0f * p);
            crackAmount = eased;
            riftOpen = 0.0f;
            distort = 0.010f * eased * proximity;
            aberration = 3.0f * eased * proximity;
            flash = 0.0f;
        } else {
            float open = Mth.clamp(sinceImpact / DimensionalPunchCinematic.OPEN_TICKS, 0.0f, 1.0f);
            float tail = 1.0f - Mth.clamp((sinceImpact - DimensionalPunchCinematic.OPEN_TICKS)
                    / (DimensionalPunchCinematic.TAIL_TICKS - DimensionalPunchCinematic.OPEN_TICKS), 0.0f, 1.0f);
            crackAmount = (float) Math.exp(-sinceImpact * 0.22);
            riftOpen = open * tail;
            distort = 0.030f * (float) Math.exp(-sinceImpact * 0.28) * proximity;
            aberration = 10.0f * (float) Math.exp(-sinceImpact * 0.5) * proximity;
            flash = Mth.clamp(1.1f * (float) Math.exp(-sinceImpact * 1.5), 0.0f, 1.0f) * (0.4f + 0.6f * proximity);
        }

        if (crackAmount + riftOpen + distort + aberration + flash < 0.01f) return;

        // Project the rift into screen UV (stage pose stack is camera-rotation only).
        Matrix4f worldToClip = new Matrix4f(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());
        Vec3 rift = state.rift();
        Vector4f clip = worldToClip.transform(new Vector4f(
                (float) (rift.x - cameraPos.x),
                (float) (rift.y - cameraPos.y),
                (float) (rift.z - cameraPos.z), 1.0f));
        float focusValid = clip.w > 0.05f ? 1.0f : 0.0f;
        float focusX = focusValid > 0.0f ? Mth.clamp(clip.x / clip.w * 0.5f + 0.5f, -0.4f, 1.4f) : 0.5f;
        float focusY = focusValid > 0.0f ? Mth.clamp(clip.y / clip.w * 0.5f + 0.5f, -0.4f, 1.4f) : 0.5f;

        float finalCrack = crackAmount;
        float finalRift = riftOpen;
        float finalDistort = distort;
        float finalAberration = aberration;
        float finalFlash = flash;
        CHAIN.process(event.getPartialTick(), effect -> {
            setVec2(effect, "FocusPoint", focusX, focusY);
            setFloat(effect, "FocusValid", focusValid);
            setFloat(effect, "CrackAmount", finalCrack);
            setFloat(effect, "RiftOpen", finalRift);
            setFloat(effect, "Distort", finalDistort);
            setFloat(effect, "Aberration", finalAberration);
            setFloat(effect, "Flash", finalFlash);
        });
    }
}
