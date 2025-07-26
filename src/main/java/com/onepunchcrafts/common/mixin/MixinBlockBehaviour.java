package com.onepunchcrafts.common.mixin;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockBehaviour extends StateHolder<Block, BlockState> {

    @Shadow
    protected abstract BlockState asState();

    @Unique
    private static final Map<Integer, VoxelShape> SHAPES_CACHE = new HashMap<>();

    protected MixinBlockBehaviour(Block pOwner, ImmutableMap<Property<?>, Comparable<?>> pValues, MapCodec<BlockState> pPropertiesCodec) {
        super(pOwner, pValues, pPropertiesCodec);
    }

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("RETURN"), cancellable = true)
    public void getCollisionShape(BlockGetter level, BlockPos pPos, CollisionContext pContext, CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = this.asState();
        if (state.getBlock() instanceof LiquidBlock && !state.getFluidState().isEmpty() &&
                pContext instanceof EntityCollisionContext context && context.getEntity() instanceof Player player && HelpUtility.canWalk(player, state.getFluidState())) {
            cir.setReturnValue(SHAPES_CACHE.computeIfAbsent((int) (state.getFluidState().getHeight(level, pPos) * 15) + 2, i -> Block.box(0.0, 0.0, 0.0, 16.0, i, 16.0)));
        } else if (state.getBlock() instanceof LiquidBlockContainer && state.getFluidState().is(FluidTags.WATER) &&
                pContext instanceof EntityCollisionContext context && context.getEntity() instanceof Player player && HelpUtility.canWalk(player, state.getFluidState())) {
            cir.setReturnValue(Shapes.join(cir.getReturnValue(), SHAPES_CACHE.computeIfAbsent(15, i -> Block.box(0.0, 0.0, 0.0, 16.0, i, 16.0)), BooleanOp.OR));
        }
    }
}
