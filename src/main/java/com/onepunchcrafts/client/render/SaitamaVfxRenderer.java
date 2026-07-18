package com.onepunchcrafts.client.render;

import com.onepunchcrafts.client.event.ScreenEffectHandler;
import com.onepunchcrafts.network.packet.SaitamaVfxPacket;
import com.onepunchcrafts.v3.content.ConsecutiveNormalPunches;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
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

    // Saitama palette: ivory-white shock, hero-suit yellow, glove red, dust.
    private static final float[] WHITE = {1.0f, 0.98f, 0.94f};
    private static final float[] YELLOW = {1.0f, 0.83f, 0.35f};
    private static final float[] RED = {0.96f, 0.24f, 0.18f};
    private static final float[] DUST = {0.84f, 0.80f, 0.73f};

    private static final java.util.Random RANDOM = new java.util.Random();

    /** Impact shakes are throttled so barrages rumble instead of convulsing. */
    private static long lastImpactShakeTick = Long.MIN_VALUE;
    private static long lastBarrageShakeTick = Long.MIN_VALUE;

    private record VfxEffect(int casterId, Vec3 pos, Vec3 direction, float scale,
                             int style, int lifeTicks, long createdTick) {}

    private static final List<VfxEffect> EFFECTS = new CopyOnWriteArrayList<>();

    private SaitamaVfxRenderer() {}

    public static void addEffect(int casterId, Vec3 pos, Vec3 direction, float scale, int style, int lifeTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 dir = direction.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : direction.normalize();
        EFFECTS.add(new VfxEffect(casterId, pos, dir, scale, style, lifeTicks, minecraft.level.getGameTime()));

        // Hit-stop feel: a short distance-attenuated kick when a punch lands.
        if (style == SaitamaVfxPacket.STYLE_PUNCH_IMPACT && scale >= 0.75f) {
            long now = minecraft.level.getGameTime();
            if (now - lastImpactShakeTick >= 6) {
                double camDist = minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(pos);
                float kick = (float) Math.min(3.0, 3.0 * 18.0 / (camDist + 10.0));
                if (kick > 0.3f) {
                    lastImpactShakeTick = now;
                    ScreenEffectHandler.addEffect(kick, 6, 0.8f);
                }
            }
        }
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
        applyBarrageRumble(minecraft, now);

        // Real dust/debris around impacts: vanilla particles read as matter in
        // a way additive quads never do. Budget is split across live impacts
        // so barrages stay dense but bounded.
        int impacts = 0;
        for (VfxEffect effect : EFFECTS)
            if (effect.style == SaitamaVfxPacket.STYLE_PUNCH_IMPACT) impacts++;
        if (impacts == 0) return;
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        for (VfxEffect effect : EFFECTS) {
            if (effect.style != SaitamaVfxPacket.STYLE_PUNCH_IMPACT) continue;
            long age = now - effect.createdTick;
            if (age >= 0 && age <= effect.lifeTicks && camera.distanceToSqr(effect.pos) < 96.0 * 96.0)
                emitImpactParticles(minecraft, effect, age, impacts);
        }
    }

    private static void applyBarrageRumble(Minecraft minecraft, long now) {
        if (lastBarrageShakeTick == now) return;
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        for (VfxEffect effect : EFFECTS) {
            if (effect.style != SaitamaVfxPacket.STYLE_BARRAGE || effect.scale < 0.9f) continue;
            int age = (int) (now - effect.createdTick);
            if (!ConsecutiveNormalPunches.isWaveTick(age)) continue;

            Entity caster = minecraft.level.getEntity(effect.casterId);
            Vec3 source = caster == null ? effect.pos : caster.position();
            double distance = camera.distanceTo(source);
            float attenuation = (float) Mth.clamp(1.0 - distance / 48.0, 0.0, 1.0);
            if (attenuation <= 0.0f) continue;

            float crescendo = ConsecutiveNormalPunches.progress(age);
            ScreenEffectHandler.addEffect((0.8f + crescendo) * attenuation, 2,
                    1.0f - 0.025f * crescendo * attenuation);
            lastBarrageShakeTick = now;
            return;
        }
    }

    private static void emitImpactParticles(Minecraft minecraft, VfxEffect effect, long age, int activeImpacts) {
        float scale = effect.scale;
        Vec3 pos = effect.pos;
        Vec3 dir = effect.direction;
        Vec3 side = stableSide(dir);
        Vec3 up = side.cross(dir).normalize();
        int budget = Math.max(1, 8 / activeImpacts);

        // Contact burst: one big puff plus sparks thrown everywhere.
        if (age == 0) {
            minecraft.level.addParticle(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 0, 0, 0);
            int sparks = Math.max(4, 14 / activeImpacts);
            for (int i = 0; i < sparks; i++) {
                Vec3 velocity = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.7,
                        RANDOM.nextGaussian()).normalize().scale((0.5 + RANDOM.nextDouble() * 0.7) * scale);
                minecraft.level.addParticle(ParticleTypes.CRIT,
                        pos.x, pos.y, pos.z, velocity.x, velocity.y + 0.1, velocity.z);
                if (i % 3 == 0)
                    minecraft.level.addParticle(ParticleTypes.FLAME,
                            pos.x, pos.y, pos.z, velocity.x * 0.5, velocity.y * 0.5, velocity.z * 0.5);
            }
        }

        // The dust wall blasting out behind the victim.
        if (age <= 8) {
            for (int i = 0; i < budget; i++) {
                double along = (0.6 + RANDOM.nextDouble() * 5.5) * scale;
                double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                Vec3 spawn = pos.add(dir.scale(along)).add(radial.scale(along * 0.35 * RANDOM.nextDouble()));
                Vec3 velocity = dir.scale(0.35 + RANDOM.nextDouble() * 0.4)
                        .add(radial.scale(0.10 + RANDOM.nextDouble() * 0.08));
                minecraft.level.addParticle(
                        i % 2 == 0 ? ParticleTypes.CLOUD : ParticleTypes.LARGE_SMOKE,
                        spawn.x, spawn.y, spawn.z, velocity.x, velocity.y + 0.02, velocity.z);
            }
        }

        // Chunks of the floor ripped up in a widening ring around the hit.
        if (age <= 5) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                    Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
            for (int i = 0; i < 4; i++) {
                cursor.move(0, -1, 0);
                BlockState state = minecraft.level.getBlockState(cursor);
                if (state.isAir()) continue;
                double ringRadius = (0.5 + age * 0.7) * scale;
                for (int k = 0; k < budget; k++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                    double gx = pos.x + Math.cos(angle) * ringRadius;
                    double gz = pos.z + Math.sin(angle) * ringRadius;
                    minecraft.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                            gx, cursor.getY() + 1.1, gz,
                            Math.cos(angle) * 0.22, 0.28 + RANDOM.nextDouble() * 0.30 * scale,
                            Math.sin(angle) * 0.22);
                    minecraft.level.addParticle(ParticleTypes.POOF,
                            gx, cursor.getY() + 1.1, gz,
                            Math.cos(angle) * 0.10, 0.05, Math.sin(angle) * 0.10);
                }
                break;
            }
        }
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

    /**
     * Anime hit-spark, layered like a fighting-game impact:
     *
     * <pre>
     *   [0..2)    impact frame — harsh white pop (biggest on frame one) and a
     *             star of razor blades crossing the contact point.
     *   [0..6)    burst — tapered radial spikes, stacked shock rings and a
     *             piercing lance shot forward through the target.
     *   [1..end)  the OPM signature: a dust cone fanning out BEHIND the
     *             victim, wind streaks chasing it, ember debris arcing down
     *             with gravity, and a ground dust ring if there is a floor.
     * </pre>
     *
     * Fast layers die in a handful of ticks; the dust is what lingers.
     */
    private static void renderPunchImpact(VfxQuadBatch batch,
                                          VfxEffect effect, float age) {
        float life = Math.max(1.0f, effect.lifeTicks);
        float scale = effect.scale;
        Vec3 pos = effect.pos;
        Vec3 dir = effect.direction;
        Vec3 side = stableSide(dir);
        Vec3 up = side.cross(dir).normalize();
        int seedBase = (int) (effect.createdTick % 100_000) * 7;

        // Impact-frame flash: pops on frame one, then a smaller second pulse.
        float flash = (float) Math.exp(-age * 1.1)
                + 0.35f * (float) Math.exp(-(age - 2.2f) * (age - 2.2f) * 1.4f);
        if (flash > 0.03f) {
            float f = Math.min(1.0f, flash);
            batch.billboard(pos, scale * (4.2f + age * 0.8f),
                    WHITE[0], WHITE[1], WHITE[2], 0.9f * f);
            batch.billboard(pos, scale * (7.0f + age * 1.2f),
                    YELLOW[0], YELLOW[1], YELLOW[2], 0.32f * f);
        }

        // Hit-spark star: long razor blades crossing THROUGH the contact point.
        if (age < 3.0f) {
            float starFade = 1.0f - age / 3.0f;
            float length = scale * (4.5f + age * 2.5f);
            for (int i = 0; i < 3; i++) {
                double angle = hash(seedBase + i * 11) * Math.PI + i * Math.PI / 3.0;
                Vec3 blade = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                batch.strip(pos.subtract(blade.scale(length)), pos.add(blade.scale(length)),
                        scale * 0.13f, WHITE[0], WHITE[1], WHITE[2], 0.9f * starFade);
                batch.strip(pos.subtract(blade.scale(length * 0.55)), pos.add(blade.scale(length * 0.55)),
                        scale * 0.36f, WHITE[0], WHITE[1], WHITE[2], 0.35f * starFade);
            }
        }

        float burstFade = 1.0f - Mth.clamp(age / 6.0f, 0.0f, 1.0f);
        if (burstFade > 0.0f) {
            // Tapered radial spikes: thick bright base, thin reaching tip.
            int spikes = 12;
            float grow = 0.45f + 0.55f * Math.min(age / 2.5f, 1.0f);
            for (int i = 0; i < spikes; i++) {
                double angle = Math.PI * 2.0 * i / spikes + hash(seedBase + i * 31) * 0.5;
                double reach = scale * (3.2 + hash(seedBase + i * 57) * 4.6) * grow;
                Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                float[] color = i % 4 == 1 ? YELLOW : WHITE;
                batch.strip(pos.add(radial.scale(reach * 0.15)), pos.add(radial.scale(reach * 0.55)),
                        scale * 0.18f, color[0], color[1], color[2], 0.75f * burstFade);
                batch.strip(pos.add(radial.scale(reach * 0.4)), pos.add(radial.scale(reach)),
                        scale * 0.07f, color[0], color[1], color[2], 0.6f * burstFade);
            }

            // Shock front: fat rings racing out to swallow the whole scene.
            batch.ring(pos, side, up, scale * (0.6f + age * 1.6f), scale * 0.30f, 26,
                    WHITE[0], WHITE[1], WHITE[2], 0.55f * burstFade, age * 0.3f);
            batch.ring(pos, side, up, scale * (0.4f + age * 2.3f), scale * 0.20f, 26,
                    RED[0], RED[1], RED[2], 0.26f * burstFade, -age * 0.2f);
            batch.ring(pos.add(dir.scale(scale * 0.6)), side, up, scale * (0.3f + age * 1.1f),
                    scale * 0.16f, 22,
                    YELLOW[0], YELLOW[1], YELLOW[2], 0.30f * burstFade, age * 0.45f);
        }

        // Piercing lance: the force punching THROUGH and out the other side.
        float lanceFade = 1.0f - Mth.clamp(age / 5.0f, 0.0f, 1.0f);
        if (lanceFade > 0.0f) {
            Vec3 tip = pos.add(dir.scale(scale * (3.5 + Math.min(age, 3.5f) * 3.0)));
            Vec3 base = pos.subtract(dir.scale(scale * 0.8));
            batch.strip(base, tip, scale * 0.22f,
                    WHITE[0], WHITE[1], WHITE[2], 0.8f * lanceFade);
            batch.strip(base, tip, scale * 0.52f,
                    YELLOW[0], YELLOW[1], YELLOW[2], 0.28f * lanceFade);
            batch.billboard(tip, scale * 0.8f, WHITE[0], WHITE[1], WHITE[2], 0.55f * lanceFade);
        }

        // OPM dust cone fanning out far behind the victim.
        if (age > 1.0f) {
            float dustP = Mth.clamp((age - 1.0f) / (life - 1.0f), 0.0f, 1.0f);
            float dustFade = (1.0f - dustP) * (1.0f - dustP);
            float spread = 0.5f + 0.9f * dustP;
            for (int k = 0; k < 5; k++) {
                double along = scale * (1.4 + k * 2.6) * (0.55 + 0.65 * dustP);
                float radius = (float) (along * 0.5) + scale * (0.5f + 1.4f * dustP);
                batch.ring(pos.add(dir.scale(along)), side, up, radius * spread, scale * 0.30f, 22,
                        DUST[0], DUST[1], DUST[2], 0.22f * dustFade * (1.0f - k * 0.13f),
                        age * 0.05f * (k % 2 == 0 ? 1 : -1) + k);
            }
            // Wind streaks chasing the cone outward.
            for (int i = 0; i < 8; i++) {
                float cycle = (hash(seedBase + i * 47) + age * 0.09f) % 1.0f;
                double along = scale * (1.0 + cycle * 11.0);
                Vec3 offset = side.scale((hash(seedBase + i * 13) - 0.5) * along * 0.9)
                        .add(up.scale((hash(seedBase + i * 17) - 0.5) * along * 0.7));
                Vec3 from = pos.add(dir.scale(along)).add(offset);
                batch.strip(from, from.add(dir.scale(scale * (1.6 + hash(seedBase + i * 23) * 1.4))),
                        scale * 0.07f, DUST[0], DUST[1], DUST[2],
                        0.40f * dustFade * (1.0f - cycle * 0.6f));
            }
        }

        // Ember debris arcing away under gravity.
        float emberFade = 1.0f - Mth.clamp(age / 12.0f, 0.0f, 1.0f);
        if (emberFade > 0.0f) {
            for (int i = 0; i < 9; i++) {
                double angle = Math.PI * 2.0 * i / 9 + hash(seedBase + i * 61) * 0.8;
                Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                Vec3 velocity = radial.scale(scale * (0.5 + hash(seedBase + i * 67) * 0.55))
                        .add(dir.scale(scale * 0.2))
                        .add(0, scale * 0.18 * hash(seedBase + i * 71), 0);
                double drop = 0.028 * age * age;
                Vec3 head = pos.add(velocity.scale(age)).add(0, -drop, 0);
                Vec3 tail = pos.add(velocity.scale(Math.max(0, age - 1.4)))
                        .add(0, -0.028 * Math.max(0, age - 1.4) * Math.max(0, age - 1.4), 0);
                float[] color = i % 3 == 0 ? RED : YELLOW;
                batch.strip(tail, head, scale * 0.06f,
                        color[0], color[1], color[2], 0.7f * emberFade);
            }
        }

        // Dust ring hugging the floor, if the hit happened near one.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                    Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
            for (int i = 0; i < 4; i++) {
                cursor.move(0, -1, 0);
                if (!minecraft.level.getBlockState(cursor).isAir()) {
                    float groundFade = 1.0f - Mth.clamp(age / life, 0.0f, 1.0f);
                    Vec3 center = new Vec3(pos.x, cursor.getY() + 1.15, pos.z);
                    batch.ring(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                            scale * (0.8f + age * 0.9f), scale * 0.26f, 26,
                            DUST[0], DUST[1], DUST[2], 0.30f * groundFade, age * 0.1f);
                    batch.ring(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                            scale * (0.4f + age * 1.3f), scale * 0.16f, 24,
                            WHITE[0], WHITE[1], WHITE[2], 0.13f * groundFade, -age * 0.08f);
                    break;
                }
            }
        }
    }

    /**
     * Consecutive Normal Punches — a wall of fists streaming down the aim cone,
     * hammering a focused target while sweeping a wide AoE arc. Layers:
     *
     * <pre>
     *   wall     — 18..32 fist afterimages fanned across the cone, re-rolled
     *              every tick so they read as one continuous motion blur; the
     *              swarm widens and thickens as the barrage builds (crescendo).
     *   sparks   — per-wave hit-sparks strobing on the focused victim, firing
     *              faster and faster to match the accelerating cadence.
     *   lines    — speed lines converging from the cone rim onto the victim.
     *   ring     — a pressure ring pulsing off the cone.
     * </pre>
     */
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

        float life = Math.max(1.0f, effect.lifeTicks);
        float progress = age / life;
        float fade = 1.0f - Mth.clamp((progress - 0.82f) / 0.18f, 0.0f, 1.0f);
        // The storm ramps up: sparse jabs at first, a dense wall by the end.
        float crescendo = 0.4f + 0.6f * Mth.clamp(progress * 1.3f, 0.0f, 1.0f);
        float scale = effect.scale;
        Vec3 side = stableSide(direction);
        Vec3 up = side.cross(direction).normalize();

        Vec3 focus = barrageFocus(minecraft, effect.casterId, origin, direction,
                ConsecutiveNormalPunches.RANGE);
        double focusDist = focus.subtract(origin).length();

        // Wall of fists fanned across the cone, re-rolled each tick to strobe.
        int bucket = (int) Math.floor(age);
        int fists = Math.round((18 + 14 * crescendo) * Math.max(0.5f, scale));
        float coneWidth = 0.55f * crescendo;
        for (int k = 0; k < fists; k++) {
            int seed = bucket * 131 + k * 17;
            double yaw = (hash(seed) - 0.5) * 2.0 * coneWidth;
            double pitch = (hash(seed + 1) - 0.5) * 1.5 * coneWidth;
            Vec3 jabDir = direction.add(side.scale(yaw)).add(up.scale(pitch)).normalize();
            double reach = (0.8 + hash(seed + 2) * Math.min(focusDist, 4.0 + 5.0 * crescendo));
            Vec3 tip = origin.add(jabDir.scale(reach));
            Vec3 tail = origin.add(jabDir.scale(Math.max(0.3, reach - (1.2 + 1.4 * crescendo))));

            float[] color = k % 4 == 0 ? YELLOW : WHITE;
            float a = (0.5f + 0.4f * hash(seed + 4)) * fade;
            // Tapered afterimage: bright thick base, thin blurred tip.
            batch.strip(tail, tip.subtract(jabDir.scale((tip.subtract(tail)).length() * 0.4)),
                    0.12f * scale, color[0], color[1], color[2], a);
            batch.strip(tail.add(jabDir.scale((tip.subtract(tail)).length() * 0.4)), tip,
                    0.05f * scale, color[0], color[1], color[2], a * 0.8f);
            batch.billboard(tip, 0.24f * scale * (0.7f + hash(seed + 3) * 0.7f),
                    color[0], color[1], color[2], 0.5f * fade);
        }

        // Normal uses its accelerating server cadence; the smaller Weak Punch
        // barrage still emits one wave per tick.
        if (scale < 0.9f || ConsecutiveNormalPunches.isWaveTick(bucket)) {
            int sSeed = bucket * 71;
            float pop = 1.0f - (age - bucket);
            batch.billboard(focus, scale * (0.8f + 0.5f * hash(sSeed)),
                    WHITE[0], WHITE[1], WHITE[2], 0.7f * pop * fade);
            for (int i = 0; i < 3; i++) {
                double a2 = hash(sSeed + i * 13) * Math.PI + i;
                Vec3 blade = side.scale(Math.cos(a2)).add(up.scale(Math.sin(a2)));
                float len = scale * (1.0f + hash(sSeed + i) * 1.2f);
                batch.strip(focus.subtract(blade.scale(len)), focus.add(blade.scale(len)),
                        0.07f * scale, WHITE[0], WHITE[1], WHITE[2], 0.7f * pop * fade);
            }
            batch.ring(focus, side, up, scale * (0.4f + (age % 4) * 0.6f), 16,
                    RED[0], RED[1], RED[2], 0.3f * fade, age * 0.3f);
        }

        // Speed lines converging from the cone rim onto the victim.
        int lines = 6;
        for (int i = 0; i < lines; i++) {
            double angle = Math.PI * 2.0 * i / lines + age * 0.15;
            Vec3 rim = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 from = focus.add(rim.scale(scale * (2.5f + 1.5f * crescendo)))
                    .subtract(direction.scale(1.5));
            Vec3 to = focus.add(rim.scale(scale * 0.6f));
            batch.strip(from, to, 0.05f * scale,
                    WHITE[0], WHITE[1], WHITE[2], 0.28f * crescendo * fade);
        }

        // Pressure ring pulsing off the cone.
        float pulse = (age % 8.0f) / 8.0f;
        batch.ring(origin.add(direction.scale(1.2 + pulse * 3.0 * scale)), side, up,
                scale * (0.6f + pulse * 2.2f), 22,
                YELLOW[0], YELLOW[1], YELLOW[2], 0.22f * (1.0f - pulse) * fade, age * 0.25f);
    }

    /** Nearest living target inside the caster's aim cone, or a point ahead. */
    private static Vec3 barrageFocus(Minecraft minecraft, int casterId, Vec3 origin, Vec3 dir, double range) {
        Vec3 fallback = origin.add(dir.scale(6.0));
        if (minecraft.level == null) return fallback;
        double minDot = Math.cos(Math.toRadians(ConsecutiveNormalPunches.HALF_ANGLE_DEGREES));
        double bestSq = range * range;
        Vec3 best = null;
        var box = new net.minecraft.world.phys.AABB(origin, origin.add(dir.scale(range))).inflate(range);
        for (net.minecraft.world.entity.LivingEntity entity :
                minecraft.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
            if (entity.getId() == casterId) continue;
            Vec3 center = entity.getBoundingBox().getCenter();
            Vec3 to = center.subtract(origin);
            double sq = to.lengthSqr();
            if (sq <= bestSq && to.normalize().dot(dir) >= minDot) {
                bestSq = sq;
                best = center;
            }
        }
        return best != null ? best : fallback;
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
