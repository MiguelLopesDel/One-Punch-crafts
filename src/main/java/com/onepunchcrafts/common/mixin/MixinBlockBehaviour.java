package com.onepunchcrafts.common.mixin;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.content.SaitamaContent;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
        } else if (pContext instanceof EntityCollisionContext context && context.getEntity() instanceof Player player) {
            if (HelpUtility.hasV3Tag(player, SaitamaContent.TAG_EXTREME_SPEED) && shouldRemoveCollisionAggressive(player, pPos)) {
                cir.setReturnValue(Shapes.empty());
                return;
            }
            HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(cap -> {
                if (cap.isExtremeSpeedActive()) {
                    if (shouldRemoveCollisionAggressive(player, pPos)) {
                        cir.setReturnValue(Shapes.empty());
                    }
                }
            });
        }
    }

    @Unique
    private boolean shouldRemoveCollisionAggressive(Player player, BlockPos blockPos) {
        Vec3 playerPos = player.position();
        AABB playerBox = player.getBoundingBox();

        // REGRA 1: NUNCA remove collision de blocos muito abaixo dos pés
        if (blockPos.getY() < playerBox.minY - 1.5) {
            return false;
        }

        // REGRA 2: Distância horizontal - se está próximo, remove collision
        Vec3 blockCenter = Vec3.atCenterOf(blockPos);
        double horizontalDist = Math.sqrt(
                Math.pow(blockCenter.x - playerPos.x, 2) +
                        Math.pow(blockCenter.z - playerPos.z, 2)
        );

        // Se o bloco está próximo horizontalmente, remove collision
        if (horizontalDist <= 3.0) {

            // PROTEÇÃO ESPECÍFICA: Não remove de blocos diretamente abaixo dos pés
            if (blockPos.getY() <= playerBox.minY && horizontalDist < 0.7) {
                return false; // É o chão onde está pisando
            }

            // REGRA 3: Se há movimento horizontal, remove collision
            Vec3 velocity = player.getDeltaMovement();
            if (velocity.horizontalDistanceSqr() > 0.05) {
                return true;
            }

            // REGRA 4: Se o jogador está pressionando movimento, remove collision
            if (player.zza != 0 || player.xxa != 0) {
                return true;
            }
        }

        return false;
    }

}
