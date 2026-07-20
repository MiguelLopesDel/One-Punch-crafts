package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.common.dimension.DimensionalPortalManager;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client asks the server to throw a Dimensional Punch toward the chosen
 * dimension: Saitama's fist tears space open and a portal to {@code dimension}
 * is left behind (see {@link DimensionalPortalManager}).
 */
public class DimensionalPunchPacket {

    private final ResourceKey<Level> dimension;

    public DimensionalPunchPacket(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public DimensionalPunchPacket(FriendlyByteBuf buf) {
        this.dimension = buf.readResourceKey(Registries.DIMENSION);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceKey(dimension);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null
                && (HelpUtility.hasSaitamaPowerSet(player) || HelpUtility.verifyIsSaitamaAndGetCapability(player).isPresent())
                && player.level().getServer().levelKeys().contains(dimension)) {
            DimensionalPortalManager.punch(player, dimension);
        }
        ctx.get().setPacketHandled(true);
    }
}
