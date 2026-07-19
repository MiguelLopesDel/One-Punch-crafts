package com.onepunchcrafts.client.render;

import com.onepunchcrafts.network.packet.SaitamaTechniqueVfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.onepunchcrafts.OnePunchCrafts.MODID;
import static com.onepunchcrafts.client.render.VfxQuadBatch.hash;
import static com.onepunchcrafts.client.render.VfxQuadBatch.stableSide;

/** Physical, world-reaction VFX for Saitama's utility and body techniques. */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class SaitamaTechniqueVfxRenderer {
    private static final float[] WHITE = {1.0f, 0.99f, 0.96f};
    private static final float[] GRAY = {0.55f, 0.58f, 0.61f};
    private static final float[] DUST = {0.78f, 0.74f, 0.67f};
    private static final float[] RED = {0.82f, 0.12f, 0.10f};
    private static final Random RANDOM = new Random();

    private record Effect(int casterId, Vec3 origin, Vec3 direction, float scale,
                          int style, int lifeTicks, long started) {}

    private static final List<Effect> EFFECTS = new CopyOnWriteArrayList<>();

    private SaitamaTechniqueVfxRenderer() {}

    public static void addEffect(int casterId, Vec3 origin, Vec3 direction,
                                 float scale, int style, int lifeTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 safeDirection = direction.lengthSqr() < 1.0e-5 ? new Vec3(0, 0, 1) : direction.normalize();
        // Continuous locomotion events refresh one trail instead of piling up.
        if (style == SaitamaTechniqueVfxPacket.SWIM_WAKE
                || style == SaitamaTechniqueVfxPacket.EXTREME_SPEED
                || style == SaitamaTechniqueVfxPacket.SERIOUS_FART)
            EFFECTS.removeIf(effect -> effect.casterId == casterId && effect.style == style);
        EFFECTS.add(new Effect(casterId, origin, safeDirection, scale, style,
                Math.max(1, lifeTicks), minecraft.level.getGameTime()));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            EFFECTS.clear();
            return;
        }
        long now = minecraft.level.getGameTime();
        EFFECTS.removeIf(effect -> now - effect.started > effect.lifeTicks + 2L);
        for (Effect effect : EFFECTS) {
            long age = now - effect.started;
            if (age < 0 || age > effect.lifeTicks || age > 5) continue;
            emitMatter(minecraft, effect, age);
        }
    }

    private static void emitMatter(Minecraft minecraft, Effect effect, long age) {
        if (effect.style == SaitamaTechniqueVfxPacket.SERIOUS_FART) {
            for (int i = 0; i < 3; i++) {
                Vec3 back = effect.direction.scale(-0.35 - RANDOM.nextDouble() * 0.55);
                minecraft.level.addParticle(i == 0 ? ParticleTypes.CLOUD : ParticleTypes.POOF,
                        effect.origin.x, effect.origin.y, effect.origin.z, back.x, back.y, back.z);
            }
        } else if (effect.style == SaitamaTechniqueVfxPacket.SWIM_WAKE) {
            minecraft.level.addParticle(ParticleTypes.BUBBLE,
                    effect.origin.x + RANDOM.nextGaussian() * 0.3,
                    effect.origin.y + RANDOM.nextGaussian() * 0.25,
                    effect.origin.z + RANDOM.nextGaussian() * 0.3,
                    -effect.direction.x * 0.2, 0.08, -effect.direction.z * 0.2);
        } else if ((effect.style == SaitamaTechniqueVfxPacket.WEIGHT
                || effect.style == SaitamaTechniqueVfxPacket.EXTREME_JUMP) && age <= 2) {
            BlockPos below = BlockPos.containing(effect.origin).below();
            BlockState state = minecraft.level.getBlockState(below);
            if (!state.isAir()) {
                for (int i = 0; i < Math.min(12, 3 + (int) effect.scale); i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    minecraft.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                            effect.origin.x, below.getY() + 1.05, effect.origin.z,
                            Math.cos(angle) * 0.16 * effect.scale,
                            0.12 + RANDOM.nextDouble() * 0.25,
                            Math.sin(angle) * 0.16 * effect.scale);
                }
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EFFECTS.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        long now = minecraft.level.getGameTime();
        try (VfxQuadBatch batch = VfxQuadBatch.begin(event)) {
            for (Effect effect : EFFECTS) {
                float age = now - effect.started + event.getPartialTick();
                if (age < 0 || age > effect.lifeTicks) continue;
                switch (effect.style) {
                    case SaitamaTechniqueVfxPacket.WEAKENING -> weakening(batch, effect, age);
                    case SaitamaTechniqueVfxPacket.QUICK_BACKSTAB -> displacement(batch, effect, age, false);
                    case SaitamaTechniqueVfxPacket.AREA_ROUTE -> displacement(batch, effect, age, true);
                    case SaitamaTechniqueVfxPacket.SERIOUS_FART -> seriousFart(batch, effect, age);
                    case SaitamaTechniqueVfxPacket.WEIGHT -> groundReaction(batch, effect, age, false);
                    case SaitamaTechniqueVfxPacket.KNOCKBACK_RESIST -> resistance(batch, effect, age);
                    case SaitamaTechniqueVfxPacket.ATTACK_KNOCKBACK -> launchWake(batch, effect, age);
                    case SaitamaTechniqueVfxPacket.SWIM_WAKE -> swimWake(batch, effect, age);
                    case SaitamaTechniqueVfxPacket.BREAK_BLOCK -> breakBlock(batch, effect, age);
                    case SaitamaTechniqueVfxPacket.EXTREME_SPEED -> extremeSpeed(batch, effect, age);
                    case SaitamaTechniqueVfxPacket.EXTREME_JUMP -> groundReaction(batch, effect, age, true);
                    case SaitamaTechniqueVfxPacket.DASH -> displacement(batch, effect, age, false);
                    default -> { }
                }
            }
        }
    }

    private static void weakening(VfxQuadBatch batch, Effect e, float age) {
        float fade = fade(e, age);
        Vec3 side = stableSide(e.direction);
        Vec3 up = side.cross(e.direction).normalize();
        for (int sign : new int[]{-1, 1}) {
            Vec3 center = e.origin.add(side.scale(sign * (0.25 + age * 0.04)));
            batch.ring(center, side, up, e.scale * (0.28f + age * 0.11f), 18,
                    WHITE[0], WHITE[1], WHITE[2], 0.46f * fade, sign * age * 0.2f);
        }
        if (age < 2.5f) batch.strip(e.origin.subtract(up.scale(1.2)), e.origin.add(up.scale(1.2)),
                0.035f, WHITE[0], WHITE[1], WHITE[2], 0.8f * (1 - age / 2.5f));
    }

    private static void displacement(VfxQuadBatch batch, Effect e, float age, boolean route) {
        float fade = fade(e, age);
        double length = Math.max(1.5, e.scale);
        Vec3 end = e.origin.add(e.direction.scale(length));
        batch.strip(e.origin, end, route ? 0.055f : 0.09f,
                WHITE[0], WHITE[1], WHITE[2], 0.65f * fade);
        batch.strip(e.origin, end, route ? 0.16f : 0.25f,
                GRAY[0], GRAY[1], GRAY[2], 0.20f * fade);
        int ghosts = route ? 3 : 5;
        for (int i = 0; i < ghosts; i++) {
            double t = (i + 0.5) / ghosts;
            Vec3 ghost = e.origin.lerp(end, t);
            batch.billboard(ghost, 0.25f + 0.35f * fade,
                    i == ghosts - 1 ? RED[0] : WHITE[0],
                    i == ghosts - 1 ? RED[1] : WHITE[1],
                    i == ghosts - 1 ? RED[2] : WHITE[2], 0.18f * fade);
        }
        Vec3 side = stableSide(e.direction);
        Vec3 up = side.cross(e.direction).normalize();
        batch.ring(e.origin, side, up, 0.3f + age * 0.35f, 20,
                WHITE[0], WHITE[1], WHITE[2], 0.36f * fade, 0);
    }

    private static void seriousFart(VfxQuadBatch batch, Effect e, float age) {
        float fade = fade(e, age);
        Vec3 back = e.direction.scale(-1);
        Vec3 side = stableSide(back);
        Vec3 up = side.cross(back).normalize();
        for (int i = 0; i < 8; i++) {
            int seed = i * 31 + (int) e.started;
            Vec3 ray = back.add(side.scale((hash(seed) - 0.5) * 0.55))
                    .add(up.scale((hash(seed + 1) - 0.5) * 0.45)).normalize();
            batch.strip(e.origin, e.origin.add(ray.scale((1.2 + hash(seed + 2) * 3.5) * e.scale)),
                    0.06f, WHITE[0], WHITE[1], WHITE[2], 0.38f * fade);
        }
        batch.ring(e.origin.add(back.scale(age * 0.35)), side, up,
                e.scale * (0.3f + age * 0.25f), 22,
                GRAY[0], GRAY[1], GRAY[2], 0.45f * fade, 0);
    }

    private static void groundReaction(VfxQuadBatch batch, Effect e, float age, boolean jump) {
        float fade = fade(e, age);
        Vec3 x = new Vec3(1, 0, 0);
        Vec3 z = new Vec3(0, 0, 1);
        float force = Mth.clamp(e.scale, 0.4f, 6.0f);
        batch.ring(e.origin, x, z, 0.25f + age * force * 0.32f,
                0.08f + force * 0.025f, 28, DUST[0], DUST[1], DUST[2], 0.58f * fade, 0);
        batch.ring(e.origin, x, z, 0.15f + age * force * 0.48f,
                22, WHITE[0], WHITE[1], WHITE[2], 0.22f * fade, 0);
        if (jump) batch.strip(e.origin.add(0, 0.1, 0),
                e.origin.add(e.direction.scale(Math.max(2, force * 2.0))), 0.08f,
                WHITE[0], WHITE[1], WHITE[2], 0.34f * fade);
    }

    private static void resistance(VfxQuadBatch batch, Effect e, float age) {
        float fade = fade(e, age);
        Vec3 side = stableSide(e.direction);
        Vec3 up = side.cross(e.direction).normalize();
        // The incoming pressure separates around the body; this is intentionally not a shield sphere.
        for (int sign : new int[]{-1, 1}) {
            Vec3 split = e.origin.add(side.scale(sign * (0.35 + age * 0.12)));
            batch.strip(e.origin.subtract(e.direction.scale(1.1)),
                    split.add(e.direction.scale(0.9)), 0.06f,
                    WHITE[0], WHITE[1], WHITE[2], 0.5f * fade);
        }
        batch.ring(e.origin.add(0, -0.85, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                0.18f + age * 0.16f, 16, DUST[0], DUST[1], DUST[2], 0.42f * fade, 0);
    }

    private static void launchWake(VfxQuadBatch batch, Effect e, float age) {
        float fade = fade(e, age);
        Vec3 side = stableSide(e.direction);
        Vec3 up = side.cross(e.direction).normalize();
        Vec3 center = e.origin.add(e.direction.scale(age * 0.7 * e.scale));
        batch.ring(center, side, up, 0.25f + age * 0.22f * e.scale, 22,
                WHITE[0], WHITE[1], WHITE[2], 0.48f * fade, 0);
        batch.strip(e.origin, center.add(e.direction.scale(e.scale * 1.5)), 0.08f,
                GRAY[0], GRAY[1], GRAY[2], 0.34f * fade);
    }

    private static void swimWake(VfxQuadBatch batch, Effect e, float age) {
        float fade = fade(e, age);
        Vec3 side = stableSide(e.direction);
        Vec3 up = side.cross(e.direction).normalize();
        Vec3 back = e.direction.scale(-1);
        for (int i = 0; i < 5; i++) {
            Vec3 center = e.origin.add(back.scale(i * 0.55 + age * 0.12));
            batch.ring(center, side, up, 0.28f + i * 0.10f, 16,
                    WHITE[0], WHITE[1], WHITE[2], (0.28f - i * 0.035f) * fade, 0);
        }
    }

    private static void breakBlock(VfxQuadBatch batch, Effect e, float age) {
        float fade = fade(e, age);
        Vec3 side = stableSide(e.direction);
        Vec3 up = side.cross(e.direction).normalize();
        batch.ring(e.origin, side, up, Math.max(0.2f, 0.75f - age * 0.09f), 20,
                WHITE[0], WHITE[1], WHITE[2], 0.55f * fade, age * 0.25f);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0 + hash(i + (int) e.started) * 0.3;
            Vec3 ray = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            batch.strip(e.origin, e.origin.add(ray.scale(0.5 + age * 0.18)), 0.025f,
                    GRAY[0], GRAY[1], GRAY[2], 0.5f * fade);
        }
    }

    private static void extremeSpeed(VfxQuadBatch batch, Effect e, float age) {
        float fade = fade(e, age);
        Vec3 back = e.direction.scale(-1);
        Vec3 side = stableSide(e.direction);
        Vec3 up = side.cross(e.direction).normalize();
        int seedBase = (int) e.started * 19;
        for (int i = 0; i < 12; i++) {
            Vec3 offset = side.scale((hash(seedBase + i) - 0.5) * 1.5)
                    .add(up.scale((hash(seedBase + i + 1) - 0.5) * 1.8));
            Vec3 from = e.origin.add(offset);
            batch.strip(from, from.add(back.scale(1.5 + e.scale * (1 + hash(seedBase + i + 2)))),
                    0.045f, WHITE[0], WHITE[1], WHITE[2], 0.42f * fade);
        }
        batch.billboard(e.origin.add(back.scale(0.5)), 0.7f + e.scale * 0.18f,
                WHITE[0], WHITE[1], WHITE[2], 0.14f * fade);
    }

    private static float fade(Effect e, float age) {
        float p = Mth.clamp(age / Math.max(1.0f, e.lifeTicks), 0, 1);
        return (1 - p) * (1 - p);
    }
}
