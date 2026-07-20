package com.onepunchcrafts.minecraft;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.runtime.combat.BorosMitigationInterceptor;
import net.darkhax.attributefix.mixin.AccessorRangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.common.ForgeMod;

/**
 * Ensures Minecraft attribute bounds can represent the domain's absolute
 * values. AttributeFix is a hard dependency and loads first (mods.toml orders
 * us AFTER it); this runs on {@code FMLLoadCompleteEvent} and raises every cap
 * the mod pushes past the vanilla/Forge ceiling, so no AttributeFix config
 * needs to be edited by hand. Existing (possibly larger) values are preserved.
 */
public final class MinecraftAttributeScale {
    private MinecraftAttributeScale() {}

    public static void applyRequiredBounds() {
        // Boros carries hundreds of millions of health.
        ensureMaximum(Attributes.MAX_HEALTH,
                BorosMitigationInterceptor.MAX_HEALTH, "minecraft:generic.max_health");
        // Boros' attribute-based melee damage (100k base * up to 8x form scale).
        ensureMaximum(Attributes.ATTACK_DAMAGE, 10_000_000.0, "minecraft:generic.attack_damage");
        // Saitama/Boros adjustable knockback (up to 500, times Boros form scale).
        ensureMaximum(Attributes.ATTACK_KNOCKBACK, 5_000.0, "minecraft:generic.attack_knockback");
        // Knockback resistance dialled up to 500 (vanilla ceiling is 1.0).
        ensureMaximum(Attributes.KNOCKBACK_RESISTANCE, 512.0, "minecraft:generic.knockback_resistance");
        // Saitama's weight maps to Forge gravity (weight/10, up to 50; cap is 8).
        ensureMaximum(ForgeMod.ENTITY_GRAVITY.get(), 64.0, "forge:entity_gravity");
        // Boros' swim speed (up to 500, times form scale; Forge cap is 1024).
        ensureMaximum(ForgeMod.SWIM_SPEED.get(), 5_000.0, "forge:swim_speed");
    }

    static double requiredMaximum(double configuredMaximum, double domainMaximum) {
        return Math.max(configuredMaximum, domainMaximum);
    }

    private static void ensureMaximum(Attribute attribute, double domainMaximum, String id) {
        if (!(attribute instanceof RangedAttribute ranged)) return;
        double configuredMaximum = ranged.getMaxValue();
        double requiredMaximum = requiredMaximum(configuredMaximum, domainMaximum);
        if (requiredMaximum == configuredMaximum) return;

        ((AccessorRangedAttribute) ranged).attributefix$setMaxValue(requiredMaximum);
        OnePunchCrafts.LOGGER.info("Raised {} maximum from {} to {} for OnePunchCrafts domain scale.",
                id, configuredMaximum, requiredMaximum);
    }
}
