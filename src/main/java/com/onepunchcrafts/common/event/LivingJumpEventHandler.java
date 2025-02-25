package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.MovementPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber
public class LivingJumpEventHandler {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void saitamaJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(sai -> {
                if (sai.isExtremeJump())
                    extremeJumpCalc(player);
            });
        }
    }

    private static void extremeJumpCalc(Player player) {
        double reachDistance = 600.0;
        HitResult result = player.pick(reachDistance, 1.0F, false);

        if (result instanceof BlockHitResult blockResult) {
            BlockPos targetPos = blockResult.getBlockPos();
            double margin = 15.0;
            double targetX = targetPos.getX() + 0.5;
            double targetY = targetPos.getY() + margin;
            double targetZ = targetPos.getZ() + 0.5;
            Vec3 diff = new Vec3(targetX - player.getX(), targetY - player.getY(), targetZ - player.getZ());
            double heightDiff = (targetPos.getY() - player.getY()) + margin;
            heightDiff = Math.max(heightDiff, 0);
            double desiredVerticalStrength = (heightDiff / 11.0) / 2;
            double scaleVertical = diff.y != 0 ? desiredVerticalStrength / diff.y : 1.0;
            final Vec3 finalVelocity = diff.scale(scaleVertical);
            double dx = targetX - player.getX();
            double dz = targetZ - player.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            double horizontalStrength = horizontalDistance * 0.3;
            Vec3 horizontalDir = new Vec3(dx, 0, dz);
            if (horizontalDir.lengthSqr() != 0) {
                horizontalDir = horizontalDir.normalize();
            }
            final double pX = horizontalDir.x * horizontalStrength;
            final double pZ = horizontalDir.z * horizontalStrength;

            player.level().getCapability(OnePunchCrafts.WORLD_RULES_CAPABILITY).ifPresent(cap -> {
                List<Double> maxStrength = cap.getMaxStrength();
                double[] movementValues = {pX, finalVelocity.y, pZ};
                for (int i = 0; i < 3; i++) {
                    if (Math.abs(movementValues[i]) > Math.abs(maxStrength.get(i))) {
                        movementValues[i] = movementValues[i] < 0 ? -maxStrength.get(i) : maxStrength.get(i);
                    }
                }
                player.setDeltaMovement(player.getDeltaMovement().add(movementValues[0], movementValues[1], movementValues[2]));
                NetworkRegister.sendToServer(new MovementPacket(player.getDeltaMovement()));
            });
        }
    }
}
