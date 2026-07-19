package com.onepunchcrafts.minecraft;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.runtime.combat.BorosMitigationInterceptor;
import net.darkhax.attributefix.mixin.AccessorRangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Ensures Minecraft attribute bounds can represent the domain's absolute values. */
public final class MinecraftAttributeScale {
    private MinecraftAttributeScale() {}

    public static void applyRequiredBounds() {
        ensureMaximum((RangedAttribute) Attributes.MAX_HEALTH,
                BorosMitigationInterceptor.MAX_HEALTH, "minecraft:generic.max_health");
    }

    static double requiredMaximum(double configuredMaximum, double domainMaximum) {
        return Math.max(configuredMaximum, domainMaximum);
    }

    private static void ensureMaximum(RangedAttribute attribute, double domainMaximum, String id) {
        double configuredMaximum = attribute.getMaxValue();
        double requiredMaximum = requiredMaximum(configuredMaximum, domainMaximum);
        if (requiredMaximum == configuredMaximum) return;

        ((AccessorRangedAttribute) attribute).attributefix$setMaxValue(requiredMaximum);
        OnePunchCrafts.LOGGER.info("Raised {} maximum from {} to {} for OnePunchCrafts domain scale.",
                id, configuredMaximum, requiredMaximum);
    }
}
