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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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

    /**
     * Live destruction front, fed by {@code STYLE_SERIOUS_FRONT} updates from
     * the server. The punch keeps carving the world for many seconds after the
     * cinematic timeline ends, so fronts live independently of {@link #active}
     * and follow the real destruction wherever it currently is.
     */
    private static final class Front {
        Vec3 prevPos, lastPos;
        long prevTime, lastTime;
        Vec3 direction = new Vec3(0, 0, 1);
        float radius = 15.0f;
        long lastSeen;
        boolean ending;
        boolean quiet;
        long endStart;
        Vec3 endPos;
    }

    private static final Map<Integer, Front> FRONTS = new HashMap<>();

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

    /** Server told us where the destruction front actually is right now. */
    public static void updateFront(int caster, Vec3 pos, Vec3 dir, float radius) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        long now = minecraft.level.getGameTime();
        Front front = FRONTS.computeIfAbsent(caster, id -> new Front());
        if (front.ending) {
            // A new punch started while the previous burst was still fading.
            front = new Front();
            FRONTS.put(caster, front);
        }
        if (front.lastPos == null) {
            front.prevPos = pos;
            front.prevTime = now;
            front.lastPos = pos;
            front.lastTime = now;
        } else if (now > front.lastTime) {
            front.prevPos = front.lastPos;
            front.prevTime = front.lastTime;
            front.lastPos = pos;
            front.lastTime = now;
        } else {
            front.lastPos = pos;
        }
        if (dir.lengthSqr() > 1.0e-4) front.direction = dir.normalize();
        if (radius > 0.5f) front.radius = radius;
        front.lastSeen = now;
    }

    /** The destruction reached its end — burst once, then let the front die. */
    public static void endFront(int caster, Vec3 pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Front front = FRONTS.computeIfAbsent(caster, id -> new Front());
        front.ending = true;
        front.quiet = false;
        front.endPos = pos;
        front.endStart = minecraft.level.getGameTime();
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
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            active = false;
            FRONTS.clear();
            return;
        }
        tickFronts(minecraft);
        if (!active) return;

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

    /** Timeouts, ambient dust around the front and a rumble when it passes close. */
    private static void tickFronts(Minecraft minecraft) {
        if (FRONTS.isEmpty()) return;
        long now = minecraft.level.getGameTime();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();

        Iterator<Front> iterator = FRONTS.values().iterator();
        while (iterator.hasNext()) {
            Front front = iterator.next();
            if (front.ending) {
                if (now - front.endStart > 40) iterator.remove();
                continue;
            }
            if (now - front.lastSeen > 60) {
                // Updates stopped without an end burst (unloaded chunks, caster
                // logged off): fade out quietly where we last saw it.
                front.ending = true;
                front.quiet = true;
                front.endPos = frontPositionAt(front, now);
                front.endStart = now;
                continue;
            }

            Vec3 pos = frontPositionAt(front, now);
            double camDist = camera.distanceTo(pos);
            if (camDist < 140.0) {
                Vec3 side = stableSide(front.direction);
                Vec3 up = side.cross(front.direction).normalize();
                for (int i = 0; i < 3; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                    Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                    Vec3 p = pos.add(radial.scale(front.radius * (0.8 + RANDOM.nextDouble() * 0.4)));
                    Vec3 v = front.direction.scale(0.4).add(radial.scale(0.2));
                    minecraft.level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, v.x, v.y, v.z);
                }
            }
            if (camDist < 56.0 && now % 5 == 0) {
                ScreenEffectHandler.addEffect((float) Math.min(2.5, 2.5 * 24.0 / (camDist + 10.0)), 6, 0.7f);
            }
        }
    }

    /**
     * Where the front is right now: the last server update extrapolated at the
     * observed speed, clamped so packet loss never shoots it past the truth.
     */
    private static Vec3 frontPositionAt(Front front, float now) {
        if (front.lastPos == null) return front.endPos != null ? front.endPos : Vec3.ZERO;
        Vec3 velocity = front.lastTime > front.prevTime
                ? front.lastPos.subtract(front.prevPos).scale(1.0 / (front.lastTime - front.prevTime))
                : front.direction.scale(1.2);
        float ahead = Mth.clamp(now - front.lastTime, 0.0f, 6.0f);
        return front.lastPos.add(velocity.scale(ahead));
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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        State state = state(event.getPartialTick());
        if (state == null && FRONTS.isEmpty()) return;

        VfxQuadBatch batch = VfxQuadBatch.begin(event);

        if (state != null) {
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
        }

        if (!FRONTS.isEmpty()) {
            float now = minecraft.level.getGameTime() + event.getPartialTick();
            for (Front front : FRONTS.values()) {
                if (front.ending) renderFrontEnd(batch, front, now);
                else renderFront(batch, front, now);
            }
        }

        batch.close();
    }

    /**
     * The travelling shockwave riding the destruction front: a bright shock
     * disc at the tip, compressed air ahead of it, the hot tunnel rim cooling
     * off behind, and speed streaks hugging the edge.
     */
    private static void renderFront(VfxQuadBatch batch, Front front, float now) {
        Vec3 pos = frontPositionAt(front, now);
        Vec3 dir = front.direction;
        Vec3 side = stableSide(dir);
        Vec3 up = side.cross(dir).normalize();
        float r = front.radius;
        float spin = now * 0.12f;

        // Leading shock disc.
        batch.billboard(pos, r * 0.40f + r * 0.05f * (float) Math.sin(now * 0.9),
                WHITE[0], WHITE[1], WHITE[2], 0.40f);
        batch.ring(pos.add(dir.scale(0.8)), side, up, r * 1.02f, 0.9f, 30,
                WHITE[0], WHITE[1], WHITE[2], 0.50f, spin);
        batch.ring(pos.add(dir.scale(0.3)), side, up, r * 1.18f, 0.55f, 30,
                YELLOW[0], YELLOW[1], YELLOW[2], 0.28f, -spin * 0.7f);
        batch.ring(pos, side, up, r * 1.34f, 0.35f, 28,
                RED[0], RED[1], RED[2], 0.14f, spin * 0.5f);

        // Air being squeezed just ahead of the disc.
        batch.ring(pos.add(dir.scale(5)), side, up, r * 0.78f, 24,
                WHITE[0], WHITE[1], WHITE[2], 0.14f, -spin);
        batch.ring(pos.add(dir.scale(11)), side, up, r * 0.50f, 20,
                WHITE[0], WHITE[1], WHITE[2], 0.07f, spin * 1.3f);

        // The tunnel rim still glowing behind the front, cooling with distance.
        float[] offsets = {6, 14, 24, 38, 56, 78};
        for (int i = 0; i < offsets.length; i++) {
            float alpha = 0.26f * (float) Math.exp(-offsets[i] / 34.0);
            float[] color = i < 2 ? YELLOW : WHITE;
            batch.ring(pos.subtract(dir.scale(offsets[i])), side, up,
                    r * (1.02f + offsets[i] * 0.003f), 26,
                    color[0], color[1], color[2], alpha, spin * (i % 2 == 0 ? 0.4f : -0.3f) + i);
        }

        // Speed streaks hugging the rim, constantly overtaken by the front.
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0 * i / 12 + hash(i * 31) * 0.5;
            Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            float cycle = (hash(i * 47) + now * 0.06f) % 1.0f;
            Vec3 from = pos.add(radial.scale(r * (0.88 + 0.18 * hash(i * 53))))
                    .subtract(dir.scale(2.0 + cycle * 6.0));
            Vec3 to = from.add(dir.scale(5.0 + hash(i * 59) * 4.0));
            batch.strip(from, to, 0.14f,
                    WHITE[0], WHITE[1], WHITE[2], 0.40f * (1.0f - cycle * 0.6f));
        }
    }

    /** Terminal bloom where the punch spent itself; quiet fade on timeout. */
    private static void renderFrontEnd(VfxQuadBatch batch, Front front, float now) {
        Vec3 pos = front.endPos != null ? front.endPos : frontPositionAt(front, now);
        float age = Math.max(0.0f, now - front.endStart);
        float fade = 1.0f - Mth.clamp(age / 30.0f, 0.0f, 1.0f);
        if (fade <= 0.01f) return;
        Vec3 dir = front.direction;
        Vec3 side = stableSide(dir);
        Vec3 up = side.cross(dir).normalize();
        float r = front.radius;

        if (front.quiet) {
            batch.ring(pos, side, up, r * 1.05f, 28,
                    WHITE[0], WHITE[1], WHITE[2], 0.25f * fade, age * 0.1f);
            return;
        }
        float flash = (float) Math.exp(-age * 0.45);
        batch.billboard(pos, r * (0.8f + age * 0.25f),
                WHITE[0], WHITE[1], WHITE[2], 0.85f * flash);
        batch.ring(pos, side, up, r * (1.0f + age * 0.9f), 32,
                WHITE[0], WHITE[1], WHITE[2], 0.50f * fade, age * 0.2f);
        batch.ring(pos, side, up, r * (1.3f + age * 1.4f), 32,
                YELLOW[0], YELLOW[1], YELLOW[2], 0.25f * fade, -age * 0.15f);
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
