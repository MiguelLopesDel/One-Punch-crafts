package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.onepunchcrafts.common.event.PlayerInteractEventhandler.everyDrop;

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
        HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(cap -> {
            final Level level = player.level();
            blocksPos.stream().filter(b -> b.distSqr(player.getOnPos()) <= 5).forEach(pos -> {
                if (level.isLoaded(pos) && !level.getBlockState(pos).isAir()) {
                    everyDrop(level.getBlockState(pos), level, pos);
                    level.destroyBlock(pos, false);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
