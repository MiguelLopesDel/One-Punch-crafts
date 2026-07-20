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

/** Alternate Serious Punch: silence and physical pressure, with almost no colored energy. */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class NewSeriousPunchCinematic {
    private static final float[] WHITE = {1.0f, 1.0f, 0.98f};
    private static final float[] GRAY = {0.52f, 0.55f, 0.58f};
    private static final float[] DUST = {0.76f, 0.72f, 0.66f};
    private static final int TAIL = 90;
    private static final Random RANDOM = new Random();

    /** One in-flight cinematic; several can play at once for concurrent punches. */
    private static final class Cinematic {
        final int id;
        final Vec3 origin;
        final Vec3 direction;
        final int windup;
        final long started;
        boolean impactPlayed;
        Cinematic(int id, Vec3 origin, Vec3 direction, int windup, long started) {
            this.id = id;
            this.origin = origin;
            this.direction = direction;
            this.windup = windup;
            this.started = started;
        }
    }

    private static final Map<Integer, Cinematic> CINEMATICS = new HashMap<>();

    // Scratch context loaded from the cinematic currently ticked/rendered.
    private static int casterId;
    private static Vec3 origin = Vec3.ZERO;
    private static Vec3 direction = new Vec3(0, 0, 1);
    private static int windup;
    private static long started;

    private NewSeriousPunchCinematic() {}

    private static void load(Cinematic cinematic) {
        casterId = cinematic.id;
        origin = cinematic.origin;
        direction = cinematic.direction;
        windup = cinematic.windup;
        started = cinematic.started;
    }

    public static void start(int id, Vec3 pos, Vec3 dir, int windupTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 dirN = dir.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        CINEMATICS.put(id, new Cinematic(id, pos, dirN, Math.max(1, windupTicks), minecraft.level.getGameTime()));
        minecraft.level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS, 0.32f, 0.45f, false);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            CINEMATICS.clear();
            return;
        }
        if (CINEMATICS.isEmpty()) return;

        long now = minecraft.level.getGameTime();
        Iterator<Map.Entry<Integer, Cinematic>> iterator = CINEMATICS.entrySet().iterator();
        while (iterator.hasNext()) {
            Cinematic cinematic = iterator.next().getValue();
            long age = now - cinematic.started;
            if (age > cinematic.windup + TAIL) {
                iterator.remove();
                continue;
            }
            load(cinematic);

            // The world is drawn toward the fist during the hush, not emitted by it.
            if (age < windup) {
                Vec3 side = stableSide(direction);
                Vec3 up = side.cross(direction).normalize();
                for (int i = 0; i < 5; i++) {
                    Vec3 radial = side.scale(RANDOM.nextGaussian() * 2.8)
                            .add(up.scale(RANDOM.nextGaussian() * 1.7))
                            .subtract(direction.scale(0.8 + RANDOM.nextDouble() * 4.0));
                    Vec3 point = origin.add(radial);
                    Vec3 velocity = origin.subtract(point).normalize().scale(0.07 + age * 0.004);
                    minecraft.level.addParticle(i % 2 == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.POOF,
                            point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
                }
            }

            if (age >= windup && !cinematic.impactPlayed) {
                cinematic.impactPlayed = true;
                double distance = minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(origin);
                float shake = (float) Math.min(6.0, 6.0 * 60.0 / (distance + 18.0));
                ScreenEffectHandler.addEffect(shake, 7, 0.96f);
                minecraft.level.playLocalSound(origin.x, origin.y, origin.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 0.62f, false);
            }
            if (age > windup + 16 && age % 3 == 0) {
                Vec3 side = stableSide(direction);
                double along = 4 + RANDOM.nextDouble() * 70;
                Vec3 point = origin.add(direction.scale(along))
                        .add(side.scale(RANDOM.nextGaussian() * (1 + along * 0.08)));
                minecraft.level.addParticle(ParticleTypes.CLOUD, point.x, point.y, point.z,
                        direction.x * 0.18, direction.y * 0.18, direction.z * 0.18);
            }
        }
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || CINEMATICS.isEmpty()) return;
        Cinematic cinematic = freshest(minecraft.level.getGameTime(), (float) event.getPartialTick());
        if (cinematic == null) return;
        float age = minecraft.level.getGameTime() - cinematic.started + (float) event.getPartialTick();
        if (age < cinematic.windup) {
            float p = age / cinematic.windup;
            event.setFOV(event.getFOV() * (1.0 - p * p * 0.075));
        } else {
            event.setFOV(event.getFOV() * (1.0 + 0.055 * Math.exp(-(age - cinematic.windup) * 0.5)));
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || CINEMATICS.isEmpty()) return;

        long now = minecraft.level.getGameTime();
        try (VfxQuadBatch batch = VfxQuadBatch.begin(event)) {
            for (Cinematic cinematic : CINEMATICS.values()) {
                float age = now - cinematic.started + event.getPartialTick();
                if (age < 0 || age > cinematic.windup + TAIL) continue;
                load(cinematic);
                Vec3 side = stableSide(direction);
                Vec3 up = side.cross(direction).normalize();
                if (age < windup) renderHush(batch, side, up, age / windup, age);
                else renderRelease(batch, side, up, age - windup);
            }
        }
    }

    /** Freshest live cinematic — the screen/FOV follow the newest cast. */
    private static Cinematic freshest(long now, float partialTick) {
        Cinematic best = null;
        float bestAge = Float.MAX_VALUE;
        for (Cinematic cinematic : CINEMATICS.values()) {
            float age = now - cinematic.started + partialTick;
            if (age < 0 || age > cinematic.windup + TAIL) continue;
            if (age < bestAge) {
                bestAge = age;
                best = cinematic;
            }
        }
        return best;
    }

    private static void renderHush(VfxQuadBatch batch, Vec3 side, Vec3 up, float p, float age) {
        Vec3 fist = origin.add(direction.scale(0.35));
        // Thin fragments and pressure arcs collapse inward; no magic charge sphere.
        for (int i = 0; i < 14; i++) {
            int seed = casterId * 97 + i * 41;
            double angle = hash(seed) * Math.PI * 2.0;
            float radius = (3.6f + hash(seed + 1) * 3.0f) * (1.0f - p * 0.9f);
            Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 from = fist.add(radial.scale(radius)).subtract(direction.scale(hash(seed + 2) * 2.5));
            Vec3 to = from.add(fist.subtract(from).normalize().scale(0.35 + p * 1.1));
            batch.strip(from, to, 0.025f, DUST[0], DUST[1], DUST[2], 0.18f + p * 0.26f);
        }
        batch.ring(fist, side, up, 2.4f * (1 - p) + 0.15f, 24,
                GRAY[0], GRAY[1], GRAY[2], 0.18f * p, -age * 0.08f);
    }

    private static void renderRelease(VfxQuadBatch batch, Vec3 side, Vec3 up, float age) {
        // The signature frame: one razor line, followed by the world opening around it.
        if (age < 4) {
            float fade = 1 - age / 4;
            Vec3 end = origin.add(direction.scale(320));
            batch.strip(origin, end, 0.035f, WHITE[0], WHITE[1], WHITE[2], 0.98f * fade);
            batch.strip(origin, end, 0.18f, GRAY[0], GRAY[1], GRAY[2], 0.30f * fade);
            batch.billboard(origin, 3.0f + age * 2.0f,
                    WHITE[0], WHITE[1], WHITE[2], 0.70f * fade);
        }

        float split = Mth.clamp((age - 1.0f) / 15.0f, 0, 1);
        float splitFade = 1 - Mth.clamp((age - 25.0f) / 55.0f, 0, 1);
        if (split > 0) {
            for (int sign : new int[]{-1, 1}) {
                Vec3 previous = origin.add(direction.scale(2));
                for (int i = 1; i <= 11; i++) {
                    double along = i * 18.0 * split;
                    double opening = sign * Math.pow(i / 11.0, 1.45) * 34.0 * split;
                    Vec3 point = origin.add(direction.scale(along)).add(up.scale(opening));
                    batch.strip(previous, point, 0.12f + i * 0.05f,
                            WHITE[0], WHITE[1], WHITE[2], 0.34f * splitFade);
                    previous = point;
                }
            }
            for (int i = 0; i < 9; i++) {
                float along = 5 + i * 23 * split;
                float radius = 0.6f + along * 0.17f * split;
                batch.ring(origin.add(direction.scale(along)), side, up, radius, 26,
                        i % 2 == 0 ? WHITE[0] : GRAY[0],
                        i % 2 == 0 ? WHITE[1] : GRAY[1],
                        i % 2 == 0 ? WHITE[2] : GRAY[2],
                        (0.32f - i * 0.025f) * splitFade, 0);
            }
        }
    }
}
