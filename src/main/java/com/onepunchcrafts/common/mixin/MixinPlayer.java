package com.onepunchcrafts.common.mixin;

import com.onepunchcrafts.v3.minecraft.MinecraftPowerDispatcher;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class MixinPlayer extends LivingEntity implements net.minecraftforge.common.extensions.IForgePlayer {

    @Shadow public abstract Iterable<ItemStack> getArmorSlots();

    @Shadow public abstract HumanoidArm getMainArm();

    @Shadow public abstract void setItemSlot(EquipmentSlot pSlot, ItemStack pStack);

    @Shadow public abstract ItemStack getItemBySlot(EquipmentSlot pSlot);

    @Shadow public abstract float getAttackStrengthScale(float pAdjustTicks);

    @Shadow public abstract SoundSource getSoundSource();

    @Shadow public abstract float getSpeed();

    @Shadow public abstract void sweepAttack();

    @Shadow public abstract void crit(Entity pEntityHit);

    @Shadow public abstract void magicCrit(Entity pEntityHit);

    @Shadow public abstract void awardStat(ResourceLocation pStat, int pIncrement);

    @Shadow public abstract void causeFoodExhaustion(float pExhaustion);

    @Shadow public abstract void resetAttackStrengthTicker();

    protected MixinPlayer(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void attack(Entity pTarget) {
        if ((Object) this instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && MinecraftPowerDispatcher.primaryAttack(serverPlayer, pTarget)) return;
        if (!ForgeHooks.onPlayerAttackTarget((Player) (Object )this, pTarget)) return;
        if (pTarget.isAttackable()) {
            if (!pTarget.skipAttackInteraction(this)) {
             float f = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
             float f1;
             if (pTarget instanceof LivingEntity) {
                 f1 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity) pTarget).getMobType());
             } else {
                 f1 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), MobType.UNDEFINED);
             }

             float f2 = this.getAttackStrengthScale(0.5F);
             f *= 0.2F + f2 * f2 * 0.8F;
             f1 *= f2;
             if (f > 0.0F || f1 > 0.0F) {
                 boolean flag = f2 > 0.9F;
                 boolean flag1 = false;
                 float i = (float) this.getAttributeValue(Attributes.ATTACK_KNOCKBACK); // Forge: Initialize this value to the attack knockback attribute of the player, which is by default 0
                 i += EnchantmentHelper.getKnockbackBonus(this);
                 if (this.isSprinting() && flag) {
                     this.level().playSound((Player) null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F);
                     ++i;
                     flag1 = true;
                 }

                 boolean flag2 = flag && this.fallDistance > 0.0F && !this.onGround() && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && pTarget instanceof LivingEntity;
                 flag2 = flag2 && !this.isSprinting();
                 CriticalHitEvent hitResult = ForgeHooks.getCriticalHit((Player) (Object )this, pTarget, flag2, flag2 ? 1.5F : 1.0F);
                 flag2 = hitResult != null;
                 if (flag2) {
                     f *= hitResult.getDamageModifier();
                 }

                 f += f1;
                 boolean flag3 = false;
                 double d0 = (double) (this.walkDist - this.walkDistO);
                 if (flag && !flag2 && !flag1 && this.onGround() && d0 < (double) this.getSpeed()) {
                     ItemStack itemstack = this.getItemInHand(InteractionHand.MAIN_HAND);
                     flag3 = itemstack.canPerformAction(ToolActions.SWORD_SWEEP);
                 }

                 float f4 = 0.0F;
                 boolean flag4 = false;
                 int j = EnchantmentHelper.getFireAspect(this);
                 if (pTarget instanceof LivingEntity) {
                     f4 = ((LivingEntity) pTarget).getHealth();
                     if (j > 0 && !pTarget.isOnFire()) {
                         flag4 = true;
                         pTarget.setSecondsOnFire(1);
                     }
                 }

                 Vec3 vec3 = pTarget.getDeltaMovement();
                 boolean flag5 = pTarget.hurt(this.damageSources().playerAttack((Player) (Object )this), f);
                 if (flag5) {
                     if (i > 0) {
                         if (pTarget instanceof LivingEntity) {
                             ((LivingEntity) pTarget).knockback((double) ((float) i * 0.5F), (double) Mth.sin(this.getYRot() * ((float) Math.PI / 180F)), (double) (-Mth.cos(this.getYRot() * ((float) Math.PI / 180F))));
                         } else {
                             pTarget.push((double) (-Mth.sin(this.getYRot() * ((float) Math.PI / 180F)) * (float) i * 0.5F), 0.1D, (double) (Mth.cos(this.getYRot() * ((float) Math.PI / 180F)) * (float) i * 0.5F));
                         }

                         this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
                         this.setSprinting(false);
                     }

                     if (flag3) {
                         float f3 = 1.0F + EnchantmentHelper.getSweepingDamageRatio(this) * f;

                         for (LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, this.getItemInHand(InteractionHand.MAIN_HAND).getSweepHitBox((Player) (Object )this, pTarget))) {
                             double entityReachSq = Mth.square(this.getEntityReach()); // Use entity reach instead of constant 9.0. Vanilla uses bottom center-to-center checks here, so don't update this to use canReach, since it uses closest-corner checks.
                             if (livingentity != this && livingentity != pTarget && !this.isAlliedTo(livingentity) && (!(livingentity instanceof ArmorStand) || !((ArmorStand) livingentity).isMarker()) && this.distanceToSqr(livingentity) < entityReachSq) {
                                 livingentity.knockback((double) 0.4F, (double) Mth.sin(this.getYRot() * ((float) Math.PI / 180F)), (double) (-Mth.cos(this.getYRot() * ((float) Math.PI / 180F))));
                                 livingentity.hurt(this.damageSources().playerAttack((Player) (Object )this), f3);
                             }
                         }

                         this.level().playSound((Player) null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F);
                         this.sweepAttack();
                     }

                     if (pTarget instanceof ServerPlayer && pTarget.hurtMarked) {
                         ((ServerPlayer) pTarget).connection.send(new ClientboundSetEntityMotionPacket(pTarget));
                         pTarget.hurtMarked = false;
                         pTarget.setDeltaMovement(vec3);
                     }

                     if (flag2) {
                         this.level().playSound((Player) null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F);
                         this.crit(pTarget);
                     }

                     if (!flag2 && !flag3) {
                         if (flag) {
                             this.level().playSound((Player) null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F);
                         } else {
                             this.level().playSound((Player) null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F);
                         }
                     }

                     if (f1 > 0.0F) {
                         this.magicCrit(pTarget);
                     }

                     this.setLastHurtMob(pTarget);
                     if (pTarget instanceof LivingEntity) {
                         EnchantmentHelper.doPostHurtEffects((LivingEntity) pTarget, this);
                     }

                     EnchantmentHelper.doPostDamageEffects(this, pTarget);
                     ItemStack itemstack1 = this.getMainHandItem();
                     Entity entity = pTarget;
                     if (pTarget instanceof PartEntity) {
                         entity = ((PartEntity<?>) pTarget).getParent();
                     }

                     if (!this.level().isClientSide && !itemstack1.isEmpty() && entity instanceof LivingEntity) {
                         ItemStack copy = itemstack1.copy();
                         itemstack1.hurtEnemy((LivingEntity) entity, (Player) (Object )this);
                         if (itemstack1.isEmpty()) {
                             ForgeEventFactory.onPlayerDestroyItem((Player) (Object )this, copy, InteractionHand.MAIN_HAND);
                             this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                         }
                     }

                     if (pTarget instanceof LivingEntity) {
                         float f5 = f4 - ((LivingEntity) pTarget).getHealth();
                         this.awardStat(Stats.DAMAGE_DEALT, Math.round(f5 * 10.0F));
                         if (j > 0) {
                             pTarget.setSecondsOnFire(j * 4);
                         }

                         if (this.level() instanceof ServerLevel && f5 > 2.0F) {
                             int k = (int) ((double) f5 * 0.5D);
//                             ((ServerLevel) this.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, pTarget.getX(), pTarget.getY(0.5D), pTarget.getZ(), k, 0.1D, 0.0D, 0.1D, 0.2D);
                         }
                     }

                     this.causeFoodExhaustion(0.1F);
                 } else {
                     this.level().playSound((Player) null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F);
                     if (flag4) {
                         pTarget.clearFire();
                     }
                 }
             }
             this.resetAttackStrengthTicker(); // FORGE: Moved from beginning of attack() so that getAttackStrengthScale() returns an accurate value during all attack events

            }
        }
    }

    @Override
    public boolean alwaysAccepts() {
        return this.alwaysAccepts();
    }

    @Override
    public LivingEntity self() {
        return super.self();
    }
}
