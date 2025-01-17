package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillPassive;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.CheckAndDestructionBlockInAroundPacket;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;

public class ExtremeSpeed implements SkillPassive {

    @Override
    public void execute(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            HelpUtility.verifyIsSaitamaAndGetCapability(serverPlayer).ifPresent(sai -> {
                sai.setExtremeSpeedActive(!sai.isExtremeSpeedActive());
                NetworkRegister.sendToPlayer(serverPlayer, new PlayerSyncPacket(sai));
            });
        }
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide() && ((SaitamaPack) HelpUtility.getSaitamaPack(player).get().getSkillPack()).isExtremeSpeedActive()) {
            breakNearlyBlocks(player);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void breakNearlyBlocks(Player player) {
        Level world = player.level();
        float forward = player.zza;
        float strafe = player.xxa;
        float yaw = player.getYRot();

        if (forward == 0 && strafe == 0) {
            return;
        }

        double rad = Math.toRadians(yaw);
        double mx = -Math.sin(rad) * forward + Math.cos(rad) * strafe;
        double mz = Math.cos(rad) * forward + Math.sin(rad) * strafe;
        Vec3 movementDirection = new Vec3(mx, 0, mz).normalize();

        AABB playerBox = player.getBoundingBox();
//        Vec3 velocity = player.getDeltaMovement();
//        int minY = (int) Math.floor(playerBox.minY) - 1; // Considera 1 bloco abaixo para colisões no chão
//        int maxY = (int) Math.ceil(playerBox.maxY) + 1; // Considera 1 bloco acima para colisões no teto
//        Vec3 predictedPosition = player.position().add(velocity);
//        AABB nextBox = playerBox.move(predictedPosition.subtract(player.position()));


        AABB nextBox = playerBox.move(movementDirection);
        AABB checkBox = nextBox.inflate(0.2, 0, 0.2);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int minX = (int) Math.floor(checkBox.minX);
        int maxX = (int) Math.ceil(checkBox.maxX);
        int minY = (int) Math.floor(playerBox.minY);
        int maxY = (int) Math.ceil(playerBox.maxY) + 1;
        int minZ = (int) Math.floor(checkBox.minZ);
        int maxZ = (int) Math.ceil(checkBox.maxZ);
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    mutablePos.set(x, y, z);
                    if (!world.getBlockState(mutablePos).isAir()) {
                        Vec3 blockCenter = new Vec3(x + 0.5, y + 0.5, z + 0.5);
                        Vec3 toBlock = blockCenter.subtract(player.position());
                        if (toBlock.dot(movementDirection) > 0) {
                            AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
                            if (nextBox.intersects(blockBox)) {
                                positions.add(mutablePos.immutable());
                            }
                        }
                    }
                }
            }
        }
        NetworkRegister.sendToServer(new CheckAndDestructionBlockInAroundPacket(positions));
    }
}
