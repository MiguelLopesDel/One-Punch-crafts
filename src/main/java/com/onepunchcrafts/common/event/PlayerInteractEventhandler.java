package com.onepunchcrafts.common.event;

import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

import static net.minecraft.world.level.block.Block.getDrops;
import static net.minecraft.world.level.block.Block.popResource;

@Mod.EventBusSubscriber
public class PlayerInteractEventhandler {

    @SubscribeEvent
    public static void breakBlocks(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer) {
            HelpUtility.verifyIsSaitamaAndGetCapability((ServerPlayer) event.getEntity()).ifPresent(cap -> {
                if (!cap.isBreakBlocksQuickly())
                    return;
                Level level = event.getLevel();
                BlockPos pos = event.getPos();
                everyDrop(level.getBlockState(pos), level, pos);
                level.destroyBlock(pos, false);
            });
        }
    }

    public static void everyDrop(BlockState blockState, Level level, BlockPos pos) {
        if (blockState.getBlock().getLootTable() == BuiltInLootTables.EMPTY) {
            ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), blockState.getBlock().asItem().getDefaultInstance());
            level.addFreshEntity(itemEntity);
        } else {
            List<ItemStack> drops = getDrops(blockState, (ServerLevel) level, pos, null);
            if (drops.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), blockState.getBlock().asItem().getDefaultInstance());
                level.addFreshEntity(itemEntity);
            } else
                drops.forEach((item) -> {
                    popResource(level, pos, item);
                });
        }
        blockState.spawnAfterBreak((ServerLevel) level, pos, ItemStack.EMPTY, true);
    }
}
