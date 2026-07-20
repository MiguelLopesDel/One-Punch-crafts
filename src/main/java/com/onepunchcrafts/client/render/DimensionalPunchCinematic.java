package com.onepunchcrafts.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.onepunchcrafts.OnePunchCrafts.MODID;
import static com.onepunchcrafts.client.render.VfxQuadBatch.hash;
import static com.onepunchcrafts.client.render.VfxQuadBatch.stableSide;

/**
 * Client-side Dimensional Punch cinematic. The fist gathers, space fractures
 * along jagged cracks, then a rift tears open at the impact point and settles
 * into the portal. World-space geometry lives here; the fullscreen refraction
 * of the shattered dimension is in {@link DimensionalPostChainHandler}.
 *
 * <p>Keyed by a per-cast instance id so concurrent punches never share state
 * (the context-swap pattern: {@link #load} copies one cast into scratch fields
 * before the shared render/tick code runs).
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class DimensionalPunchCinematic {

    public static final int OPEN_TICKS = 18;
    public static final int TAIL_TICKS = 70;

    private static final float[] SEAM = {1.0f, 0.98f, 1.0f};
    private static final float[] VIOLET = {0.62f, 0.32f, 1.0f};
    private static final float[] CYAN = {0.36f, 0.90f, 1.0f};

    private static final class Cinematic {
        final Vec3 origin;
        final Vec3 direction;
        final int windupTicks;
        final long startTick;
        boolean impactCued;
        Cinematic(Vec3 origin, Vec3 direction, int windupTicks, long startTick) {
            this.origin = origin;
            this.direction = direction;
            this.windupTicks = windupTicks;
            this.startTick = startTick;
        }
    }

    private static final Map<Integer, Cinematic> CINEMATICS = new HashMap<>();

    // Scratch context loaded from the cinematic currently ticked/rendered.
    private static Vec3 origin = Vec3.ZERO;
    private static Vec3 direction = new Vec3(0, 0, 1);
    private static int windupTicks = 12;
    private static long startTick;

    private DimensionalPunchCinematic() {}

    private static void load(Cinematic cinematic) {
        origin = cinematic.origin;
        direction = cinematic.direction;
        windupTicks = cinematic.windupTicks;
        startTick = cinematic.startTick;
    }

    public static void start(int id, Vec3 pos, Vec3 dir, int windup) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 dirN = dir.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        CINEMATICS.put(id, new Cinematic(pos, dirN, Math.max(1, windup), minecraft.level.getGameTime()));
        minecraft.level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.7f, 0.35f, false);
    }

    /** Rift center: a couple of blocks ahead of the fist, where the portal lands. */
    private static Vec3 rift() {
        return origin.add(direction.scale(2.0));
    }

    /** Timeline state consumed by {@link DimensionalPostChainHandler}. */
    public record State(Vec3 origin, Vec3 rift, Vec3 direction, float age, int windupTicks) {}

    public static State state(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || CINEMATICS.isEmpty()) return null;
        long now = minecraft.level.getGameTime();
        Cinematic best = null;
        float bestAge = Float.MAX_VALUE;
        for (Cinematic cinematic : CINEMATICS.values()) {
            float age = now - cinematic.startTick + partialTick;
            if (age < 0 || age > cinematic.windupTicks + TAIL_TICKS) continue;
            if (age < bestAge) {
                bestAge = age;
                best = cinematic;
            }
        }
        if (best == null) return null;
        load(best);
        return new State(best.origin, best.origin.add(best.direction.scale(2.0)), best.direction, bestAge, best.windupTicks);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
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
            long age = now - cinematic.startTick;
            if (age > cinematic.windupTicks + TAIL_TICKS) {
                iterator.remove();
                continue;
            }
            load(cinematic);
            Vec3 rift = rift();
            double camDist = minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(rift);

            if (age < windupTicks && camDist < 48.0) {
                // Space groans: sparse portal motes gathering at the crack.
                for (int i = 0; i < 3; i++) {
                    Vec3 p = rift.add(new Vec3(RANDOM(), RANDOM(), RANDOM()).scale(1.6));
                    Vec3 v = rift.subtract(p).scale(0.06);
                    minecraft.level.addParticle(ParticleTypes.PORTAL, p.x, p.y, p.z, v.x, v.y, v.z);
                }
            }

            if (age >= windupTicks && !cinematic.impactCued) {
                cinematic.impactCued = true;
                minecraft.level.playLocalSound(rift.x, rift.y, rift.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 0.55f, false);
                minecraft.level.playLocalSound(rift.x, rift.y, rift.z,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9f, 0.7f, false);
                for (int i = 0; i < 40 && camDist < 64.0; i++) {
                    Vec3 v = new Vec3(RANDOM(), RANDOM(), RANDOM()).scale(0.35);
                    minecraft.level.addParticle(ParticleTypes.REVERSE_PORTAL, rift.x, rift.y, rift.z, v.x, v.y, v.z);
                }
            }

            // The open rift keeps breathing portal energy until it settles.
            if (age > windupTicks && age % 2 == 0 && camDist < 48.0) {
                Vec3 side = stableSide(direction);
                Vec3 up = side.cross(direction).normalize();
                double a = RANDOM() * Math.PI;
                Vec3 rim = rift.add(side.scale(Math.cos(a) * 1.8)).add(up.scale(Math.sin(a) * 1.8));
                minecraft.level.addParticle(ParticleTypes.PORTAL, rim.x, rim.y, rim.z, 0, 0.02, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || CINEMATICS.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        long now = minecraft.level.getGameTime();
        VfxQuadBatch batch = VfxQuadBatch.begin(event);
        for (Cinematic cinematic : CINEMATICS.values()) {
            float age = now - cinematic.startTick + event.getPartialTick();
            if (age < 0 || age > cinematic.windupTicks + TAIL_TICKS) continue;
            load(cinematic);
            Vec3 side = stableSide(direction);
            Vec3 up = side.cross(direction).normalize();
            if (age < windupTicks) renderWindup(batch, side, up, age / windupTicks, age);
            else renderRift(batch, side, up, age - windupTicks);
        }
        batch.close();
    }

    /** Hairline cracks crawling inward toward the point about to break. */
    private static void renderWindup(VfxQuadBatch batch, Vec3 side, Vec3 up, float progress, float age) {
        Vec3 rift = rift();
        int cracks = 7;
        for (int i = 0; i < cracks; i++) {
            double angle = hash(i * 71) * Math.PI * 2.0;
            Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            float reach = (1.0f + hash(i * 37) * 2.2f) * progress;
            Vec3 outer = rift.add(radial.scale(reach));
            batch.strip(outer, rift.add(radial.scale(0.1)), 0.025f,
                    VIOLET[0], VIOLET[1], VIOLET[2], 0.35f * progress);
        }
        batch.billboard(rift, 0.12f + 0.45f * progress * progress, SEAM[0], SEAM[1], SEAM[2], 0.45f * progress);
        batch.ring(rift, side, up, 0.2f + 1.4f * (1.0f - progress), 20,
                CYAN[0], CYAN[1], CYAN[2], 0.25f * progress, age * 0.1f);
    }

    /** The tear bursts open, jagged cracks fly out, then the portal rim settles. */
    private static void renderRift(VfxQuadBatch batch, Vec3 side, Vec3 up, float sinceImpact) {
        Vec3 rift = rift();
        float open = Mth.clamp(sinceImpact / OPEN_TICKS, 0.0f, 1.0f);
        float tail = 1.0f - Mth.clamp((sinceImpact - OPEN_TICKS) / (TAIL_TICKS - OPEN_TICKS), 0.0f, 1.0f);

        // Few-frame flash the instant space gives.
        float flash = (float) Math.exp(-sinceImpact * 1.1);
        if (flash > 0.03f) batch.billboard(rift, 2.4f + sinceImpact * 0.6f, SEAM[0], SEAM[1], SEAM[2], 0.85f * flash);

        // Jagged cracks shooting outward from the tear.
        float crackFade = 1.0f - Mth.clamp(sinceImpact / 8.0f, 0.0f, 1.0f);
        if (crackFade > 0.0f) {
            for (int i = 0; i < 11; i++) {
                double angle = hash(i * 53) * Math.PI * 2.0;
                Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                float len = (2.0f + hash(i * 29) * 4.0f) * Math.min(sinceImpact / 3.0f, 1.0f);
                Vec3 mid = rift.add(radial.scale(len * 0.5)).add(side.scale((hash(i * 13) - 0.5) * 0.6));
                batch.strip(rift, mid, 0.05f, SEAM[0], SEAM[1], SEAM[2], 0.9f * crackFade);
                batch.strip(mid, rift.add(radial.scale(len)), 0.03f,
                        VIOLET[0], VIOLET[1], VIOLET[2], 0.7f * crackFade);
            }
        }

        // The portal rim: a jagged double ring opening up, facing the punch line.
        float radius = 0.4f + 1.8f * open;
        float spin = sinceImpact * 0.06f;
        batch.ring(rift, side, up, radius, 0.14f, 40, VIOLET[0], VIOLET[1], VIOLET[2], 0.7f * tail, spin);
        batch.ring(rift, side, up, radius * 0.82f, 0.08f, 36, CYAN[0], CYAN[1], CYAN[2], 0.5f * tail, -spin * 1.3f);
        batch.ring(rift, side, up, radius * 1.12f, 0.06f, 44, SEAM[0], SEAM[1], SEAM[2], 0.3f * tail, spin * 0.7f);

        // Inner void glow — the other dimension bleeding through.
        batch.billboard(rift, radius * 0.9f, VIOLET[0] * 0.4f, VIOLET[1] * 0.2f, VIOLET[2] * 0.6f, 0.35f * open * tail);
        for (int i = 0; i < 5; i++) {
            float t = (hash(i * 91) + sinceImpact * 0.03f) % 1.0f;
            batch.billboard(rift.add(side.scale((hash(i * 7) - 0.5) * radius)).add(up.scale((hash(i * 11) - 0.5) * radius)),
                    0.12f + 0.1f * t, CYAN[0], CYAN[1], CYAN[2], 0.3f * (1.0f - t) * tail);
        }
    }

    private static double RANDOM() {
        return Math.random() * 2.0 - 1.0;
    }
}
