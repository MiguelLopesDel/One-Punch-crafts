package com.onepunchcrafts.common.mixin;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.runtime.combat.DamagePipeline;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Earliest VanillaInbound Seam for runtime-owned targets; no EventBus ordering race. */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onePunchCrafts$powerTargetPolicy(DamageSource source, float amount,
                                                CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player
                && HelpUtility.getSkillData(player).getPowerState().tags().contains(DamagePipeline.SAITAMA_TARGET)) {
            callback.setReturnValue(false);
        }
    }
}
