package com.onepunchcrafts.common.capability;

import com.onepunchcrafts.OnePunchCrafts;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class OnePunchCraftsLevelProvider implements ICapabilitySerializable<Tag> {

    private final WorldRules worldRules;
    private final LazyOptional<WorldRules> instance;

    public OnePunchCraftsLevelProvider() {
        this.worldRules = new WorldRules();
        this.instance = LazyOptional.of(() -> this.worldRules);
    }

    @Override
    public Tag serializeNBT() {
        return this.worldRules.writeNBT();
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
        return (cap == OnePunchCrafts.WORLD_RULES_CAPABILITY) ? this.instance.cast() : LazyOptional.empty();
    }

    @Override
    public void deserializeNBT(final Tag nbt) {
        this.worldRules.readNBT(nbt);
    }
}
