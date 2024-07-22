package com.onepunchcrafts.common.event;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.RegisterSounds;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AnimationPacket;
import com.onepunchcrafts.util.TickScheduler;
import com.onepunchcrafts.util.TickUtilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Mod.EventBusSubscriber
public class LivingDamageEventHandler {

    @SubscribeEvent
    public static void saitamaAttack(LivingDamageEvent event) {
        if (event.getSource() == null)
            return;
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            saitamaOnAttackEntity(event, player);
        }
    }

    private static void saitamaOnAttackEntity(LivingDamageEvent event, ServerPlayer player) {
        player.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            if (cap.isSaitama()) {
                switch (cap.getActualAbility()) {
                    case 0:
                        event.setAmount(event.getAmount() * 100_000);
                        break;
                    case 1:
                        LivingEntity target = event.getEntity();
                        double d1;
                        double d0 = player.getX() - target.getX();
                        for (d1 = player.getZ() - target.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
                            d0 = (Math.random() - Math.random()) * 0.01D;
                        }
                        knockback(target, 5, d0, d1);
                        event.setAmount(event.getAmount() * 10_000_000);
                        event.getEntity().addTag("targetsaitama");
                        break;
                    case 2:
                        PerformSeriousPunch(event, player);
                        break;
                }
            }
        });
    }

    private static void PerformSeriousPunch(LivingDamageEvent event, ServerPlayer player) {
        ServerLevel serverLevel = player.serverLevel();
        clientEffects(player);
        LivingEntity target = event.getEntity();

        target.setInvulnerable(false);
        target.setSecondsOnFire(60);
        event.setAmount(event.getAmount() * 10_000_000_000_000_000f);

        seriousPunchWithoutSpecificTargetWithClientEffects(player, serverLevel);
    }

    public static void seriousPunchWithoutSpecificTargetWithClientEffects(ServerPlayer player, ServerLevel serverLevel) {
        clientEffects(player);
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        Vec3 cylinderStartPos = playerPos.add(lookVec.scale(3));


        ArrayList<BlockPos> blockPos = markBlocksToClear(serverLevel, 15, 1000, (int) Math.floor(cylinderStartPos.x), (int) Math.floor(cylinderStartPos.y), (int) Math.floor(cylinderStartPos.z), lookVec);
        final TickUtilities tickU = new TickUtilities();
        TickScheduler.scheduleWithCondition(Duration.of(50, ChronoUnit.MILLIS), () -> tickU.fillCylinderAndEmuleEffects(serverLevel, 1000, blockPos));
    }

    private static void clientEffects(ServerPlayer player) {
        player.serverLevel().playSound(null, player.getOnPos(), RegisterSounds.SERIOUS_PUNCH.get(), SoundSource.PLAYERS, 1, 1);
        NetworkRegister.sendToPlayer(player, new AnimationPacket("punch_animation"));
    }

    private static ArrayList<BlockPos> markBlocksToClear(ServerLevel level, int radius, int height, int startX, int startY, int startZ, Vec3 direction) {
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

    private static void knockback(LivingEntity target, double strength, double pX, double pZ) {
        if (!(strength <= 0.0D)) {
            target.hasImpulse = true;
            Vec3 vec3 = target.getDeltaMovement();
            Vec3 vec31 = (new Vec3(pX, 0.0D, pZ)).normalize().scale(strength);
            target.setDeltaMovement(vec3.x / 2.0D - vec31.x, target.onGround() ? Math.min(0.4D, vec3.y / 2.0D + strength) : vec3.y, vec3.z / 2.0D - vec31.z);
        }
    }
}