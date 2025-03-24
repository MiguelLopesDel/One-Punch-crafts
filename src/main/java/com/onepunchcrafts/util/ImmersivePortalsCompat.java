package com.onepunchcrafts.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.IPRegistry;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;

public class ImmersivePortalsCompat {

    public static void placeByWayBiFacedPortal(ServerPlayer player, ResourceKey<Level> dimension) {
        Portal portal = new Portal(IPRegistry.PORTAL.get(), player.level());
        Vec3 frontPosition = getFrontPosition(player, 2);
        portal.setOriginPos(frontPosition);
        portal.setDestinationDimension(dimension);
        portal.setDestination(frontPosition.add(frontPosition.x, 100 - frontPosition.y, frontPosition.z));
        portal.setOrientationAndSize(new Vec3(1, 0, 0), new Vec3(0, 1, 0),
                4, 4);
        McHelper.spawnServerEntity(portal);
        PortalManipulation.completeBiWayBiFacedPortal(portal, p -> {
        }, p -> {
        }, IPRegistry.PORTAL.get());
    }

    private static Vec3 getFrontPosition(ServerPlayer player, double distance, double yOffset) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(distance));
        if (yOffset != 0) {
            targetPos = new Vec3(targetPos.x, targetPos.y + yOffset, targetPos.z);
        }
        return targetPos;
    }

    private static Vec3 getFrontPosition(ServerPlayer player, double distance) {
        return getFrontPosition(player, distance, 0);
    }

    public static BlockPos getFrontBlockPosition(ServerPlayer player, double distance) {
        Vec3 frontPosition = getFrontPosition(player, distance);
        return new BlockPos((int) frontPosition.x, (int) frontPosition.y, (int) frontPosition.z);
    }
}
