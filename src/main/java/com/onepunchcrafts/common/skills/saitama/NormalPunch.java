package com.onepunchcrafts.common.skills.saitama;

import com.brandon3055.draconicevolution.entity.GuardianCrystalEntity;
import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity;
import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AnimationPacket;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.onepunchcrafts.OnePunchCrafts.DRACONIC_MOD;

public class NormalPunch implements Skill {

    @Override
    public void execute(Player p) {
        consecutiveNormalPunches(p);
    }

    @Override
    public void flux(LivingEvent event) {
        if (event instanceof LivingDamageEvent damageEvent && HelpUtility.isSaitamaServerSide(damageEvent.getSource().getEntity())) {
            normalPunch(damageEvent, (ServerPlayer) damageEvent.getSource().getEntity());
        }
    }

    private static void normalPunch(LivingDamageEvent event, ServerPlayer player) {
        LivingEntity target = event.getEntity();
        double d1;
        double d0 = player.getX() - target.getX();
        for (d1 = player.getZ() - target.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
            d0 = (Math.random() - Math.random()) * 0.01D;
        }
        knockback(target, 5, d0, d1);
        event.setAmount(event.getAmount() * 10_000_000);
        event.getEntity().addTag("targetnormalpunch");
        TickScheduler.scheduleFromHere(Duration.of(5, ChronoUnit.SECONDS), () -> event.getEntity().removeTag("targetnormalpunch"));
    }

    private static void knockback(LivingEntity target, double strength, double pX, double pZ) {
        if (!(strength <= 0.0D)) {
            target.hasImpulse = true;
            Vec3 vec3 = target.getDeltaMovement();
            Vec3 vec31 = (new Vec3(pX, 0.0D, pZ)).normalize().scale(strength);
            target.setDeltaMovement(vec3.x / 2.0D - vec31.x, target.onGround() ? Math.min(0.4D, vec3.y / 2.0D + strength) : vec3.y, vec3.z / 2.0D - vec31.z);
        }
    }

    private static void consecutiveNormalPunches(Player p) {
        if (!(p.level() instanceof ServerLevel serverLevel) || !(p instanceof ServerPlayer player))
            return;
        TickScheduler.scheduleDuringAndWithInterval(Duration.of(5, ChronoUnit.SECONDS), Duration.of(50, ChronoUnit.MILLIS), () -> {
            BlockPos pStart = player.blockPosition();
            int i = 2;
            AABB pArea = new AABB(new BlockPos(pStart.getX(), pStart.getY(), pStart.getZ()),
                    new BlockPos(pStart.getX() + i, pStart.getY() + i, pStart.getZ() + i));
            serverLevel.getEntitiesOfClass(LivingEntity.class, pArea).forEach(entity -> {
                if (player.equals(entity))
                    return;
                entity.setInvulnerable(false);
                if (DRACONIC_MOD.isPresent() && entity instanceof DraconicGuardianEntity guardian) {
                    attackGuardian(player, guardian);
                    return;
                }
                player.attack(entity);
            });
            if (DRACONIC_MOD.isPresent())
                attackCrystals(player, serverLevel, pArea);
        });
        NetworkRegister.sendToAllClientsExcept(player, new AnimationPacket(player.getStringUUID(), "multiple_punches"));
        TickScheduler.scheduleFromHere(Duration.of(5, ChronoUnit.SECONDS), () -> NetworkRegister.sendToAllClientsExcept(player, new AnimationPacket(player.getStringUUID(), "stop")));
    }

    private static void attackGuardian(ServerPlayer player, DraconicGuardianEntity guardian) {
        guardian.setShieldPower(0);
        guardian.hurt(player.damageSources().playerAttack(player), (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 10_000_000));
    }

    private static void attackCrystals(ServerPlayer player, ServerLevel serverLevel, AABB pArea) {
        serverLevel.getEntitiesOfClass(GuardianCrystalEntity.class, pArea).forEach(crystal -> {
            crystal.setInvulnerable(false);
            crystal.setShieldPower(0);
            player.attack(crystal);
        });
    }
}
