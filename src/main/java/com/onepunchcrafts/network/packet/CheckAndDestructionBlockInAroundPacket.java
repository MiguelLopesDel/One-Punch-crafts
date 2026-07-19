package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.onepunchcrafts.common.event.PlayerInteractEventhandler.everyDrop;
import com.onepunchcrafts.content.SaitamaContent;
import com.onepunchcrafts.api.presentation.VfxProfile;

public class CheckAndDestructionBlockInAroundPacket {

    private final List<BlockPos> blocksPos;

    public CheckAndDestructionBlockInAroundPacket(List<BlockPos> pos) {
        this.blocksPos = pos;
    }

    public CheckAndDestructionBlockInAroundPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.blocksPos = new ArrayList<>();
        for (int c = 0; c < size; c++)
            this.blocksPos.add(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        int totalSize = blocksPos.size();
        buf.writeInt(totalSize);
        blocksPos.forEach(buf::writeBlockPos);
    }


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (player != null && HelpUtility.hasPowerTag(player, SaitamaContent.TAG_EXTREME_SPEED))
                destroy(player);
            HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(cap -> {
                destroy(player);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private void destroy(ServerPlayer player) {
        final Level level = player.level();
        AtomicBoolean sentVfx = new AtomicBoolean();
        blocksPos.stream()
                .filter(b -> b.distSqr(player.getOnPos()) <= 25)
                .sorted(Comparator.comparingDouble(a -> a.distSqr(player.getOnPos())))
                .forEach(pos -> {
                    if (level.isLoaded(pos) && !level.getBlockState(pos).isAir()) {
                        BlockState state = level.getBlockState(pos);
                        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    5, 0.25, 0.25, 0.25, 0.12);
                            if (sentVfx.compareAndSet(false, true)
                                    && HelpUtility.getSkillData(player).getPowerState().vfxPreferences()
                                    .get(SaitamaContent.EXTREME_SPEED) == VfxProfile.NEW) {
                                SaitamaTechniqueVfxPacket.broadcast(serverLevel, new SaitamaTechniqueVfxPacket(
                                        player.getId(), net.minecraft.world.phys.Vec3.atCenterOf(pos),
                                        player.getLookAngle(), 1.4f,
                                        SaitamaTechniqueVfxPacket.BREAK_BLOCK, 8));
                            }
                        }
                        everyDrop(state, level, pos, player);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                });
    }
}
