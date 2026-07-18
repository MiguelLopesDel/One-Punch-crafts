package com.onepunchcrafts.network;

import com.onepunchcrafts.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

public class NetworkRegister {

    private static int id = 0;
    private static final String PROTOCOL_VERSION = "2";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    public static void registerMessages() {
        INSTANCE.messageBuilder(PlayerSyncPacket.class, ++id, null)
                .encoder(PlayerSyncPacket::encode)
                .decoder(PlayerSyncPacket::new)
                .consumerMainThread(PlayerSyncPacket::handle)
                .add();
        INSTANCE.messageBuilder(SpecialSkillPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpecialSkillPacket::encode)
                .decoder(SpecialSkillPacket::new)
                .consumerMainThread(SpecialSkillPacket::handle)
                .add();
        INSTANCE.messageBuilder(AnimationPacket.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AnimationPacket::encode)
                .decoder(AnimationPacket::new)
                .consumerMainThread(AnimationPacket::handle)
                .add();
        INSTANCE.messageBuilder(SeriousFartPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SeriousFartPacket::encode)
                .decoder(SeriousFartPacket::new)
                .consumerMainThread(SeriousFartPacket::handle)
                .add();
        INSTANCE.messageBuilder(TeleportPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TeleportPacket::encode)
                .decoder(TeleportPacket::new)
                .consumerMainThread(TeleportPacket::handle)
                .add();
        INSTANCE.messageBuilder(CheckAndDestructionBlockInAroundPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CheckAndDestructionBlockInAroundPacket::encode)
                .decoder(CheckAndDestructionBlockInAroundPacket::new)
                .consumerMainThread(CheckAndDestructionBlockInAroundPacket::handle)
                .add();
        INSTANCE.messageBuilder(MovementPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MovementPacket::encode)
                .decoder(MovementPacket::new)
                .consumerMainThread(MovementPacket::handle)
                .add();
        INSTANCE.messageBuilder(LevelSyncPacket.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LevelSyncPacket::encode)
                .decoder(LevelSyncPacket::new)
                .consumerMainThread(LevelSyncPacket::handle)
                .add();
        INSTANCE.messageBuilder(DimensionsPacket.class, ++id, null)
                .encoder(DimensionsPacket::encode)
                .decoder(DimensionsPacket::new)
                .consumerMainThread(DimensionsPacket::handle)
                .add();
        INSTANCE.messageBuilder(ScreenEffectPacket.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ScreenEffectPacket::encode)
                .decoder(ScreenEffectPacket::new)
                .consumerMainThread(ScreenEffectPacket::handle)
                .add();
        INSTANCE.messageBuilder(BorosMovementInputPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BorosMovementInputPacket::encode)
                .decoder(BorosMovementInputPacket::new)
                .consumerMainThread(BorosMovementInputPacket::handle)
                .add();
        INSTANCE.messageBuilder(BorosCsrcVfxPacket.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BorosCsrcVfxPacket::encode)
                .decoder(BorosCsrcVfxPacket::new)
                .consumerMainThread(BorosCsrcVfxPacket::handle)
                .add();
        INSTANCE.messageBuilder(BorosBeamVfxPacket.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BorosBeamVfxPacket::encode)
                .decoder(BorosBeamVfxPacket::new)
                .consumerMainThread(BorosBeamVfxPacket::handle)
                .add();
        INSTANCE.messageBuilder(SaitamaVfxPacket.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SaitamaVfxPacket::encode)
                .decoder(SaitamaVfxPacket::new)
                .consumerMainThread(SaitamaVfxPacket::handle)
                .add();
        INSTANCE.messageBuilder(SeriousPunchVfxPacket.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SeriousPunchVfxPacket::encode)
                .decoder(SeriousPunchVfxPacket::new)
                .consumerMainThread(SeriousPunchVfxPacket::handle)
                .add();
        INSTANCE.messageBuilder(PowerStateSnapshotPacket.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PowerStateSnapshotPacket::encode)
                .decoder(PowerStateSnapshotPacket::new)
                .consumerMainThread(PowerStateSnapshotPacket::handle)
                .add();
        INSTANCE.messageBuilder(SelectTechniqueIntentPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectTechniqueIntentPacket::encode)
                .decoder(SelectTechniqueIntentPacket::new)
                .consumerMainThread(SelectTechniqueIntentPacket::handle)
                .add();
        INSTANCE.messageBuilder(SwapTechniqueIntentPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SwapTechniqueIntentPacket::encode)
                .decoder(SwapTechniqueIntentPacket::new)
                .consumerMainThread(SwapTechniqueIntentPacket::handle)
                .add();
        INSTANCE.messageBuilder(ActivateTechniqueIntentPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivateTechniqueIntentPacket::encode)
                .decoder(ActivateTechniqueIntentPacket::new)
                .consumerMainThread(ActivateTechniqueIntentPacket::handle)
                .add();
        INSTANCE.messageBuilder(PowerComponentDeltaPacket.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PowerComponentDeltaPacket::encode)
                .decoder(PowerComponentDeltaPacket::new)
                .consumerMainThread(PowerComponentDeltaPacket::handle)
                .add();
        INSTANCE.messageBuilder(AdjustTechniqueIntentPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AdjustTechniqueIntentPacket::encode)
                .decoder(AdjustTechniqueIntentPacket::new)
                .consumerMainThread(AdjustTechniqueIntentPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg) {
        INSTANCE.sendToServer(msg);
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAllClients(Object msg) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }

    /** Send to every client of the level within {@code range} blocks of {@code pos}. */
    public static void sendToNearby(ServerLevel level, Vec3 pos, double range, Object msg) {
        double rangeSqr = range * range;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && player.distanceToSqr(pos.x, pos.y, pos.z) <= rangeSqr) {
                sendToPlayer(player, msg);
            }
        }
    }

    public static void sendToAllClientsExcept(ServerPlayer player, Object msg) {
        for (ServerPlayer player1 : player.getServer().getPlayerList().getPlayers()) {
            if (!player.equals(player1))
                sendToPlayer(player1, msg);
        }
    }
}
