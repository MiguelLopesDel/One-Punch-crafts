package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.common.block.entity.PortalBlockEntity;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.onepunchcrafts.OnePunchCrafts.IMMERSIVE_PORTALS_MOD;
import static com.onepunchcrafts.OnePunchCrafts.PORTAL_BLOCK;
import static com.onepunchcrafts.util.HelpUtilityMod.*;

public class TeleportPacket {


    private ResourceKey<Level> dimension;

    public TeleportPacket() {
    }

    public TeleportPacket(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public void encode(FriendlyByteBuf buf) {
        boolean pValue = dimension != null;
        buf.writeBoolean(pValue);
        if (pValue) {
            buf.writeResourceKey(dimension);
        }
    }

    public TeleportPacket(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            dimension = buf.readResourceKey(Registries.DIMENSION);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(cap -> {
            if (dimension == null) {
                HelpUtility.teleportPlayerToTarget(player);
            } else if (IMMERSIVE_PORTALS_MOD.isPresent() && isValidDimension(player, dimension)) {
                placeByWayBiFacedPortal(player, dimension);
            } else if (isValidDimension(player, dimension)) {
                ServerLevel serverLevel = player.serverLevel();
                BlockPos pos = getFrontBlockPosition(player, 2);
                if (serverLevel.getBlockEntity(pos) instanceof PortalBlockEntity)
                    serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                else {
                    serverLevel.setBlockAndUpdate(pos, PORTAL_BLOCK.get().defaultBlockState());
                    ((PortalBlockEntity) serverLevel.getBlockEntity(pos)).setDimension(dimension);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private boolean isValidDimension(ServerPlayer player, ResourceKey<Level> dimension) {
        return player.level().getServer().levelKeys().contains(dimension);
    }
}
