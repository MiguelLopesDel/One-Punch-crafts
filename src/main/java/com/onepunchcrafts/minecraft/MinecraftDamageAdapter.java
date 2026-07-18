package com.onepunchcrafts.minecraft;

import com.onepunchcrafts.common.damage.DamageSourceMod;
import com.onepunchcrafts.common.damage.DamagesRegistry;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.combat.DamageApplier;
import com.onepunchcrafts.api.combat.DamageContext;
import com.onepunchcrafts.api.combat.DamageSpec;
import com.onepunchcrafts.api.combat.DamageTarget;
import com.onepunchcrafts.api.combat.DamageTier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** VanillaOutbound Adapter: the sole runtime owner of hurt()/fallback mutation. */
public final class MinecraftDamageAdapter implements DamageTarget, DamageApplier {
    private final ServerPlayer attacker;
    private final LivingEntity target;

    public MinecraftDamageAdapter(ServerPlayer attacker, LivingEntity target) {
        this.attacker = attacker;
        this.target = target;
    }

    @Override public String stableId() { return target.getStringUUID(); }
    @Override public double health() { return target.getHealth(); }
    @Override public boolean alive() { return target.isAlive(); }
    @Override public boolean hasTag(Id tag) {
        return target instanceof ServerPlayer player && HelpUtility.getSkillData(player).getPowerState().tags().contains(tag);
    }

    @Override public ApplyResult apply(DamageContext context) {
        double before = target.getHealth();
        if (context.spec().iFramePolicy() == DamageSpec.IFramePolicy.IGNORE) target.invulnerableTime = 0;
        if (context.spec().tier() == DamageTier.SERIOUS) target.setSecondsOnFire(60);
        boolean accepted = target.hurt(source(context.spec()), (float) context.spec().amount());
        return result(before, accepted ? Method.NORMAL : Method.DENIED);
    }

    @Override public ApplyResult force(DamageContext context, ApplyResult deniedResult) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return deniedResult;
        target.invulnerableTime = 0;
        target.hurt(attacker.serverLevel().damageSources().fellOutOfWorld(), (float) context.spec().amount());
        if (!target.isAlive()) return result(deniedResult.healthBefore(), Method.FALLBACK_DAMAGE);

        target.setHealth(0);
        target.die(source(context.spec()));
        if (target.isAlive()) target.setHealth(0);
        return result(deniedResult.healthBefore(), Method.DIRECT_HEALTH);
    }

    private DamageSource source(DamageSpec spec) {
        if (spec.tier() != DamageTier.SERIOUS) return attacker.damageSources().playerAttack(attacker);
        Holder<DamageType> holder = attacker.serverLevel().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamagesRegistry.SERIOUS_PUNCH);
        return new DamageSourceMod(holder, attacker, attacker);
    }

    private ApplyResult result(double before, Method method) {
        return new ApplyResult(before, target.getHealth(), target.isAlive(), method);
    }
}
