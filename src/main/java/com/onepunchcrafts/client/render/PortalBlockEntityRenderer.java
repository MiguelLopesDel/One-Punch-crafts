package com.onepunchcrafts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.onepunchcrafts.common.block.entity.PortalBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

public class PortalBlockEntityRenderer implements BlockEntityRenderer<PortalBlockEntity> {
    private static final int SEGMENTS = 32;
    private static final float PORTAL_WIDTH = 1.0f;
    private static final float PORTAL_HEIGHT = 2.0f;
    private static final int PARTICLE_COUNT = 16;
    private final float[] particleOffsets = new float[PARTICLE_COUNT];
    private final float[] particlePhases = new float[PARTICLE_COUNT];
    private final float[] particleColors = new float[PARTICLE_COUNT * 3];

    public PortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        Random random = new Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleOffsets[i] = random.nextFloat();
            particlePhases[i] = random.nextFloat() * (float) Math.PI * 2;
            particleColors[i * 3] = random.nextFloat();
            particleColors[i * 3 + 1] = random.nextFloat();
            particleColors[i * 3 + 2] = random.nextFloat();
        }
    }

    @Override
    public void render(PortalBlockEntity portalEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 blockPos = Vec3.atCenterOf(portalEntity.getBlockPos());
        Vec3 direction = cameraPos.subtract(blockPos).normalize();

        double rotY = Math.atan2(direction.x, direction.z);
        double rotX = Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z));

        poseStack.mulPose(Axis.YP.rotation((float) rotY));
        poseStack.mulPose(Axis.XP.rotation((float) rotX));

        renderPortalOval(poseStack, bufferSource, combinedLight);
        renderGlowingParticles(poseStack, bufferSource, combinedLight, partialTicks);

        poseStack.popPose();
    }

    private void renderGlowingParticles(PoseStack poseStack, MultiBufferSource bufferSource, int light, float partialTicks) {
        float time = (Minecraft.getInstance().level.getGameTime() + partialTicks) / 20.0f;
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float phase = particlePhases[i] + time;
            float chaosFactor = (float) Math.sin(time * 0.5) * 0.5f + 0.5f;
            float offset = particleOffsets[i];

            float progress = ((time + offset) % 3.0f) / 3.0f;
            float radius = (1 - progress) * PORTAL_WIDTH * 0.5f;

            float angle = phase + chaosFactor * (float) Math.sin(time * 2 + offset * 10) * 2;
            float x = (float) (radius * Math.cos(angle));
            float y = (float) (radius * Math.sin(angle));

            float r = particleColors[i * 3];
            float g = particleColors[i * 3 + 1];
            float b = particleColors[i * 3 + 2];

            float alpha = (1 - progress) * (0.8f + 0.2f * (float) Math.sin(time * 5 + offset * 20));

            renderGlowingParticle(buffer, matrix, x, y, r, g, b, alpha);
        }
    }

    private void renderGlowingParticle(VertexConsumer buffer, Matrix4f matrix, float x, float y,
                                       float r, float g, float b, float alpha) {
        float size = 0.05f;
        buffer.vertex(matrix, x - size, y - size, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x + size, y - size, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x + size, y + size, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x - size, y + size, 0).color(r, g, b, alpha).endVertex();
    }

    private void renderPortalOval(PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i < SEGMENTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / SEGMENTS);
            float angle2 = (float) (2 * Math.PI * (i + 1) / SEGMENTS);

            float x1 = (float) (PORTAL_WIDTH * Math.cos(angle1) * 0.5);
            float y1 = (float) (PORTAL_HEIGHT * Math.sin(angle1) * 0.5);
            float x2 = (float) (PORTAL_WIDTH * Math.cos(angle2) * 0.5);
            float y2 = (float) (PORTAL_HEIGHT * Math.sin(angle2) * 0.5);

            addVertex(buffer, matrix, 0, 0, 0, 0.5f, 0.5f, light);
            addVertex(buffer, matrix, x1, y1, 0, 0, 1, light);
            addVertex(buffer, matrix, x2, y2, 0, 1, 1, light);
        }
    }

    private void addVertex(VertexConsumer buffer, Matrix4f matrix,
                           float x, float y, float z, float u, float v, int light) {
        buffer.vertex(matrix, x, y, z)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
    }

    private void addGlowVertex(VertexConsumer buffer, Matrix4f matrix,
                               float x, float y, float z, float u, float v, int light) {
        buffer.vertex(matrix, x, y, z)
                .color(0.5f, 0.8f, 1.0f, 0.7f)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(PortalBlockEntity blockEntity) {
        return true;
    }
}