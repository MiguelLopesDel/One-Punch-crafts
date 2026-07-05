package com.onepunchcrafts.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onepunchcrafts.network.packet.BorosBeamVfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

/**
 * Quad-based beam VFX for Boros' Energy Projection and the normal Roaring
 * Cannon: layered additive beam + counter-rotating helix ribbons + traveling
 * rings + muzzle/impact flashes. Deliberately lighter than the CSRC post
 * chain, and it works regardless of shaderpacks.
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class BorosBeamVfxRenderer {

    /** Palette and proportions for each beam style. */
    private record BeamStyle(float width, float[] core, float[] mid, float[] glow) {}

    private static final BeamStyle ENERGY_PROJECTION = new BeamStyle(1.15f,
            new float[]{1.0f, 0.88f, 1.0f}, new float[]{1.0f, 0.36f, 0.95f}, new float[]{0.55f, 0.16f, 0.92f});
    private static final BeamStyle ROARING_CANNON = new BeamStyle(2.3f,
            new float[]{1.0f, 0.92f, 1.0f}, new float[]{0.82f, 0.32f, 1.0f}, new float[]{1.0f, 0.68f, 0.28f});
    private static final BeamStyle ROARING_METEORIC = new BeamStyle(3.5f,
            new float[]{0.92f, 1.0f, 1.0f}, new float[]{0.30f, 0.88f, 1.0f}, new float[]{1.0f, 0.72f, 0.25f});

    private record BeamEffect(int casterId, Vec3 start, Vec3 direction, double range,
                              int style, int lifeTicks, long createdTick) {}

    private static final List<BeamEffect> EFFECTS = new CopyOnWriteArrayList<>();

    private BorosBeamVfxRenderer() {}

    public static void addEffect(int casterId, Vec3 start, Vec3 direction, double range, int style, int lifeTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        EFFECTS.add(new BeamEffect(casterId, start, direction.normalize(), range, style, lifeTicks,
                minecraft.level.getGameTime()));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            EFFECTS.clear();
            return;
        }
        long now = minecraft.level.getGameTime();
        EFFECTS.removeIf(effect -> now - effect.createdTick > effect.lifeTicks + 2L);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EFFECTS.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());
        Vec3 camera = event.getCamera().getPosition();
        // The stage pose stack only carries the camera rotation; world-space
        // geometry must be emitted relative to the camera position.
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = event.getPoseStack().last().pose();
        long gameTime = minecraft.level.getGameTime();

        for (BeamEffect effect : EFFECTS) {
            float age = gameTime - effect.createdTick + event.getPartialTick();
            if (age < 0 || age > effect.lifeTicks) continue;
            renderBeam(buffer, matrix, camera, minecraft, effect, age);
        }

        bufferSource.endBatch(RenderType.lightning());
        event.getPoseStack().popPose();
    }

    private static void renderBeam(VertexConsumer buffer, Matrix4f matrix, Vec3 camera,
                                   Minecraft minecraft, BeamEffect effect, float age) {
        BeamStyle style = switch (effect.style) {
            case BorosBeamVfxPacket.STYLE_ROARING_CANNON -> ROARING_CANNON;
            case BorosBeamVfxPacket.STYLE_ROARING_CANNON_METEORIC -> ROARING_METEORIC;
            default -> ENERGY_PROJECTION;
        };

        float progress = age / Math.max(1.0f, effect.lifeTicks);
        // The beam shoots out fast, holds, then dissolves over the last third.
        float travel = Mth.clamp(progress * 2.4f, 0.0f, 1.0f);
        float fade = 1.0f - Mth.clamp((progress - 0.62f) / 0.38f, 0.0f, 1.0f);
        if (fade <= 0.0f) return;

        Vec3 origin = resolveMuzzle(minecraft, effect);
        Vec3 end = origin.add(effect.direction.scale(effect.range));
        Vec3 visibleEnd = origin.lerp(end, travel);
        float pulse = 0.88f + 0.12f * Mth.sin(age * 1.4f);
        float width = style.width * pulse * (0.75f + 0.25f * travel);

        renderBeamStrip(buffer, matrix, camera, origin, visibleEnd, width * 0.45f,
                style.core[0], style.core[1], style.core[2], 0.85f * fade);
        renderBeamStrip(buffer, matrix, camera, origin, visibleEnd, width * 0.95f,
                style.mid[0], style.mid[1], style.mid[2], 0.42f * fade);
        renderBeamStrip(buffer, matrix, camera, origin, visibleEnd, width * 1.65f,
                style.glow[0], style.glow[1], style.glow[2], 0.20f * fade);

        Vec3 side = stableSide(effect.direction);
        Vec3 up = side.cross(effect.direction).normalize();

        // Twin helix ribbons corkscrewing along the beam.
        int segments = (int) Math.min(64, Math.max(20, effect.range / 3.0));
        for (int ribbon = 0; ribbon < 2; ribbon++) {
            double phase = ribbon * Math.PI + age * 0.55;
            Vec3 previous = null;
            for (int i = 0; i <= segments; i++) {
                double t = (double) i / segments * travel;
                double angle = phase + t * effect.range * 0.55;
                double radius = width * (1.15 + 0.35 * Math.sin(t * 9.0 + age * 0.3));
                Vec3 point = origin.lerp(end, t)
                        .add(side.scale(Math.cos(angle) * radius))
                        .add(up.scale(Math.sin(angle) * radius));
                if (previous != null) {
                    renderBeamStrip(buffer, matrix, camera, previous, point, width * 0.14f,
                            style.glow[0], style.glow[1], style.glow[2], 0.5f * fade);
                }
                previous = point;
            }
        }

        // Rings racing down the beam.
        for (int i = 0; i < 5; i++) {
            float ringT = (progress * 2.0f + i * 0.19f) % 1.0f;
            if (ringT > travel) continue;
            Vec3 center = origin.lerp(end, ringT);
            renderRing(buffer, matrix, camera, center, side, up, width * (1.4f + ringT * 1.3f), 20,
                    style.mid[0], style.mid[1], style.mid[2], 0.34f * fade * (1.0f - ringT * 0.6f), age * 0.2f + i);
        }

        // Muzzle flare while the shot leaves the hand.
        float muzzleFade = 1.0f - Mth.clamp(progress / 0.45f, 0.0f, 1.0f);
        if (muzzleFade > 0.0f) {
            renderBillboard(buffer, matrix, camera, origin, width * (2.0f + progress * 2.0f),
                    style.core[0], style.core[1], style.core[2], 0.6f * muzzleFade);
            renderBillboard(buffer, matrix, camera, origin, width * (3.4f + progress * 2.4f),
                    style.glow[0], style.glow[1], style.glow[2], 0.3f * muzzleFade);
        }

        // Impact bloom once the front lands.
        if (travel >= 0.999f) {
            float impactAge = age - effect.lifeTicks / 2.4f;
            float impactFade = fade * (0.75f + 0.25f * Mth.sin(age * 1.7f));
            renderBillboard(buffer, matrix, camera, end, width * (2.6f + impactAge * 0.4f),
                    style.core[0], style.core[1], style.core[2], 0.55f * impactFade);
            renderRing(buffer, matrix, camera, end, side, up, width * (3.0f + impactAge * 0.8f), 28,
                    style.glow[0], style.glow[1], style.glow[2], 0.3f * impactFade, age * 0.12f);
        }
    }

    /** Follow the caster's hand while they are visible so the beam tracks them. */
    private static Vec3 resolveMuzzle(Minecraft minecraft, BeamEffect effect) {
        Entity caster = minecraft.level == null ? null : minecraft.level.getEntity(effect.casterId);
        if (caster == null) return effect.start;
        double x = Mth.lerp(minecraft.getPartialTick(), caster.xo, caster.getX());
        double y = Mth.lerp(minecraft.getPartialTick(), caster.yo, caster.getY()) + caster.getBbHeight() * 0.72;
        double z = Mth.lerp(minecraft.getPartialTick(), caster.zo, caster.getZ());
        return new Vec3(x, y, z).add(effect.direction.scale(1.1));
    }

    private static void renderBeamStrip(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 start, Vec3 end,
                                        float width, float r, float g, float b, float alpha) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 0.001) return;

        Vec3 toCamera = camera.subtract(start.add(end).scale(0.5));
        Vec3 side = direction.cross(toCamera);
        if (side.lengthSqr() < 0.001) side = stableSide(direction);
        side = side.normalize().scale(width);

        // RenderType.lightning() culls back faces, so emit both windings —
        // exactly one of them survives from any camera angle.
        addVertex(buffer, matrix, start.add(side), r, g, b, alpha);
        addVertex(buffer, matrix, start.subtract(side), r, g, b, alpha);
        addVertex(buffer, matrix, end.subtract(side), r, g, b, alpha);
        addVertex(buffer, matrix, end.add(side), r, g, b, alpha);

        addVertex(buffer, matrix, end.add(side), r, g, b, alpha);
        addVertex(buffer, matrix, end.subtract(side), r, g, b, alpha);
        addVertex(buffer, matrix, start.subtract(side), r, g, b, alpha);
        addVertex(buffer, matrix, start.add(side), r, g, b, alpha);
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

        // Camera-facing winding (lightning() culls back faces).
        addVertex(buffer, matrix, center.subtract(right).add(up), r, g, b, alpha);
        addVertex(buffer, matrix, center.add(right).add(up), r, g, b, alpha);
        addVertex(buffer, matrix, center.add(right).subtract(up), r, g, b, alpha);
        addVertex(buffer, matrix, center.subtract(right).subtract(up), r, g, b, alpha);
    }

    private static void renderRing(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Vec3 center, Vec3 side, Vec3 up,
                                   float radius, int segments, float r, float g, float b, float alpha, float spin) {
        float thickness = Math.max(0.06f, radius * 0.045f);
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
}
