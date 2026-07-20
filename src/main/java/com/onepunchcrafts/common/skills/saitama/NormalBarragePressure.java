package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.event.LivingDamageEventHandler;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The pent-up pressure of Consecutive Normal Punches. Unlike a single Normal
 * Punch (which explodes on the victim right away), the barrage holds its force
 * back so it does not disrupt the flurry:
 *
 * <ul>
 *   <li>Nearby entities in the aim cone are tracked as the likely targets, even
 *       if none is being hit at the exact moment.</li>
 *   <li>When every tracked target is dead, the force has nowhere left to go and
 *       bursts at each of them (the "they died mid-combo" release).</li>
 *   <li>When the barrage ends, the terrain behind the aim is blown open (the
 *       punch pressure venting) plus a local explosion — always.</li>
 * </ul>
 */
@Mod.EventBusSubscriber
public final class NormalBarragePressure {

    private static final double RANGE = 16.0;
    private static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(55.0));
    private static final float KILL_POWER = 9.0f;
    private static final float FINISH_POWER = 12.0f;
    private static final int TRENCH_RADIUS = 4;
    private static final int TRENCH_LENGTH = 12;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private NormalBarragePressure() {}

    /** Begin tracking likely targets for a fresh Normal barrage. */
    public static void start(ServerPlayer caster) {
        SESSIONS.put(caster.getUUID(), new Session(caster.getUUID()));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next().getValue();
            ServerPlayer caster = event.getServer().getPlayerList().getPlayer(session.owner);
            if (caster == null) {
                iterator.remove();
                continue;
            }
            ServerLevel level = caster.serverLevel();
            Vec3 origin = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();

            // Keep folding in whoever wanders into the aim cone — they are the
            // targets, even if the flurry has not connected with them yet.
            for (LivingEntity target : coneTargets(level, caster, origin, look)) {
                session.tracked.add(target.getUUID());
                session.lastPos.put(target.getUUID(), target.getBoundingBox().getCenter());
            }

            // Release early only once every tracked target is gone.
            if (!session.released && !session.tracked.isEmpty() && allGone(level, session)) {
                detonateKills(level, caster, session);
                session.released = true;
            }
        }
    }

    /** The barrage ended: vent the pressure whether or not anything died. */
    public static void finish(ServerPlayer caster, Vec3 origin, Vec3 look) {
        Session session = SESSIONS.remove(caster.getUUID());
        ServerLevel level = caster.serverLevel();
        if (session != null && !session.released && !session.tracked.isEmpty()) {
            detonateKills(level, caster, session);
        }
        Vec3 impact = origin.add(look.scale(6.0));
        openTerrainBehind(level, impact, look);
        localExplosion(level, caster, impact);
    }

    private static List<LivingEntity> coneTargets(ServerLevel level, ServerPlayer caster, Vec3 origin, Vec3 look) {
        AABB bounds = new AABB(origin, origin.add(look.scale(RANGE))).inflate(RANGE);
        return level.getEntitiesOfClass(LivingEntity.class, bounds, target -> {
            if (target == caster || !target.isAlive()) return false;
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
            return toTarget.lengthSqr() <= RANGE * RANGE && toTarget.normalize().dot(look) >= HALF_ANGLE_COS;
        });
    }

    private static boolean allGone(ServerLevel level, Session session) {
        for (UUID id : session.tracked) {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive()) return false;
        }
        return true;
    }

    /** Force with nowhere to go: a burst at every target the barrage was on. */
    private static void detonateKills(ServerLevel level, ServerPlayer caster, Session session) {
        for (Vec3 pos : session.lastPos.values()) {
            HelpUtility.explodeWithoutKnockBackFor(caster, pos.x, pos.y, pos.z, KILL_POWER);
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 12, 0.2, 0.2, 0.2, 0.02);
        }
    }

    /** Blow the terrain open along the aim, past the target — the pressure venting. */
    private static void openTerrainBehind(ServerLevel level, Vec3 impact, Vec3 look) {
        List<BlockPos> blocks = LivingDamageEventHandler.markBlocksToClear(level, TRENCH_RADIUS, TRENCH_LENGTH,
                (int) Math.floor(impact.x), (int) Math.floor(impact.y), (int) Math.floor(impact.z), look);
        for (BlockPos pos : blocks) {
            if (level.isLoaded(pos) && !level.getBlockState(pos).isAir())
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void localExplosion(ServerLevel level, ServerPlayer caster, Vec3 impact) {
        HelpUtility.explodeWithoutKnockBackFor(caster, impact.x, impact.y, impact.z, FINISH_POWER);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 2.2f, 0.6f);
    }

    private static final class Session {
        final UUID owner;
        final java.util.Set<UUID> tracked = new java.util.HashSet<>();
        final Map<UUID, Vec3> lastPos = new LinkedHashMap<>();
        boolean released;
        Session(UUID owner) { this.owner = owner; }
    }
}
