package com.onepunchcrafts.common.event;

import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;


@Mod.EventBusSubscriber
public class LivingDamageEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void saitamaAttack(LivingDamageEvent event) {
        HelpUtility.passServerFluxToAllPlayers(event);
    }

    //contem logica de ray cast
    public static ArrayList<BlockPos> markBlocksToClear(ServerLevel level, int radius, int height, int startX, int startY, int startZ, Vec3 direction) {
        ArrayList<BlockPos> blocksPos = new ArrayList<>();
        Vec3 normalizedDirection = direction.normalize();
        double absX = Math.abs(normalizedDirection.x);
        double absY = Math.abs(normalizedDirection.y);
        double absZ = Math.abs(normalizedDirection.z);
        int alignedAxis = (absX >= absY && absX >= absZ) ? 1 :
                (absY >= absX && absY >= absZ) ? 2 : 3;
        for (int z = 0; z < height; z++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    double dx = x;
                    double dy = y;
                    if (dx * dx + dy * dy <= radius * radius) {
                        Vec3 offset;
                        if (alignedAxis == 1) {
                            offset = new Vec3(0, y, x).add(normalizedDirection.scale(z));
                        } else if (alignedAxis == 2) {
                            offset = new Vec3(x, 0, y).add(normalizedDirection.scale(z));
                        } else {
                            offset = new Vec3(x, y, 0).add(normalizedDirection.scale(z));
                        }
                        BlockPos pPos = new BlockPos(startX + (int) offset.x, startY + (int) offset.y, startZ + (int) offset.z);
                        if (level.isLoaded(pPos))
                            blocksPos.add(pPos);
                    }
                }
            }
        }
        return blocksPos;
    }
}