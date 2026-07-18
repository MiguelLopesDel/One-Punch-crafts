package com.onepunchcrafts.client.render;

import com.onepunchcrafts.client.event.ScreenEffectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

import static com.onepunchcrafts.OnePunchCrafts.MODID;
import static com.onepunchcrafts.client.render.VfxQuadBatch.hash;
import static com.onepunchcrafts.client.render.VfxQuadBatch.stableSide;

/**
 * Client-side Serious Punch cinematic. One packet starts a deterministic
 * timeline inspired by the anime cut:
 *
 * <pre>
 *   [0 .. W)        windup   — the world holds its breath: desaturation and
 *                              space compression (post shader), frozen motes,
 *                              FOV squeeze, rings contracting into the fist.
 *   [W .. W+2)      line     — a razor-thin white line crosses the sky.
 *   [W+2 .. W+16)   cone     — the line bursts into a pressure cone tearing
 *                              the air apart in two directions.
 *   [W .. W+8)      impact   — few-frame white flash, radial speed lines,
 *                              circular shockwave, camera kick.
 *   [W+16 .. END)   aftermath— near-silence, wind streaks chasing the punch,
 *                              slow-falling fragments, the cloud tear lingers.
 * </pre>
 *
 * World geometry uses the same additive-quad approach as the other VFX
 * renderers; the screen-space half (desaturation, lens distortion, flash)
 * lives in {@link SeriousPostChainHandler}.
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class SeriousPunchCinematic {

    public static final int LINE_TICKS = 2;
    public static final int CONE_TICKS = 14;
    public static final int TAIL_TICKS = 80;

    private static final float[] WHITE = {1.0f, 0.99f, 0.96f};
    private static final float[] YELLOW = {1.0f, 0.83f, 0.35f};
    private static final float[] RED = {0.96f, 0.24f, 0.18f};

    private static final double LINE_RANGE = 260.0;
    private static final Random RANDOM = new Random();

    private static boolean active;
    private static int casterId;
    private static Vec3 origin = Vec3.ZERO;
    private static Vec3 direction = new Vec3(0, 0, 1);
    private static int windupTicks = 14;
    private static long startTick;
    private static boolean impactCued;

    private SeriousPunchCinematic() {}

    public static void start(int caster, Vec3 pos, Vec3 dir, int windup) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        casterId = caster;
        origin = pos;
        direction = dir.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        windupTicks = Math.max(1, windup);
        startTick = minecraft.level.getGameTime();
        impactCued = false;
        active = true;

        minecraft.level.playLocalSound(origin.x, origin.y, origin.z,
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 0.55f, false);
    }

    /** Timeline state consumed by {@link SeriousPostChainHandler}. */
    public record State(int casterId, Vec3 origin, Vec3 direction, float age, int windupTicks) {}

    public static State state(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.level == null) return null;
        float age = minecraft.level.getGameTime() - startTick + partialTick;
        if (age < 0 || age > windupTicks + TAIL_TICKS) return null;
        return new State(casterId, origin, direction, age, windupTicks);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            active = false;
            return;
        }

        long age = minecraft.level.getGameTime() - startTick;
        if (age > windupTicks + TAIL_TICKS) {
            active = false;
            return;
        }

        double camDist = minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(origin);

        // Windup: the air itself stalls — sparse motes hanging still.
        if (age < windupTicks && camDist < 64.0) {
            for (int i = 0; i < 3; i++) {
                Vec3 offset = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.6, RANDOM.nextGaussian()).scale(2.8);
                Vec3 pos = origin.add(offset);
                minecraft.level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 0.0, -0.002, 0.0);
            }
        }

        // Impact beat: whip-crack, then the boom, then the camera kick.
        if (age >= windupTicks && !impactCued) {
            impactCued = true;
            minecraft.level.playLocalSound(origin.x, origin.y, origin.z,
                    SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.6f, 1.85f, false);
            float shake = (float) Math.min(7.0, 7.0 * 60.0 / (camDist + 20.0));
            ScreenEffectHandler.addEffect(shake, 9, 1.0f);
        }
        if (age == windupTicks + 2L) {
            minecraft.level.playLocalSound(origin.x, origin.y, origin.z,
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 0.72f, false);
            minecraft.level.playLocalSound(origin.x, origin.y, origin.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 0.45f, false);
        }

        // Aftermath: only the wind is left, dragging dust down the punch line.
        if (age == windupTicks + 16L) {
            minecraft.level.playLocalSound(origin.x, origin.y, origin.z,
                    SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.30f, 0.7f, false);
        }
        if (age > windupTicks + 14 && age % 2 == 0 && camDist < 96.0) {
            for (int i = 0; i < 2; i++) {
                double along = RANDOM.nextDouble() * 50.0;
                Vec3 offset = stableSide(direction).scale(RANDOM.nextGaussian() * 4.0)
                        .add(0, RANDOM.nextGaussian() * 2.5, 0);
                Vec3 pos = origin.add(direction.scale(along)).add(offset);
                minecraft.level.addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                        direction.x * 0.22, direction.y * 0.22 - 0.01, direction.z * 0.22);
                minecraft.level.addParticle(ParticleTypes.WHITE_ASH, pos.x, pos.y + 1.5, pos.z, 0.0, -0.05, 0.0);
            }
        }
    }

    /** Windup squeeze and impact kick — subtle, but it sells the pressure. */
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        State state = state((float) event.getPartialTick());
        if (state == null) return;

        double fov = event.getFOV();
        if (state.age() < state.windupTicks()) {
            float p = state.age() / state.windupTicks();
            fov *= 1.0 - 0.10 * p * p * p;
        } else {
            float sinceImpact = state.age() - state.windupTicks();
            fov *= 1.0 + 0.08 * Math.exp(-sinceImpact * 0.55);
        }
        event.setFOV(fov);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        State state = state(event.getPartialTick());
        if (state == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        VfxQuadBatch batch = VfxQuadBatch.begin(event);

        float age = state.age();
        int windup = state.windupTicks();
        Vec3 side = stableSide(direction);
        Vec3 up = side.cross(direction).normalize();

        if (age < windup) {
            renderWindup(batch, side, up, age / windup, age);
        } else {
            float sinceImpact = age - windup;
            renderLineAndCone(batch, side, up, sinceImpact);
            renderImpact(batch, side, up, sinceImpact);
            renderSkyTear(batch, minecraft, sinceImpact);
            renderAftermath(batch, side, up, sinceImpact);
        }

        batch.close();
    }

    /** Space folding inward: rings collapsing into the fist, a growing point of light. */
    private static void renderWindup(VfxQuadBatch batch,
                                     Vec3 side, Vec3 up, float progress, float age) {
        Vec3 focus = origin.add(direction.scale(1.1));
        for (int i = 0; i < 3; i++) {
            float phase = (progress * 1.6f + i / 3.0f) % 1.0f;
            float radius = Mth.lerp(phase, 3.2f, 0.25f);
            float alpha = 0.22f * phase * (0.4f + 0.6f * progress);
            batch.ring(focus, side, up, radius, 22,
                    WHITE[0], WHITE[1], WHITE[2], alpha, age * 0.15f + i);
        }
        batch.billboard(focus, 0.12f + 0.3f * progress * progress,
                WHITE[0], WHITE[1], WHITE[2], 0.5f * progress);
    }

    /** The razor line, then the pressure cone bursting out of it. */
    private static void renderLineAndCone(VfxQuadBatch batch,
                                          Vec3 side, Vec3 up, float sinceImpact) {
        Vec3 end = origin.add(direction.scale(LINE_RANGE));

        // Razor-thin white line, only a couple of frames.
        if (sinceImpact <= LINE_TICKS + 2.0f) {
            float lineFade = 1.0f - Mth.clamp((sinceImpact - LINE_TICKS) / 2.0f, 0.0f, 1.0f);
            batch.strip(origin, end, 0.05f,
                    WHITE[0], WHITE[1], WHITE[2], 0.95f * lineFade);
            batch.strip(origin, end, 0.22f,
                    WHITE[0], WHITE[1], WHITE[2], 0.35f * lineFade);
        }

        // Pressure cone: rings swelling along the axis behind a sweeping front.
        float coneP = Mth.clamp((sinceImpact - LINE_TICKS) / CONE_TICKS, 0.0f, 1.0f);
        if (coneP > 0.0f) {
            float coneFade = 1.0f - Mth.clamp((sinceImpact - LINE_TICKS - CONE_TICKS) / 24.0f, 0.0f, 1.0f);
            float front = (float) LINE_RANGE * Mth.clamp(coneP * 1.35f, 0.0f, 1.0f);
            for (int i = 0; i < 9; i++) {
                float along = 6.0f + i * (front - 6.0f) / 8.0f;
                if (along > front) continue;
                float spread = along * 0.16f * (0.35f + 0.65f * coneP);
                float alpha = (0.4f - i * 0.035f) * coneFade;
                if (alpha <= 0.01f) continue;
                float[] color = i % 3 == 2 ? YELLOW : WHITE;
                batch.ring(origin.add(direction.scale(along)), side, up,
                        spread, 26, color[0], color[1], color[2], alpha, sinceImpact * 0.1f + i);
            }

            // The air torn open in two directions: wedges peeling up and down.
            float wedgeReach = 24.0f * coneP;
            for (int sign = -1; sign <= 1; sign += 2) {
                Vec3 prev = origin.add(direction.scale(5));
                int steps = 7;
                for (int i = 1; i <= steps; i++) {
                    double along = 5.0 + (110.0 * coneP) * i / steps;
                    double lift = wedgeReach * Math.pow((double) i / steps, 1.6) * sign;
                    Vec3 point = origin.add(direction.scale(along)).add(up.scale(lift));
                    batch.strip(prev, point, 0.5f + i * 0.22f,
                            WHITE[0], WHITE[1], WHITE[2], 0.10f * coneFade * (1.0f - (float) i / steps * 0.5f));
                    prev = point;
                }
            }
        }
    }

    /** Flash, radial speed lines and the circular shockwave at the origin. */
    private static void renderImpact(VfxQuadBatch batch,
                                     Vec3 side, Vec3 up, float sinceImpact) {
        // White flash: just a few frames.
        float flash = (float) Math.exp(-sinceImpact * 1.4);
        if (flash > 0.03f) {
            batch.billboard(origin, 5.0f + sinceImpact * 3.0f,
                    WHITE[0], WHITE[1], WHITE[2], 0.9f * flash);
        }

        float impactFade = 1.0f - Mth.clamp(sinceImpact / 10.0f, 0.0f, 1.0f);
        if (impactFade > 0.0f) {
            // Hard speed lines bursting from the fist.
            int lines = 14;
            for (int i = 0; i < lines; i++) {
                double angle = Math.PI * 2.0 * i / lines + hash(i * 37) * 0.4;
                double reach = 5.0 + hash(i * 61) * 9.0;
                Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                Vec3 from = origin.add(radial.scale(0.6)).add(direction.scale(0.4));
                Vec3 to = origin.add(radial.scale(reach * (0.4 + 0.6 * Math.min(sinceImpact / 3.0, 1.0))));
                batch.strip(from, to, 0.08f,
                        WHITE[0], WHITE[1], WHITE[2], 0.75f * impactFade);
            }

            // Circular shockwave hugging the ground at the origin.
            Vec3 groundCenter = origin.add(0, -1.2, 0);
            float waveR = 1.0f + sinceImpact * 2.6f;
            batch.ring(groundCenter, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                    waveR, 30, WHITE[0], WHITE[1], WHITE[2], 0.45f * impactFade, sinceImpact * 0.2f);
            batch.ring(groundCenter, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                    waveR * 1.5f, 30, RED[0], RED[1], RED[2], 0.22f * impactFade, -sinceImpact * 0.15f);

            // Vertical blast ring facing the punch direction.
            batch.ring(origin.add(direction.scale(1.5)), side, up,
                    0.8f + sinceImpact * 2.0f, 24,
                    YELLOW[0], YELLOW[1], YELLOW[2], 0.35f * impactFade, sinceImpact * 0.3f);
        }
    }

    /** The giant opening ripped into the clouds above the punch line. */
    private static void renderSkyTear(VfxQuadBatch batch,
                                      Minecraft minecraft, float sinceImpact) {
        if (sinceImpact < 4.0f || minecraft.level == null) return;
        float cloudHeight = minecraft.level.effects().getCloudHeight();
        if (Float.isNaN(cloudHeight) || cloudHeight <= origin.y) return;

        float p = Mth.clamp((sinceImpact - 4.0f) / (TAIL_TICKS - 20.0f), 0.0f, 1.0f);
        float fade = 0.25f * (1.0f - p * p);
        if (fade <= 0.01f) return;

        Vec3 flat = new Vec3(direction.x, 0, direction.z);
        flat = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 center = new Vec3(origin.x, cloudHeight + 2.0, origin.z).add(flat.scale(40.0));
        float radius = 15.0f + 130.0f * (float) Math.pow(p, 0.55);

        batch.ring(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                radius, 40, WHITE[0], WHITE[1], WHITE[2], fade, sinceImpact * 0.02f);
        batch.ring(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                radius * 0.72f, 36, WHITE[0], WHITE[1], WHITE[2], fade * 0.6f, -sinceImpact * 0.015f);
    }

    /** Wind streaks chasing the punch and fragments drifting down. */
    private static void renderAftermath(VfxQuadBatch batch,
                                        Vec3 side, Vec3 up, float sinceImpact) {
        if (sinceImpact < CONE_TICKS) return;
        float windFade = 1.0f - Mth.clamp((sinceImpact - CONE_TICKS) / (TAIL_TICKS - CONE_TICKS), 0.0f, 1.0f);
        if (windFade <= 0.02f) return;

        int streaks = 12;
        for (int i = 0; i < streaks; i++) {
            float cycle = (hash(i * 53) + sinceImpact * (0.035f + hash(i * 71) * 0.02f)) % 1.0f;
            double along = 4.0 + cycle * 90.0;
            Vec3 offset = side.scale((hash(i * 13) - 0.5) * 9.0).add(up.scale((hash(i * 17) - 0.5) * 6.0));
            Vec3 from = origin.add(direction.scale(along)).add(offset);
            Vec3 to = from.add(direction.scale(2.5 + hash(i * 23) * 2.5));
            batch.strip(from, to, 0.05f,
                    WHITE[0], WHITE[1], WHITE[2], 0.16f * windFade * (1.0f - cycle * 0.5f));
        }

        // Slow-falling glints near the origin.
        for (int i = 0; i < 10; i++) {
            Vec3 base = origin.add(side.scale((hash(i * 41) - 0.5) * 10.0))
                    .add(direction.scale(hash(i * 43) * 14.0));
            double fall = (sinceImpact - CONE_TICKS) * (0.03 + hash(i * 47) * 0.05);
            Vec3 pos = base.add(0, 2.5 + hash(i * 59) * 4.0 - fall, 0);
            batch.billboard(pos, 0.10f + hash(i * 67) * 0.08f,
                    YELLOW[0], YELLOW[1], YELLOW[2], 0.25f * windFade);
        }
    }






}
