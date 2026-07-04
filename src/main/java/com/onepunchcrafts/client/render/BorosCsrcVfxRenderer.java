package com.onepunchcrafts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onepunchcrafts.client.ClientConfig;
import com.onepunchcrafts.common.RegisterSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class BorosCsrcVfxRenderer {
    private static final List<CsrcEffect> EFFECTS = new CopyOnWriteArrayList<>();
    // Must cover the shader's AFTERGLOW_TIME (2.4s) so the post effect fades out fully.
    private static final int AFTERGLOW_TICKS = 52;

    public static void addEffect(int casterId, Vec3 start, Vec3 direction, double range,
                                 int chargeTicks, int fireTicks, Vec3 impact, double impactRadius) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        long now = minecraft.level.getGameTime();
        EFFECTS.add(new CsrcEffect(casterId, start, direction.normalize(), range, chargeTicks,
                fireTicks, impact, impactRadius, now));
        // Like the anime: the scream starts right at the release, not at the
        // beginning of the charge (the clip leads the beam by ~0.3s).
        PENDING_SHOUTS.add(new PendingShout(now + Math.max(0, chargeTicks - 6), start));
    }

    private record PendingShout(long dueTick, Vec3 pos) {}
    private static final List<PendingShout> PENDING_SHOUTS = new CopyOnWriteArrayList<>();

    /** Boros screams the attack name; each player hears their configured language. */
    private static void playShout(Minecraft minecraft, Vec3 start) {
        SoundEvent voice = switch (ClientConfig.CSRC_VOICE.get()) {
            case ENGLISH -> RegisterSounds.CSRC_SHOUT_EN.get();
            case PORTUGUESE -> RegisterSounds.CSRC_SHOUT_PT.get();
            default -> RegisterSounds.CSRC_SHOUT_JP.get();
        };
        minecraft.level.playLocalSound(start.x, start.y, start.z, voice,
                SoundSource.PLAYERS, 3.0f, 1.0f, false);
    }

    /** Snapshot of the newest live effect, consumed by {@link CsrcPostChainHandler}. */
    public record ActiveVfx(Vec3 origin, Vec3 direction, double range, int chargeTicks, int fireTicks,
                            Vec3 impact, double impactRadius, float ageTicks) {}

    public static ActiveVfx activeVfx(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;

        long gameTime = minecraft.level.getGameTime();
        CsrcEffect newest = null;
        float newestAge = 0.0f;
        for (CsrcEffect effect : EFFECTS) {
            float age = gameTime - effect.createdTick + partialTick;
            if (age < 0 || age > effect.totalTicks()) continue;
            if (newest == null || effect.createdTick > newest.createdTick) {
                newest = effect;
                newestAge = age;
            }
        }
        if (newest == null) return null;

        Vec3 origin = resolveCoreOrigin(minecraft, newest, partialTick);
        return new ActiveVfx(origin, newest.direction, newest.range, newest.chargeTicks,
                newest.fireTicks, newest.impact, newest.impactRadius, newestAge);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            EFFECTS.clear();
            PENDING_SHOUTS.clear();
            return;
        }

        long now = minecraft.level.getGameTime();
        for (PendingShout shout : PENDING_SHOUTS) {
            if (now >= shout.dueTick) {
                playShout(minecraft, shout.pos);
                PENDING_SHOUTS.remove(shout);
            }
        }
        Iterator<CsrcEffect> iterator = EFFECTS.iterator();
        while (iterator.hasNext()) {
            CsrcEffect effect = iterator.next();
            if (now - effect.createdTick > effect.totalTicks() + 6L) {
                EFFECTS.remove(effect);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EFFECTS.isEmpty()) return;
        // The raymarched post chain replaces the quad geometry entirely;
        // these quads only render as fallback (e.g. Iris shaderpack active).
        if (CsrcPostChainHandler.isChainActive()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        long gameTime = minecraft.level.getGameTime();

        for (CsrcEffect effect : EFFECTS) {
            float age = gameTime - effect.createdTick + event.getPartialTick();
            if (age < 0 || age > effect.totalTicks()) continue;

            Vec3 origin = resolveCoreOrigin(minecraft, effect, event.getPartialTick());
            renderCharge(buffer, matrix, camera, origin, effect.direction, age, effect.chargeTicks);
            renderBeam(buffer, matrix, camera, origin, effect, age);
            renderMuzzleBlast(buffer, matrix, camera, origin, effect.direction, age, effect);
            renderImpact(buffer, matrix, camera, effect, age);
        }

        bufferSource.endBatch(RenderType.lightning());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (EFFECTS.isEmpty()) return;
        // Flash/chroma is handled by the post chain when it is available.
        if (CsrcPostChainHandler.isChainActive()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        long gameTime = minecraft.level.getGameTime();
        float strongest = 0.0f;
        float flash = 0.0f;
        for (CsrcEffect effect : EFFECTS) {
            float age = gameTime - effect.createdTick + event.getPartialTick();
            if (age < 0 || age > effect.totalTicks()) continue;

            float releaseAge = age - effect.chargeTicks * 0.35f;
            float chargePulse = 1.0f - Mth.clamp(age / Math.max(1.0f, effect.chargeTicks), 0.0f, 1.0f);
            float releasePulse = releaseAge < 0.0f ? 0.0f : 1.0f - Mth.clamp(releaseAge / (effect.fireTicks + AFTERGLOW_TICKS), 0.0f, 1.0f);
            strongest = Math.max(strongest, Math.max(chargePulse * 0.45f, releasePulse));
            flash = Math.max(flash, releaseAge >= 0.0f && releaseAge < 8.0f ? 1.0f - releaseAge / 8.0f : 0.0f);
        }

        if (strongest <= 0.01f && flash <= 0.01f) return;

        GuiGraphics gui = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        float time = (gameTime + event.getPartialTick()) * 0.11f;

        int purpleAlpha = clampAlpha(92.0f * strongest);
        int goldAlpha = clampAlpha(54.0f * strongest);
        gui.fill(0, 0, width, height, argb(purpleAlpha, 90, 12, 180));
        gui.fill(0, 0, width, height, argb(goldAlpha, 255, 196, 64));

        int chromaAlpha = clampAlpha(130.0f * strongest);
        int bandWidth = Math.max(4, (int) (width * 0.18f * strongest));
        int bandHeight = Math.max(3, (int) (height * 0.035f * strongest));
        int cy = height / 2;
        gui.fill(width / 2 - bandWidth - 7, cy - bandHeight - 2,
                width / 2 + bandWidth - 7, cy + bandHeight - 2,
                argb(chromaAlpha, 255, 32, 72));
        gui.fill(width / 2 - bandWidth + 7, cy - bandHeight + 2,
                width / 2 + bandWidth + 7, cy + bandHeight + 2,
                argb(chromaAlpha, 32, 220, 255));

        int scanAlpha = clampAlpha(42.0f * strongest);
        int offset = (int) (Math.abs(Mth.sin(time)) * 6.0f);
        for (int y = offset; y < height; y += 6) {
            gui.fill(0, y, width, y + 1, argb(scanAlpha, 255, 245, 190));
        }

        int pulseAlpha = clampAlpha(150.0f * strongest);
        int cx = width / 2;
        int pulseWidth = Math.max(2, (int) (width * 0.34f * strongest));
        int pulseHeight = Math.max(2, (int) (height * 0.08f * strongest));
        gui.fill(cx - pulseWidth, cy - 1, cx + pulseWidth, cy + 1, argb(pulseAlpha, 255, 230, 120));
        gui.fill(cx - 1, cy - pulseHeight, cx + 1, cy + pulseHeight, argb(pulseAlpha, 140, 210, 255));

        if (flash > 0.0f) {
            gui.fill(0, 0, width, height, argb(clampAlpha(210.0f * flash), 255, 248, 210));
        }
    }

    private static Vec3 resolveCoreOrigin(Minecraft minecraft, CsrcEffect effect, float partialTick) {
        Entity caster = minecraft.level == null ? null : minecraft.level.getEntity(effect.casterId);
        if (caster == null) return effect.start;

        double x = Mth.lerp(partialTick, caster.xo, caster.getX());
        double y = Mth.lerp(partialTick, caster.yo, caster.getY()) + caster.getBbHeight() * 0.62;
        double z = Mth.lerp(partialTick, caster.zo, caster.getZ());
        return new Vec3(x, y, z).add(effect.direction.scale(0.32));
    }

    private static void renderCharge(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 origin,
                                     Vec3 direction, float age, int chargeTicks) {
        float charge = Mth.clamp(age / Math.max(1.0f, chargeTicks), 0.0f, 1.0f);
        float fade = 1.0f - Mth.clamp((age - chargeTicks) / 16.0f, 0.0f, 1.0f);
        if (fade <= 0.0f) return;

        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.85f);
        renderBillboard(buffer, matrix, camera, origin, 1.2f + charge * 2.5f, 1.0f, 0.92f, 0.52f, 0.72f * fade * pulse);
        renderBillboard(buffer, matrix, camera, origin, 2.6f + charge * 4.2f, 0.58f, 0.16f, 0.95f, 0.42f * fade);

        Vec3 side = stableSide(direction);
        Vec3 up = side.cross(direction).normalize();
        for (int i = 0; i < 3; i++) {
            float radius = 1.5f + i * 0.55f + charge * 1.1f;
            float alpha = (0.35f - i * 0.06f) * fade;
            renderRing(buffer, matrix, camera, origin.add(direction.scale(0.12 * i)), side, up, radius, 28 + i * 8,
                    0.85f, 0.42f, 1.0f, alpha, age * (0.08f + i * 0.02f));
        }
    }

    private static void renderBeam(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 origin,
                                   CsrcEffect effect, float age) {
        float releaseAge = age - effect.chargeTicks * 0.35f;
        if (releaseAge < 0.0f) return;

        float fireProgress = Mth.clamp(releaseAge / Math.max(1.0f, effect.fireTicks), 0.0f, 1.0f);
        float afterglow = 1.0f - Mth.clamp((releaseAge - effect.fireTicks) / AFTERGLOW_TICKS, 0.0f, 1.0f);
        if (afterglow <= 0.0f) return;

        Vec3 end = origin.add(effect.direction.scale(effect.range));
        float travel = Mth.clamp(fireProgress * 1.4f, 0.0f, 1.0f);
        Vec3 visibleEnd = origin.lerp(end, travel);
        float width = (float) (8.5f + effect.impactRadius * 0.08f) * (0.85f + 0.15f * Mth.sin(age * 0.9f));

        renderBeamStrip(buffer, matrix, camera, origin, visibleEnd, width * 0.55f, 1.0f, 0.94f, 0.58f, 0.72f * afterglow);
        renderBeamStrip(buffer, matrix, camera, origin, visibleEnd, width * 1.05f, 0.62f, 0.18f, 0.95f, 0.34f * afterglow);
        renderBeamStrip(buffer, matrix, camera, origin, visibleEnd, width * 1.55f, 0.12f, 0.72f, 1.0f, 0.2f * afterglow);

        Vec3 side = stableSide(effect.direction);
        Vec3 up = side.cross(effect.direction).normalize();
        for (int i = 0; i < 11; i++) {
            float progress = (fireProgress * 1.35f - i * 0.12f);
            if (progress <= 0.0f || progress >= 1.0f) continue;
            Vec3 center = origin.lerp(end, progress);
            renderRing(buffer, matrix, camera, center, side, up, width * (0.75f + progress * 0.75f),
                    22, 1.0f, 0.74f, 0.42f, 0.32f * afterglow * (1.0f - progress), age * 0.16f + i);
        }

        renderArcs(buffer, matrix, camera, origin, visibleEnd, effect.direction, age, width, afterglow);
        renderBeamRibbons(buffer, matrix, camera, origin, visibleEnd, effect.direction, age, width, afterglow);
    }

    private static void renderMuzzleBlast(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 origin,
                                          Vec3 direction, float age, CsrcEffect effect) {
        float releaseAge = age - effect.chargeTicks * 0.35f;
        if (releaseAge < 0.0f) return;

        float fade = 1.0f - Mth.clamp(releaseAge / 48.0f, 0.0f, 1.0f);
        if (fade <= 0.0f) return;

        float radius = 8.0f + releaseAge * 2.6f;
        renderEnergyDome(buffer, matrix, camera, origin.add(direction.scale(-2.5)), direction,
                radius, 0.34f * fade, releaseAge, 0.78f, 0.18f, 1.0f);
        renderRadialBurst(buffer, matrix, camera, origin.add(direction.scale(-1.5)), direction,
                radius * 1.25f, 18, 0.42f * fade, releaseAge);
    }

    private static void renderImpact(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, CsrcEffect effect, float age) {
        float releaseAge = age - effect.chargeTicks * 0.35f;
        if (releaseAge < effect.fireTicks * 0.28f) return;

        float impactAge = releaseAge - effect.fireTicks * 0.28f;
        float fade = 1.0f - Mth.clamp(impactAge / (effect.fireTicks + AFTERGLOW_TICKS), 0.0f, 1.0f);
        if (fade <= 0.0f) return;

        float flashSize = (float) (effect.impactRadius * 0.48f + impactAge * 0.65f);
        renderBillboard(buffer, matrix, camera, effect.impact, flashSize, 1.0f, 0.88f, 0.52f, 0.48f * fade);
        renderBillboard(buffer, matrix, camera, effect.impact, flashSize * 1.45f, 0.42f, 0.18f, 1.0f, 0.25f * fade);

        Vec3 side = stableSide(effect.direction);
        Vec3 up = new Vec3(0, 1, 0);
        float ringRadius = (float) Math.min(effect.impactRadius * 2.6f, 12.0f + impactAge * 3.4f);
        renderRing(buffer, matrix, camera, effect.impact.add(0, 0.2, 0), side, up, ringRadius,
                64, 1.0f, 0.58f, 0.26f, 0.3f * fade, impactAge * 0.06f);
        renderImpactShell(buffer, matrix, camera, effect.impact, effect.direction, ringRadius, fade, impactAge);
        renderEnergyDome(buffer, matrix, camera, effect.impact, effect.direction,
                ringRadius * 0.92f, 0.32f * fade, impactAge, 1.0f, 0.78f, 0.42f);
        renderRadialBurst(buffer, matrix, camera, effect.impact, effect.direction,
                ringRadius * 1.35f, 28, 0.36f * fade, impactAge);
    }

    private static void renderArcs(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 origin, Vec3 end,
                                   Vec3 direction, float age, float width, float fade) {
        Vec3 side = stableSide(direction);
        Vec3 up = side.cross(direction).normalize();
        for (int i = 0; i < 9; i++) {
            float startProgress = (i + 1) / 11.0f;
            Vec3 a = origin.lerp(end, startProgress);
            Vec3 b = origin.lerp(end, Math.min(1.0f, startProgress + 0.055f));
            double angle = age * 0.27 + i * 1.41;
            Vec3 offset = side.scale(Math.cos(angle) * width * 0.68).add(up.scale(Math.sin(angle) * width * 0.68));
            Vec3 offsetB = side.scale(Math.cos(angle + 0.7) * width * 0.92).add(up.scale(Math.sin(angle + 0.7) * width * 0.92));
            renderBeamStrip(buffer, matrix, camera, a.add(offset), b.add(offsetB), width * 0.16f,
                    0.35f, 0.78f, 1.0f, 0.46f * fade);
        }
    }

    private static void renderBeamRibbons(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 origin, Vec3 end,
                                          Vec3 direction, float age, float width, float fade) {
        Vec3 side = stableSide(direction);
        Vec3 up = side.cross(direction).normalize();
        for (int i = 0; i < 6; i++) {
            double angle = age * 0.08 + i * Math.PI / 3.0;
            Vec3 offsetA = side.scale(Math.cos(angle) * width * 0.92).add(up.scale(Math.sin(angle) * width * 0.92));
            Vec3 offsetB = side.scale(Math.cos(angle + 0.9) * width * 1.18).add(up.scale(Math.sin(angle + 0.9) * width * 1.18));
            renderBeamStrip(buffer, matrix, camera, origin.add(offsetA), end.add(offsetB), width * 0.11f,
                    i % 2 == 0 ? 1.0f : 0.45f,
                    i % 2 == 0 ? 0.78f : 0.22f,
                    i % 2 == 0 ? 0.42f : 1.0f,
                    0.24f * fade);
        }
    }

    private static void renderImpactShell(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 center,
                                          Vec3 direction, float radius, float fade, float age) {
        Vec3 side = stableSide(direction);
        Vec3 up = side.cross(direction).normalize();
        float alpha = 0.18f * fade;
        renderRing(buffer, matrix, camera, center, side, up, radius * 0.95f, 96, 1.0f, 0.9f, 0.62f, alpha, age * 0.03f);
        renderRing(buffer, matrix, camera, center, direction, up, radius * 0.85f, 96, 0.72f, 0.22f, 1.0f, alpha, age * -0.025f);
        renderRing(buffer, matrix, camera, center, direction, side, radius * 0.75f, 96, 0.22f, 0.8f, 1.0f, alpha * 0.85f, age * 0.04f);

        for (int i = 0; i < 5; i++) {
            float scale = 0.35f + i * 0.16f;
            renderBillboard(buffer, matrix, camera, center.add(direction.scale(-i * 1.5)),
                    radius * scale, 1.0f, 0.82f, 0.52f, alpha * (1.0f - i * 0.12f));
        }
    }

    private static void renderEnergyDome(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 center,
                                         Vec3 direction, float radius, float alpha, float age,
                                         float r, float g, float b) {
        Vec3 side = stableSide(direction);
        Vec3 up = side.cross(direction).normalize();

        for (int i = 0; i < 7; i++) {
            float scale = 0.25f + i * 0.13f;
            float spin = age * (0.018f + i * 0.004f);
            renderRing(buffer, matrix, camera, center, side, up, radius * scale, 96, r, g, b, alpha * (1.0f - i * 0.09f), spin);
        }

        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0 + age * 0.025;
            Vec3 axisA = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))).normalize();
            renderRing(buffer, matrix, camera, center, direction, axisA, radius * 0.75f, 64,
                    i % 2 == 0 ? r : 0.28f,
                    i % 2 == 0 ? g : 0.78f,
                    i % 2 == 0 ? b : 1.0f,
                    alpha * 0.55f, age * 0.012f + i);
        }
    }

    private static void renderRadialBurst(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 center,
                                          Vec3 direction, float radius, int spokes, float alpha, float age) {
        Vec3 side = stableSide(direction);
        Vec3 up = side.cross(direction).normalize();

        for (int i = 0; i < spokes; i++) {
            double angle = Math.PI * 2.0 * i / spokes + age * 0.035;
            double wobble = 0.7 + 0.3 * Math.sin(age * 0.14 + i);
            Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))).normalize();
            Vec3 start = center.add(radial.scale(radius * 0.15));
            Vec3 end = center.add(radial.scale(radius * wobble)).add(direction.scale((i % 3 - 1) * radius * 0.08));
            renderBeamStrip(buffer, matrix, camera, start, end, Math.max(0.18f, radius * 0.018f),
                    1.0f, i % 2 == 0 ? 0.72f : 0.22f, i % 2 == 0 ? 0.38f : 1.0f, alpha);
        }
    }

    private static void renderBeamStrip(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 start, Vec3 end,
                                        float width, float r, float g, float b, float alpha) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 0.001) return;

        Vec3 toCamera = camera.subtract(start.add(end).scale(0.5));
        Vec3 side = direction.cross(toCamera);
        if (side.lengthSqr() < 0.001) side = stableSide(direction);
        side = side.normalize().scale(width);

        addVertex(buffer, matrix, start.add(side), r, g, b, alpha);
        addVertex(buffer, matrix, start.subtract(side), r, g, b, alpha);
        addVertex(buffer, matrix, end.subtract(side), r, g, b, alpha);
        addVertex(buffer, matrix, end.add(side), r, g, b, alpha);
    }

    private static void renderBillboard(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 center,
                                        float size, float r, float g, float b, float alpha) {
        Vec3 forward = camera.subtract(center);
        if (forward.lengthSqr() < 0.001) forward = new Vec3(0, 0, 1);
        forward = forward.normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
        right = right.normalize().scale(size);
        Vec3 up = right.cross(forward).normalize().scale(size);

        addVertex(buffer, matrix, center.subtract(right).subtract(up), r, g, b, alpha);
        addVertex(buffer, matrix, center.add(right).subtract(up), r, g, b, alpha);
        addVertex(buffer, matrix, center.add(right).add(up), r, g, b, alpha);
        addVertex(buffer, matrix, center.subtract(right).add(up), r, g, b, alpha);
    }

    private static void renderRing(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 center, Vec3 side, Vec3 up,
                                   float radius, int segments, float r, float g, float b, float alpha, float spin) {
        float thickness = Math.max(0.08f, radius * 0.035f);
        for (int i = 0; i < segments; i++) {
            double a0 = spin + Math.PI * 2.0 * i / segments;
            double a1 = spin + Math.PI * 2.0 * (i + 1) / segments;
            Vec3 p0 = center.add(side.scale(Math.cos(a0) * radius)).add(up.scale(Math.sin(a0) * radius));
            Vec3 p1 = center.add(side.scale(Math.cos(a1) * radius)).add(up.scale(Math.sin(a1) * radius));
            renderBeamStrip(buffer, matrix, camera, p0, p1, thickness, r, g, b, alpha);
        }
    }

    private static Vec3 stableSide(Vec3 direction) {
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.001) side = direction.cross(new Vec3(1, 0, 0));
        return side.normalize();
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f matrix, Vec3 pos,
                                  float r, float g, float b, float alpha) {
        buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(r, g, b, alpha)
                .endVertex();
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    private static int clampAlpha(float alpha) {
        return Mth.clamp((int) alpha, 0, 255);
    }

    private record CsrcEffect(int casterId, Vec3 start, Vec3 direction, double range,
                              int chargeTicks, int fireTicks, Vec3 impact,
                              double impactRadius, long createdTick) {
        int totalTicks() {
            return chargeTicks + fireTicks + AFTERGLOW_TICKS;
        }
    }
}
