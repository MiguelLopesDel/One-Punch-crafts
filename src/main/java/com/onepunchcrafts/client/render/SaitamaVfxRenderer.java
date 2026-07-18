package com.onepunchcrafts.client.render;

import com.onepunchcrafts.network.packet.SaitamaVfxPacket;
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
import static com.onepunchcrafts.client.render.VfxQuadBatch.hash;
import static com.onepunchcrafts.client.render.VfxQuadBatch.stableSide;

/**
 * Anime-style quad VFX for Saitama: punch impact flashes with shock rings and
 * radial speed lines, ghost-jab barrages that track the caster's aim, the
 * air-splitting Serious Punch shockwave, dash streaks and high-speed movement
 * trails. Same lightweight approach as {@link BorosBeamVfxRenderer}: additive
 * quads on {@code RenderType.lightning()}, no post chain, shaderpack-safe.
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class SaitamaVfxRenderer {

    // Saitama palette: ivory-white shock, hero-suit yellow, glove red.
    private static final float[] WHITE = {1.0f, 0.98f, 0.94f};
    private static final float[] YELLOW = {1.0f, 0.83f, 0.35f};
    private static final float[] RED = {0.96f, 0.24f, 0.18f};

    private record VfxEffect(int casterId, Vec3 pos, Vec3 direction, float scale,
                             int style, int lifeTicks, long createdTick) {}

    private static final List<VfxEffect> EFFECTS = new CopyOnWriteArrayList<>();

    private SaitamaVfxRenderer() {}

    public static void addEffect(int casterId, Vec3 pos, Vec3 direction, float scale, int style, int lifeTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 dir = direction.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : direction.normalize();
        EFFECTS.add(new VfxEffect(casterId, pos, dir, scale, style, lifeTicks, minecraft.level.getGameTime()));
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

        for (VfxEffect effect : EFFECTS) {
            float age = gameTime - effect.createdTick + event.getPartialTick();
            if (age < 0 || age > effect.lifeTicks) continue;
            switch (effect.style) {
                case SaitamaVfxPacket.STYLE_BARRAGE -> renderBarrage(batch, minecraft, effect, age);
                case SaitamaVfxPacket.STYLE_DASH -> renderDash(batch, effect, age);
                case SaitamaVfxPacket.STYLE_SPEED_TRAIL -> renderSpeedTrail(batch, effect, age);
                default -> renderPunchImpact(batch, effect, age);
            }
        }

        batch.close();
    }

    /** Impact frame: white flash, expanding shock ring and radial speed lines. */
    private static void renderPunchImpact(VfxQuadBatch batch,
                                          VfxEffect effect, float age) {
        float progress = age / Math.max(1.0f, effect.lifeTicks);
        float fade = 1.0f - progress;
        float scale = effect.scale;

        Vec3 side = stableSide(effect.direction);
        Vec3 up = side.cross(effect.direction).normalize();

        batch.billboard(effect.pos, scale * (0.9f + progress * 1.6f),
                WHITE[0], WHITE[1], WHITE[2], 0.75f * fade);
        batch.billboard(effect.pos, scale * (1.7f + progress * 2.4f),
                YELLOW[0], YELLOW[1], YELLOW[2], 0.28f * fade);

        // Two shock rings racing outward, facing the punch direction.
        batch.ring(effect.pos, side, up, scale * (0.5f + progress * 2.8f), 18,
                WHITE[0], WHITE[1], WHITE[2], 0.55f * fade, age * 0.3f);
        batch.ring(effect.pos, side, up, scale * (0.3f + progress * 4.0f), 18,
                RED[0], RED[1], RED[2], 0.30f * fade, -age * 0.2f);

        // Radial impact-frame speed lines.
        int lines = 10;
        for (int i = 0; i < lines; i++) {
            double angle = Math.PI * 2.0 * i / lines + hash(i * 31) * 0.5;
            double reach = scale * (1.2 + hash(i * 57) * 1.6) * (0.5f + progress);
            Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 from = effect.pos.add(radial.scale(reach * 0.35));
            Vec3 to = effect.pos.add(radial.scale(reach));
            batch.strip(from, to, scale * 0.06f,
                    WHITE[0], WHITE[1], WHITE[2], 0.6f * fade);
        }
    }

    /** Consecutive punches: a storm of ghost jabs inside the caster's view cone. */
    private static void renderBarrage(VfxQuadBatch batch,
                                      Minecraft minecraft, VfxEffect effect, float age) {
        Vec3 origin = effect.pos;
        Vec3 direction = effect.direction;
        Entity caster = minecraft.level == null ? null : minecraft.level.getEntity(effect.casterId);
        if (caster != null) {
            float partialTick = minecraft.getPartialTick();
            double x = Mth.lerp(partialTick, caster.xo, caster.getX());
            double y = Mth.lerp(partialTick, caster.yo, caster.getY()) + caster.getBbHeight() * 0.72;
            double z = Mth.lerp(partialTick, caster.zo, caster.getZ());
            origin = new Vec3(x, y, z);
            direction = caster.getViewVector(partialTick).normalize();
        }

        float fade = 1.0f - Mth.clamp((age / Math.max(1.0f, effect.lifeTicks) - 0.8f) / 0.2f, 0.0f, 1.0f);
        float scale = effect.scale;
        Vec3 side = stableSide(direction);
        Vec3 up = side.cross(direction).normalize();

        // Ghost jabs re-rolled every couple of ticks so the fists flicker.
        int timeBucket = (int) (age * 0.55f);
        int jabs = Math.round(7 * scale);
        for (int k = 0; k < jabs; k++) {
            int seed = timeBucket * 131 + k * 17;
            double yaw = (hash(seed) - 0.5) * 1.1;
            double pitch = (hash(seed + 1) - 0.5) * 0.8;
            double distance = 1.1 + hash(seed + 2) * 2.4 * scale;
            Vec3 jabDir = direction.add(side.scale(yaw)).add(up.scale(pitch)).normalize();
            Vec3 tip = origin.add(jabDir.scale(distance));
            Vec3 tail = origin.add(jabDir.scale(Math.max(0.4, distance - 0.9 * scale)));

            float[] color = k % 3 == 0 ? YELLOW : WHITE;
            batch.strip(tail, tip, 0.07f * scale,
                    color[0], color[1], color[2], 0.7f * fade);
            batch.billboard(tip, 0.28f * scale * (0.7f + hash(seed + 3) * 0.6f),
                    color[0], color[1], color[2], 0.55f * fade);
        }

        // Pressure ring pulsing out of the cone every half second.
        float pulse = (age % 10.0f) / 10.0f;
        batch.ring(origin.add(direction.scale(1.2 + pulse * 2.2 * scale)), side, up,
                scale * (0.5f + pulse * 1.6f), 20,
                RED[0], RED[1], RED[2], 0.25f * (1.0f - pulse) * fade, age * 0.25f);
    }

    /** Dash: afterimage streak along the travel path plus a sonic boom at launch. */
    private static void renderDash(VfxQuadBatch batch,
                                   VfxEffect effect, float age) {
        float progress = age / Math.max(1.0f, effect.lifeTicks);
        float fade = 1.0f - progress;
        double length = Math.max(2.0, effect.scale);

        Vec3 side = stableSide(effect.direction);
        Vec3 up = side.cross(effect.direction).normalize();
        Vec3 end = effect.pos.add(effect.direction.scale(length));

        batch.strip(effect.pos, end, 0.45f * fade,
                WHITE[0], WHITE[1], WHITE[2], 0.35f * fade);
        batch.strip(effect.pos, end, 0.9f * fade,
                YELLOW[0], YELLOW[1], YELLOW[2], 0.15f * fade);

        // Fading afterimages spaced along the path.
        int ghosts = 6;
        for (int i = 0; i < ghosts; i++) {
            double t = (i + 1.0) / (ghosts + 1.0);
            float ghostFade = fade * (1.0f - (float) t * 0.7f);
            batch.billboard(effect.pos.add(effect.direction.scale(length * t)),
                    0.9f * ghostFade + 0.3f, WHITE[0], WHITE[1], WHITE[2], 0.30f * ghostFade);
        }

        // Sonic boom rings left at the launch point.
        batch.ring(effect.pos, side, up, 0.6f + progress * 3.2f, 20,
                WHITE[0], WHITE[1], WHITE[2], 0.5f * fade, age * 0.4f);
        batch.ring(effect.pos, side, up, 0.4f + progress * 4.6f, 20,
                RED[0], RED[1], RED[2], 0.25f * fade, -age * 0.3f);
    }

    /** High-speed run: short streaks whipped out behind the runner. */
    private static void renderSpeedTrail(VfxQuadBatch batch,
                                         VfxEffect effect, float age) {
        float progress = age / Math.max(1.0f, effect.lifeTicks);
        float fade = 1.0f - progress;
        float speed = Math.min(effect.scale, 3.0f);

        Vec3 back = effect.direction.scale(-1);
        Vec3 side = stableSide(effect.direction);
        Vec3 up = side.cross(effect.direction).normalize();

        int streaks = 4;
        for (int i = 0; i < streaks; i++) {
            int seed = (int) (effect.createdTick % 1000) * 13 + i * 29;
            Vec3 offset = side.scale((hash(seed) - 0.5) * 0.9).add(up.scale((hash(seed + 1) - 0.5) * 1.2));
            Vec3 from = effect.pos.add(offset);
            Vec3 to = from.add(back.scale(1.2 + speed * (0.8 + hash(seed + 2) * 0.8)));
            float[] color = i % 2 == 0 ? WHITE : YELLOW;
            batch.strip(from, to, 0.06f,
                    color[0], color[1], color[2], 0.45f * fade);
        }

        batch.billboard(effect.pos, 0.5f + speed * 0.15f,
                WHITE[0], WHITE[1], WHITE[2], 0.18f * fade);
    }






}
