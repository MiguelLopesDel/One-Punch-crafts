package com.onepunchcrafts.v3.minecraft;

import com.onepunchcrafts.common.event.LivingDamageEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Stateful, cancel-safe destruction jobs; no scheduled lambda captures a player. */
@Mod.EventBusSubscriber
public final class MinecraftDestructionSystem {
    private static final int BLOCKS_PER_TICK = 1_000;
    private static final Map<UUID, Job> JOBS = new LinkedHashMap<>();

    private MinecraftDestructionSystem() {}

    public static void startCylinder(ServerPlayer player, Vec3 origin, Vec3 direction, double radius, double length) {
        Vec3 start = origin.add(direction.scale(3));
        ArrayList<BlockPos> blocks = LivingDamageEventHandler.markBlocksToClear(player.serverLevel(), (int) radius,
                (int) length, (int) Math.floor(start.x), (int) Math.floor(start.y), (int) Math.floor(start.z), direction);
        JOBS.put(player.getUUID(), new Job(player.serverLevel(), blocks, 0));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Map.Entry<UUID, Job>> iterator = JOBS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Job> entry = iterator.next();
            Job job = entry.getValue();
            if (event.getServer().getPlayerList().getPlayer(entry.getKey()) == null) {
                iterator.remove();
                continue;
            }
            int end = Math.min(job.blocks.size(), job.index + BLOCKS_PER_TICK);
            for (int index = job.index; index < end; index++) {
                BlockPos position = job.blocks.get(index);
                if (job.level.isLoaded(position)) job.level.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
            }
            if (end >= job.blocks.size()) iterator.remove();
            else entry.setValue(new Job(job.level, job.blocks, end));
        }
    }

    private record Job(ServerLevel level, ArrayList<BlockPos> blocks, int index) {}
}
