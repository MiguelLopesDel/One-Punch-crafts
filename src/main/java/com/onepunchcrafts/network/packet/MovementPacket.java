package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

public class MovementPacket {

    @Nullable
    private final Vec3 deltaMovement;

    public MovementPacket(@NotNull Vec3 deltaMovement) {
        this.deltaMovement = deltaMovement;
    }

    public MovementPacket() {
        this.deltaMovement = null;
    }

    public void encode(FriendlyByteBuf buffer) {
        if (deltaMovement != null) {
            buffer.writeBoolean(true);
            buffer.writeDouble(deltaMovement.x);
            buffer.writeDouble(deltaMovement.y);
            buffer.writeDouble(deltaMovement.z);
        } else {
            buffer.writeBoolean(false);
        }
    }

    public MovementPacket(FriendlyByteBuf buffer) {
        if (buffer.readBoolean())
            this.deltaMovement = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        else
            this.deltaMovement = null;
    }


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(sai -> {
            MobEffectInstance effect = player.getEffect(MobEffects.JUMP);
            if (!sai.isExtremeJump() && effect != null && effect.getAmplifier() >= TickUtil.convertTimeInTicks(Duration.ofSeconds(3))) {
                player.serverLevel().explode(null, player.getX(), player.getY(), player.getZ(), 10,
                        Level.ExplosionInteraction.MOB);
            } else if (this.deltaMovement != null && sai.isExtremeJump())
                extremeJump(player);
        });
        ctx.get().setPacketHandled(true);
    }

    private void extremeJump(ServerPlayer player) {
        player.level().getCapability(OnePunchCrafts.WORLD_RULES_CAPABILITY).ifPresent(cap -> {
            List<Double> maxStrength = cap.getMaxStrength();
            double[] movementValues = {deltaMovement.x, deltaMovement.y, deltaMovement.z};
            for (int i = 0; i < 3; i++) {
                if (Math.abs(movementValues[i]) > Math.abs(maxStrength.get(i))) {
                    movementValues[i] = movementValues[i] < 0 ? -maxStrength.get(i) : maxStrength.get(i);
                }
            }
            player.setDeltaMovement(movementValues[0], movementValues[1], movementValues[2]);
        });
    }
}
