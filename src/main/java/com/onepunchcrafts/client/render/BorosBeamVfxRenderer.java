package com.onepunchcrafts.client.render;

import com.onepunchcrafts.network.packet.BorosBeamVfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.onepunchcrafts.OnePunchCrafts.MODID;
import static com.onepunchcrafts.client.render.VfxQuadBatch.stableSide;

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

        VfxQuadBatch batch = VfxQuadBatch.begin(event);
        long gameTime = minecraft.level.getGameTime();

        for (BeamEffect effect : EFFECTS) {
            float age = gameTime - effect.createdTick + event.getPartialTick();
            if (age < 0 || age > effect.lifeTicks) continue;
            renderBeam(batch, minecraft, effect, age);
        }

        batch.close();
    }

    private static void renderBeam(VfxQuadBatch batch, Minecraft minecraft, BeamEffect effect, float age) {
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

        batch.strip(origin, visibleEnd, width * 0.45f,
                style.core[0], style.core[1], style.core[2], 0.85f * fade);
        batch.strip(origin, visibleEnd, width * 0.95f,
                style.mid[0], style.mid[1], style.mid[2], 0.42f * fade);
        batch.strip(origin, visibleEnd, width * 1.65f,
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
                    batch.strip(previous, point, width * 0.14f,
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
            batch.ring(center, side, up, width * (1.4f + ringT * 1.3f), 20,
                    style.mid[0], style.mid[1], style.mid[2], 0.34f * fade * (1.0f - ringT * 0.6f), age * 0.2f + i);
        }

        // Muzzle flare while the shot leaves the hand.
        float muzzleFade = 1.0f - Mth.clamp(progress / 0.45f, 0.0f, 1.0f);
        if (muzzleFade > 0.0f) {
            batch.billboard(origin, width * (2.0f + progress * 2.0f),
                    style.core[0], style.core[1], style.core[2], 0.6f * muzzleFade);
            batch.billboard(origin, width * (3.4f + progress * 2.4f),
                    style.glow[0], style.glow[1], style.glow[2], 0.3f * muzzleFade);
        }

        // Impact bloom once the front lands.
        if (travel >= 0.999f) {
            float impactAge = age - effect.lifeTicks / 2.4f;
            float impactFade = fade * (0.75f + 0.25f * Mth.sin(age * 1.7f));
            batch.billboard(end, width * (2.6f + impactAge * 0.4f),
                    style.core[0], style.core[1], style.core[2], 0.55f * impactFade);
            batch.ring(end, side, up, width * (3.0f + impactAge * 0.8f), 28,
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





}
