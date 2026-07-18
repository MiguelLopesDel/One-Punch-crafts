package com.onepunchcrafts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * One batch of additive world-space quads on {@code RenderType.lightning()},
 * shared by every VFX renderer in the mod. This is the single place that
 * knows the two render-stage gotchas:
 *
 * <ul>
 *   <li>the stage pose stack only carries the camera rotation, so geometry
 *       must be emitted relative to the camera position ({@link #begin}
 *       pushes the translation, {@link #close} pops it);</li>
 *   <li>{@code lightning()} culls back faces, so strips emit both windings
 *       and billboards use a camera-facing winding.</li>
 * </ul>
 *
 * Usage: {@code try (VfxQuadBatch batch = VfxQuadBatch.begin(event)) { ... }}
 */
public final class VfxQuadBatch implements AutoCloseable {

    private final MultiBufferSource.BufferSource bufferSource;
    private final VertexConsumer buffer;
    private final PoseStack poseStack;
    private final Matrix4f matrix;
    private final Vec3 camera;

    private VfxQuadBatch(MultiBufferSource.BufferSource bufferSource, VertexConsumer buffer,
                         PoseStack poseStack, Matrix4f matrix, Vec3 camera) {
        this.bufferSource = bufferSource;
        this.buffer = buffer;
        this.poseStack = poseStack;
        this.matrix = matrix;
        this.camera = camera;
    }

    public static VfxQuadBatch begin(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        return new VfxQuadBatch(bufferSource, buffer, poseStack, poseStack.last().pose(), camera);
    }

    @Override
    public void close() {
        bufferSource.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    public Vec3 camera() {
        return camera;
    }

    /** Camera-aligned strip between two world points. */
    public void strip(Vec3 start, Vec3 end, float width, float r, float g, float b, float alpha) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 0.001) return;

        Vec3 toCamera = camera.subtract(start.add(end).scale(0.5));
        Vec3 side = direction.cross(toCamera);
        if (side.lengthSqr() < 0.001) side = stableSide(direction);
        side = side.normalize().scale(width);

        vertex(start.add(side), r, g, b, alpha);
        vertex(start.subtract(side), r, g, b, alpha);
        vertex(end.subtract(side), r, g, b, alpha);
        vertex(end.add(side), r, g, b, alpha);

        vertex(end.add(side), r, g, b, alpha);
        vertex(end.subtract(side), r, g, b, alpha);
        vertex(start.subtract(side), r, g, b, alpha);
        vertex(start.add(side), r, g, b, alpha);
    }

    /** Camera-facing square flash. */
    public void billboard(Vec3 center, float size, float r, float g, float b, float alpha) {
        Vec3 forward = camera.subtract(center);
        if (forward.lengthSqr() < 0.001) forward = new Vec3(0, 0, 1);
        forward = forward.normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
        right = right.normalize().scale(size);
        Vec3 up = right.cross(forward).normalize().scale(size);

        vertex(center.subtract(right).add(up), r, g, b, alpha);
        vertex(center.add(right).add(up), r, g, b, alpha);
        vertex(center.add(right).subtract(up), r, g, b, alpha);
        vertex(center.subtract(right).subtract(up), r, g, b, alpha);
    }

    /** Ring in the plane spanned by {@code side}/{@code up}, default thickness. */
    public void ring(Vec3 center, Vec3 side, Vec3 up, float radius, int segments,
                     float r, float g, float b, float alpha, float spin) {
        ring(center, side, up, radius, Math.max(0.06f, radius * 0.045f), segments, r, g, b, alpha, spin);
    }

    public void ring(Vec3 center, Vec3 side, Vec3 up, float radius, float thickness, int segments,
                     float r, float g, float b, float alpha, float spin) {
        for (int i = 0; i < segments; i++) {
            double a0 = spin + Math.PI * 2.0 * i / segments;
            double a1 = spin + Math.PI * 2.0 * (i + 1) / segments;
            Vec3 p0 = center.add(side.scale(Math.cos(a0) * radius)).add(up.scale(Math.sin(a0) * radius));
            Vec3 p1 = center.add(side.scale(Math.cos(a1) * radius)).add(up.scale(Math.sin(a1) * radius));
            strip(p0, p1, thickness, r, g, b, alpha);
        }
    }

    /** Any unit vector perpendicular to {@code direction}, stable across frames. */
    public static Vec3 stableSide(Vec3 direction) {
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.001) side = direction.cross(new Vec3(1, 0, 0));
        return side.normalize();
    }

    /** Deterministic 0..1 hash for per-element variation without RNG state. */
    public static float hash(int seed) {
        int h = seed * 0x9E3779B9;
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private void vertex(Vec3 pos, float r, float g, float b, float alpha) {
        buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(r, g, b, alpha)
                .endVertex();
    }
}
