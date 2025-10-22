package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.util.ExplosionWithoutKnockBack;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class BorosEnergyProjection implements Skill {

    @Override
    public SkillExecutionResult execute(Player p) {
        if (p instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            BorosPack borosPack = HelpUtility.getSkillData(player, BorosPack.class);
            if (borosPack.getEnergy() < 1) {
                return null;
            }

            borosPack.setEnergy((short) (borosPack.getEnergy() - 1));

            fireEnergyBeam(player, level, borosPack.getCurrentForm());
        }
        return null;
    }

    private void fireEnergyBeam(ServerPlayer player, ServerLevel level, int form) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        double range = 100.0 + (form * 20);
        Vec3 end = start.add(direction.scale(range));

        level.playSound(null, start.x, start.y, start.z,
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.5F);

        for (int i = 0; i <= 30; i++) {
            Vec3 particlePos = start.add(direction.scale(i));
            level.sendParticles(ParticleTypes.DRAGON_BREATH,
                    particlePos.x, particlePos.y, particlePos.z, 5, 0.2, 0.2, 0.2, 0.1);
        }

        AABB damageBox = new AABB(start, end).inflate(7.0 + form);
        level.getEntitiesOfClass(LivingEntity.class, damageBox,
                entity -> !entity.is(player)).forEach(target -> {

            float damage = 300_000 + (form * 15);
            target.hurt(player.damageSources().playerAttack(player), damage);

            Vec3 knockback = direction.scale(3.0 + form);
            target.setDeltaMovement(knockback.add(0, 1.0, 0));
            target.hasImpulse = true;
        });
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 explosionPos = hit.getType() == BlockHitResult.Type.BLOCK ? hit.getLocation() : end;
        level.explode(player, explosionPos.x, explosionPos.y, explosionPos.z, 7.0f + form, false, Level.ExplosionInteraction.TNT);
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.boros.energy_projection"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, Color.MAGENTA.getRGB(), false);
    }
}