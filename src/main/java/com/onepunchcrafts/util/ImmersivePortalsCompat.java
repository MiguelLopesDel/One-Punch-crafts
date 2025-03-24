package com.onepunchcrafts.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.IPRegistry;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;

import java.util.List;

public class ImmersivePortalsCompat {

    public static void placeByWayBiFacedPortalOrDestroy(ServerPlayer player, ResourceKey<Level> dimension) {
        Portal portal = new Portal(IPRegistry.PORTAL.get(), player.level());
        Vec3 frontPosition = HelpUtility.getFrontPosition(player, 2);
        portal.setOriginPos(frontPosition);
        portal.setDestinationDimension(dimension);
        portal.setDestination(frontPosition.add(frontPosition.x, 100 - frontPosition.y, frontPosition.z));
        portal.setOrientationAndSize(new Vec3(1, 0, 0), new Vec3(0, 1, 0),
                4, 4);
        List<Portal> entitiesOfClass = player.level().getEntitiesOfClass(Portal.class, portal.getBoundingBox().inflate(2));
        if (entitiesOfClass.isEmpty()) {
            McHelper.spawnServerEntity(portal);
            PortalManipulation.completeBiWayBiFacedPortal(portal, p -> {}, p -> {}, IPRegistry.PORTAL.get());
        } else
            entitiesOfClass.forEach(Entity::kill);
    }

    public static void destroyPortals(ServerLevel level, AABB pArea) {
        level.getEntitiesOfClass(Portal.class, pArea).forEach(Entity::kill);
    }
}
