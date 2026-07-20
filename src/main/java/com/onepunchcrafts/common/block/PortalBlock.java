package com.onepunchcrafts.common.block;

import com.onepunchcrafts.common.block.entity.PortalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

public class PortalBlock extends BaseEntityBlock {
    public PortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The block entity renderer draws a free-standing spatial tear.
        return RenderShape.INVISIBLE;
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!world.isClientSide && !entity.isOnPortalCooldown()) {
            entity.setPortalCooldown(10);
            world.getServer().tell(new TickTask(1, () -> teleportTo(entity, pos)));
        }
    }

    private static void teleportTo(Entity entity, BlockPos pos) {
        BlockEntity block = entity.level().getBlockEntity(pos) instanceof PortalBlockEntity ? entity.level().getBlockEntity(pos) :
                entity.level().getBlockEntity(pos.below());
        if (block instanceof PortalBlockEntity blockEntity) {
            teleportToCustomDimension(entity, blockEntity.getDimension());
        }
    }

    public static void teleportToCustomDimension(Entity entity, ResourceKey<Level> dimension) {
        if (!entity.canChangeDimensions())
            return;
        ServerLevel level = entity.getServer().getLevel(dimension);
        if (level != null)
            entity.teleportTo(level, entity.getX(), entity.getY(), entity.getZ(), new HashSet<>(), entity.getYRot(), entity.getXRot());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PortalBlockEntity(pPos, pState);
    }
}
