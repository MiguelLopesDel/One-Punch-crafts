package com.onepunchcrafts.common.damage;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DamageSourceMod extends DamageSource {

    public DamageSourceMod(Holder<DamageType> pType, @Nullable Entity pDirectEntity, @Nullable Entity pCausingEntity, @Nullable Vec3 pDamageSourcePosition) {
        super(pType, pDirectEntity, pCausingEntity, pDamageSourcePosition);
    }

    public DamageSourceMod(Holder<DamageType> pType, @Nullable Entity pDirectEntity, @Nullable Entity pCausingEntity) {
        super(pType, pDirectEntity, pCausingEntity);
    }

    public DamageSourceMod(Holder<DamageType> pType, Vec3 pDamageSourcePosition) {
        super(pType, pDamageSourcePosition);
    }

    public DamageSourceMod(Holder<DamageType> pType, @Nullable Entity pEntity) {
        super(pType, pEntity);
    }

    public DamageSourceMod(Holder<DamageType> pType) {
        super(pType);
    }
}
