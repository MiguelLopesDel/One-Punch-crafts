package com.onepunchcrafts.common.capability;

import com.onepunchcrafts.OnePunchCrafts;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import static com.onepunchcrafts.OnePunchCrafts.WITHOUT_PACK;

public class OnePunchCraftsProvider implements ICapabilitySerializable<Tag> {

    private final OnePunchPlayer dataPlayer;
    private final LazyOptional<OnePunchPlayer> instance;

    public OnePunchCraftsProvider() {
        this.dataPlayer = new OnePunchPlayer(WITHOUT_PACK);
        this.instance = LazyOptional.of(() -> this.dataPlayer);
    }

    @Override
    public Tag serializeNBT() {
        return this.dataPlayer.writeNBT();
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
        return (cap == OnePunchCrafts.ONE_PLAYER_CAPABILITY) ? this.instance.cast() : LazyOptional.empty();
    }

    @Override
    public void deserializeNBT(final Tag nbt) {
        this.dataPlayer.firstReadNBT(nbt);
    }
}
