package com.onepunchcrafts.client.v3;

import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.CheckAndDestructionBlockInAroundPacket;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.content.SaitamaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/** Client input projection for Extreme Speed; server still validates every block. */
public final class SaitamaClientSystem {
    private SaitamaClientSystem() {}

    public static void tick(Player player) {
        if (!HelpUtility.hasV3Tag(player, SaitamaContent.TAG_EXTREME_SPEED)) return;
        float forward = player.zza;
        float strafe = player.xxa;
        if (forward == 0 && strafe == 0) return;

        double radians = Math.toRadians(player.getYRot());
        Vec3 direction = new Vec3(-Math.sin(radians) * forward + Math.cos(radians) * strafe, 0,
                Math.cos(radians) * forward + Math.sin(radians) * strafe).normalize();
        AABB playerBox = player.getBoundingBox();
        Vec3 velocity = player.getDeltaMovement();
        AABB check = playerBox.move(direction.scale(Math.max(2, velocity.horizontalDistance() * 2)))
                .inflate(Math.max(0.5, velocity.horizontalDistance() * 0.5), 0, Math.max(0.5, velocity.horizontalDistance() * 0.5));
        int maxY = (int) Math.ceil(playerBox.maxY) + (velocity.y > 0.1 ? 2 : 1);
        ArrayList<BlockPos> blocks = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(check.minX); x < (int) Math.ceil(check.maxX); x++) {
            for (int y = (int) Math.floor(playerBox.minY); y < maxY; y++) {
                for (int z = (int) Math.floor(check.minZ); z < (int) Math.ceil(check.maxZ); z++) {
                    cursor.set(x, y, z);
                    if (player.level().getBlockState(cursor).isAir()) continue;
                    Vec3 toBlock = Vec3.atCenterOf(cursor).subtract(player.position());
                    Vec3 horizontal = new Vec3(toBlock.x, 0, toBlock.z);
                    if (horizontal.lengthSqr() > 0 && horizontal.normalize().dot(direction) > -0.3
                            && check.intersects(new AABB(cursor)) && y >= playerBox.minY - 0.1)
                        blocks.add(cursor.immutable());
                }
            }
        }
        if (!blocks.isEmpty()) NetworkRegister.sendToServer(new CheckAndDestructionBlockInAroundPacket(blocks));
    }
}
