package com.onepunchcrafts.common.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.onepunchcrafts.OnePunchCrafts.PORTAL_BLOCK_ENTITY;

@Setter
@Getter
public class PortalBlockEntity extends BlockEntity {

    private ResourceKey<Level> dimension;

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(PORTAL_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        Tag dimensionTeleport = pTag.get("dimensionteleport");
        if (dimensionTeleport instanceof StringTag) {
            String dimensionStr = dimensionTeleport.getAsString();
            dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionStr));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        if (dimension != null)
            pTag.put("dimensionteleport", StringTag.valueOf(dimension.location().toString()));
    }
}
