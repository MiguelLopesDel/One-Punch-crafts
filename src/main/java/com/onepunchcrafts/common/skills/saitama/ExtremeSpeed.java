package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillPassive;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.CheckAndDestructionBlockInAroundPacket;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;
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
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        HelpUtility.verifyIsSaitamaAndGetCapability(Minecraft.getInstance().player).ifPresent(sai -> guiGraphics.drawString(font, Component.translatable("skill.saitama.extreme_speed"), width / 2 - defaultReduce, height / 2 + defaultAdd, sai.isExtremeSpeedActive() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false));
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
        Vec3 velocity = player.getDeltaMovement();

        double predictionMultiplier = Math.max(2.0, velocity.horizontalDistance() * 2);
        Vec3 predictedMovement = movementDirection.scale(predictionMultiplier);

        AABB nextBox = playerBox.move(predictedMovement);
        double expansionFactor = Math.max(0.5, velocity.horizontalDistance() * 0.5);
        AABB checkBox = nextBox.inflate(expansionFactor, 0, expansionFactor);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int minX = (int) Math.floor(checkBox.minX);
        int maxX = (int) Math.ceil(checkBox.maxX);

        int minY = (int) Math.floor(playerBox.minY);
        int maxY = (int) Math.ceil(playerBox.maxY) + 1;

        if (velocity.y > 0.1) {
            maxY = (int) Math.ceil(playerBox.maxY) + 2;
        }

        int minZ = (int) Math.floor(checkBox.minZ);
        int maxZ = (int) Math.ceil(checkBox.maxZ);

        ArrayList<BlockPos> positions = new ArrayList<>();
        Vec3 playerPos = player.position();
        Vec3 playerFeet = new Vec3(playerPos.x, playerBox.minY, playerPos.z);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    mutablePos.set(x, y, z);
                    if (!world.getBlockState(mutablePos).isAir()) {
                        Vec3 blockCenter = new Vec3(x + 0.5, y + 0.5, z + 0.5);
                        Vec3 toBlock = blockCenter.subtract(playerPos);

                        Vec3 horizontalToBlock = new Vec3(toBlock.x, 0, toBlock.z);
                        double dot = horizontalToBlock.normalize().dot(movementDirection);

                        if (dot > -0.3) {
                            AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);

                            if (checkBox.intersects(blockBox)) {
                                if (y >= playerBox.minY - 0.1) {
                                    positions.add(mutablePos.immutable());
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!positions.isEmpty()) {
            NetworkRegister.sendToServer(new CheckAndDestructionBlockInAroundPacket(positions));
        }
    }
}
