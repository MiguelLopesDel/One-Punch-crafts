package com.onepunchcrafts.minecraft;

import com.onepunchcrafts.common.damage.DamageSourceMod;
import com.onepunchcrafts.common.damage.DamagesRegistry;
import com.onepunchcrafts.common.event.LivingDamageEventHandler;
import com.onepunchcrafts.common.vfx.SeriousPunchFront;
import com.onepunchcrafts.util.DraconicCompat;
import com.onepunchcrafts.util.ImmersivePortalsCompat;
import com.onepunchcrafts.api.Id;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.onepunchcrafts.OnePunchCrafts.DRACONIC_MOD;
import static com.onepunchcrafts.OnePunchCrafts.IMMERSIVE_PORTALS_MOD;

/** Stateful, cancel-safe destruction jobs; no scheduled lambda captures a player. */
@Mod.EventBusSubscriber
public final class MinecraftDestructionSystem {
    private static final int BLOCKS_PER_TICK = 1_000;
    // Keyed by a unique per-cast instance id, NOT the player: a player can have
    // several Serious Punches carving at once and each keeps its own job.
    private static final Map<Integer, Job> JOBS = new LinkedHashMap<>();

    private MinecraftDestructionSystem() {}

    public static void startCylinder(ServerPlayer player, Vec3 origin, Vec3 direction, double radius, double length,
                                     Id strikeId) {
        Vec3 start = origin.add(direction.scale(3));
        ArrayList<BlockPos> blocks = LivingDamageEventHandler.markBlocksToClear(player.serverLevel(), (int) radius,
                (int) length, (int) Math.floor(start.x), (int) Math.floor(start.y), (int) Math.floor(start.z), direction);
        int instanceId = SeriousPunchFront.nextInstanceId();
        JOBS.put(instanceId, new Job(player.getUUID(), instanceId, player.serverLevel(), blocks, 0,
                start, direction, (float) radius, strikeId));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Map.Entry<Integer, Job>> iterator = JOBS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Job> entry = iterator.next();
            Job job = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(job.owner);
            if (player == null) {
                iterator.remove();
                continue;
            }
            SeriousPunchFront.advance(job.level, player, job.id, job.blocks, job.index,
                    job.axisOrigin, job.direction, job.radius);
            sweepFront(job, player);
            int end = Math.min(job.blocks.size(), job.index + BLOCKS_PER_TICK);
            for (int index = job.index; index < end; index++) {
                BlockPos position = job.blocks.get(index);
                if (job.level.isLoaded(position)) job.level.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
            }
            if (end >= job.blocks.size()) {
                SeriousPunchFront.finish(job.level, player, job.id, job.blocks,
                        job.axisOrigin, job.direction, job.radius);
                iterator.remove();
            } else {
                entry.setValue(new Job(job.owner, job.id, job.level, job.blocks, end, job.axisOrigin,
                        job.direction, job.radius, job.strikeId));
            }
        }
    }

    /**
     * The punch keeps hitting whatever the front reaches, for as long as the
     * destruction travels: living entities go through the damage pipeline
     * (Saitama immunity, unstoppable escalation), while non-living combat
     * structures (end crystals, Draconic crystals, Immersive Portals) get the
     * same treatment the legacy sweep gave them.
     */
    private static void sweepFront(Job job, ServerPlayer player) {
        AABB area = new AABB(job.blocks.get(job.index)).inflate(job.radius);
        MinecraftExecutionSink sink = new MinecraftExecutionSink(player);
        job.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive())
                .forEach(entity -> sink.strike(job.strikeId, entity.getStringUUID()));

        Holder<DamageType> holder = job.level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamagesRegistry.SERIOUS_PUNCH_SECOND);
        DamageSource source = new DamageSourceMod(holder, null, player);
        job.level.getEntitiesOfClass(EndCrystal.class, area).forEach(crystal -> {
            crystal.setInvulnerable(false);
            crystal.hurt(source, 1.0e16f);
        });
        if (DRACONIC_MOD.isPresent()) DraconicCompat.hurtDraconicCrystals(job.level, area, source);
        if (IMMERSIVE_PORTALS_MOD.isPresent()) ImmersivePortalsCompat.destroyPortals(job.level, area);
    }

    private record Job(UUID owner, int id, ServerLevel level, ArrayList<BlockPos> blocks, int index,
                       Vec3 axisOrigin, Vec3 direction, float radius, Id strikeId) {}
}
