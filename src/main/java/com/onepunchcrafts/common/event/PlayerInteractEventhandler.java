package com.onepunchcrafts.common.event;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.content.SaitamaContent;
import com.onepunchcrafts.network.packet.SaitamaTechniqueVfxPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

import static net.minecraft.world.level.block.Block.getDrops;
import static net.minecraft.world.level.block.Block.popResource;

@Mod.EventBusSubscriber
public class PlayerInteractEventhandler {

    @SubscribeEvent
    public static void breakBlocks(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (HelpUtility.hasPowerTag(player, SaitamaContent.TAG_BREAK_BLOCKS)) {
                Level level = event.getLevel();
                BlockPos pos = event.getPos();
                BlockState state = level.getBlockState(pos);
                blockBreakVfx(player, pos, state);
                everyDrop(state, level, pos, player);
                level.destroyBlock(pos, false);
                return;
            }
            HelpUtility.verifyIsSaitamaAndGetCapability((ServerPlayer) event.getEntity()).ifPresent(cap -> {
                if (!cap.isBreakBlocksQuickly())
                    return;
                Level level = event.getLevel();
                BlockPos pos = event.getPos();
                everyDrop(level.getBlockState(pos), level, pos, player);
                level.destroyBlock(pos, false);
            });
        }
    }

    private static void blockBreakVfx(ServerPlayer player, BlockPos pos, BlockState state) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 direction = player.getLookAngle();
        SaitamaTechniqueVfxPacket.broadcast(player.serverLevel(), new SaitamaTechniqueVfxPacket(
                player.getId(), center, direction, 1.0f,
                SaitamaTechniqueVfxPacket.BREAK_BLOCK, 10));
        player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                center.x, center.y, center.z, 12, 0.28, 0.28, 0.28, 0.18);
    }

    public static void everyDrop(BlockState blockState, Level level, BlockPos pos, ServerPlayer player) {
        if (blockState.getBlock().getLootTable() == BuiltInLootTables.EMPTY) {
            ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), blockState.getBlock().asItem().getDefaultInstance());
            level.addFreshEntity(itemEntity);
        } else {
            List<ItemStack> drops = getDrops(blockState, (ServerLevel) level, pos, null, player, player.getMainHandItem());
            if (drops.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), blockState.getBlock().asItem().getDefaultInstance());
                level.addFreshEntity(itemEntity);
            } else
                drops.forEach((item) -> popResource(level, pos, item));
        }
        blockState.spawnAfterBreak((ServerLevel) level, pos, ItemStack.EMPTY, true);
    }
}
