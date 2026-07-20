package com.onepunchcrafts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.onepunchcrafts.common.block.entity.PortalBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

/** Renders the fallback portal as a tall emissive tear instead of a cube. */
public final class PortalBlockEntityRenderer implements BlockEntityRenderer<PortalBlockEntity> {
    private static final ResourceLocation RIFT_TEXTURE =
            new ResourceLocation(MODID, "textures/block/dimensional_portal_rift.png");
    private static final float WIDTH = 1.65f;
    private static final float HEIGHT = 2.9f;
    private static final int RIM_SEGMENTS = 36;

    public PortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PortalBlockEntity portal, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        float time = minecraft.level.getGameTime() + partialTick;
        float pulse = 1.0f + 0.025f * (float) Math.sin(time * 0.18f);

        poseStack.pushPose();
        poseStack.translate(0.5, 1.46, 0.5);

        // Keep the rift vertical while turning only around Y to face observers.
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 toCamera = camera.getPosition().subtract(Vec3.atCenterOf(portal.getBlockPos()));
        poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(toCamera.x, toCamera.z)));
        poseStack.scale(pulse, pulse, pulse);

        renderRiftTexture(poseStack, buffers, time);
        renderLivingRim(poseStack, buffers, time);
        renderOrbitingShards(poseStack, buffers, time);
        poseStack.popPose();
    }

    private static void renderRiftTexture(PoseStack poseStack, MultiBufferSource buffers, float time) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(RIFT_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        float shimmer = 0.94f + 0.06f * (float) Math.sin(time * 0.31f);
        float halfWidth = WIDTH * 0.5f;
        float halfHeight = HEIGHT * 0.5f;

        texturedVertex(vertices, matrix, -halfWidth, -halfHeight, 0.0f, 0, 1, shimmer);
        texturedVertex(vertices, matrix, halfWidth, -halfHeight, 0.0f, 1, 1, shimmer);
        texturedVertex(vertices, matrix, halfWidth, halfHeight, 0.0f, 1, 0, shimmer);
        texturedVertex(vertices, matrix, -halfWidth, halfHeight, 0.0f, 0, 0, shimmer);

        // Back face for observers standing on the other side of the tear.
        texturedVertex(vertices, matrix, -halfWidth, halfHeight, -0.006f, 0, 0, shimmer);
        texturedVertex(vertices, matrix, halfWidth, halfHeight, -0.006f, 1, 0, shimmer);
        texturedVertex(vertices, matrix, halfWidth, -halfHeight, -0.006f, 1, 1, shimmer);
        texturedVertex(vertices, matrix, -halfWidth, -halfHeight, -0.006f, 0, 1, shimmer);
    }

    private static void renderLivingRim(PoseStack poseStack, MultiBufferSource buffers, float time) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < RIM_SEGMENTS; i++) {
            float a1 = (float) (Math.PI * 2.0 * i / RIM_SEGMENTS);
            float a2 = (float) (Math.PI * 2.0 * (i + 1) / RIM_SEGMENTS);
            float jitter1 = 1.0f + 0.045f * (float) Math.sin(i * 5.17f + time * 0.27f);
            float jitter2 = 1.0f + 0.045f * (float) Math.sin((i + 1) * 5.17f + time * 0.27f);
            float x1 = (float) Math.cos(a1) * WIDTH * 0.42f * jitter1;
            float y1 = (float) Math.sin(a1) * HEIGHT * 0.47f * jitter1;
            float x2 = (float) Math.cos(a2) * WIDTH * 0.42f * jitter2;
            float y2 = (float) Math.sin(a2) * HEIGHT * 0.47f * jitter2;
            boolean cyan = (i + (int) (time / 4)) % 5 == 0;
            addGlowSegment(vertices, matrix, x1, y1, x2, y2, 0.014f,
                    cyan ? 0.30f : 0.67f, cyan ? 0.92f : 0.22f, 1.0f, 0.75f);
        }
    }

    private static void renderOrbitingShards(PoseStack poseStack, MultiBufferSource buffers, float time) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < 12; i++) {
            float phase = i * 2.399f + time * (0.012f + i % 3 * 0.004f);
            float orbit = 0.76f + hash(i * 31) * 0.24f;
            float x = (float) Math.cos(phase) * WIDTH * orbit * 0.55f;
            float y = (hash(i * 71) - 0.5f) * HEIGHT * 0.94f
                    + 0.08f * (float) Math.sin(time * 0.08f + i);
            float size = 0.018f + hash(i * 13) * 0.024f;
            addDiamond(vertices, matrix, x, y, 0.012f, size,
                    i % 3 == 0 ? 0.35f : 0.72f, i % 3 == 0 ? 0.90f : 0.28f, 1.0f, 0.7f);
        }
    }

    private static void texturedVertex(VertexConsumer vertices, Matrix4f matrix,
                                       float x, float y, float z, float u, float v, float alpha) {
        vertices.vertex(matrix, x, y, z)
                .color(1.0f, 1.0f, 1.0f, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 0, 1)
                .endVertex();
    }

    private static void addGlowSegment(VertexConsumer vertices, Matrix4f matrix,
                                       float x1, float y1, float x2, float y2, float width,
                                       float red, float green, float blue, float alpha) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0e-4f) return;
        float px = -dy / length * width;
        float py = dx / length * width;
        glowVertex(vertices, matrix, x1 - px, y1 - py, 0.01f, red, green, blue, alpha);
        glowVertex(vertices, matrix, x1 + px, y1 + py, 0.01f, red, green, blue, alpha);
        glowVertex(vertices, matrix, x2 + px, y2 + py, 0.01f, red, green, blue, alpha);
        glowVertex(vertices, matrix, x2 - px, y2 - py, 0.01f, red, green, blue, alpha);
    }

    private static void addDiamond(VertexConsumer vertices, Matrix4f matrix,
                                   float x, float y, float z, float size,
                                   float red, float green, float blue, float alpha) {
        glowVertex(vertices, matrix, x, y - size * 1.8f, z, red, green, blue, alpha);
        glowVertex(vertices, matrix, x + size, y, z, red, green, blue, alpha);
        glowVertex(vertices, matrix, x, y + size * 1.8f, z, red, green, blue, alpha);
        glowVertex(vertices, matrix, x - size, y, z, red, green, blue, alpha);
    }

    private static void glowVertex(VertexConsumer vertices, Matrix4f matrix,
                                   float x, float y, float z,
                                   float red, float green, float blue, float alpha) {
        vertices.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static float hash(int value) {
        int mixed = value * 0x45d9f3b;
        mixed = (mixed ^ (mixed >>> 16)) * 0x45d9f3b;
        mixed ^= mixed >>> 16;
        return (mixed & 0xFFFFFF) / (float) 0x1000000;
    }

    @Override
    public boolean shouldRenderOffScreen(PortalBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
