package com.onepunchcrafts.network;

import com.onepunchcrafts.network.packet.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.*;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

public class NetworkRegister {

    private static int id = 0;
    private static final String PROTOCOL_VERSION = "1";
    //    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, "main"),
//            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
//    );
    private static final SimpleChannel INSTANCE = createChannel(ResourceLocation.fromNamespaceAndPath(MODID, "main"));

    protected static SimpleChannel createChannel(ResourceLocation name) {
        ChannelBuilder channelBuilder = ChannelBuilder.named(name);
        return channelBuilder.simpleChannel();
    }

    protected static SimpleChannel getChannel() {
        return INSTANCE;
    }

    protected static <MSG> void registerClientToServer(Class<MSG> type, BiConsumer<MSG, RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf, MSG> decoder, BiConsumer<MSG, CustomPayloadEvent.Context> consumer) {
        getChannel().messageBuilder(type, NetworkDirection.PLAY_TO_SERVER).encoder(encoder).decoder(decoder).consumerMainThread(consumer).add();
    }

    protected static <MSG> void registerServerToClient(Class<MSG> type, BiConsumer<MSG, RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf, MSG> decoder, BiConsumer<MSG, CustomPayloadEvent.Context> consumer) {
        getChannel().messageBuilder(type, NetworkDirection.PLAY_TO_CLIENT).encoder(encoder).decoder(decoder).consumerMainThread(consumer).add();
    }

    public static void registerMessages() {
        registerServerToClient(DimensionsPacket.class,
                DimensionsPacket::encode,
                DimensionsPacket::new,
                DimensionsPacket::handle);
        registerClientToServer(DimensionsPacket.class,
                DimensionsPacket::encode,
                DimensionsPacket::new,
                DimensionsPacket::handle);
        registerServerToClient(LevelSyncPacket.class,
                LevelSyncPacket::encode,
                LevelSyncPacket::new,
                LevelSyncPacket::handle);
        registerClientToServer(LevelSyncPacket.class,
                LevelSyncPacket::encode,
                LevelSyncPacket::new,
                LevelSyncPacket::handle);
        registerClientToServer(PlayerSyncPacket.class,
                PlayerSyncPacket::encode,
                PlayerSyncPacket::new,
                PlayerSyncPacket::handle);
        registerServerToClient(PlayerSyncPacket.class,
                PlayerSyncPacket::encode,
                PlayerSyncPacket::new,
                PlayerSyncPacket::handle);
        registerServerToClient(AnimationPacket.class,
                AnimationPacket::encode,
                AnimationPacket::new,
                AnimationPacket::handle);
        registerClientToServer(SpecialSkillPacket.class,
                SpecialSkillPacket::encode,
                SpecialSkillPacket::new,
                SpecialSkillPacket::handle);
        registerClientToServer(SeriousFartPacket.class,
                SeriousFartPacket::encode,
                SeriousFartPacket::new,
                SeriousFartPacket::handle);

        registerClientToServer(TeleportPacket.class,
                TeleportPacket::encode,
                TeleportPacket::new,
                TeleportPacket::handle);
        registerClientToServer(CheckAndDestructionBlockInAroundPacket.class,
                CheckAndDestructionBlockInAroundPacket::encode,
                CheckAndDestructionBlockInAroundPacket::new,
                CheckAndDestructionBlockInAroundPacket::handle);
        registerClientToServer(MovementPacket.class,
                MovementPacket::encode,
                MovementPacket::new,
                MovementPacket::handle);
    }

    public static void sendToServer(Object msg) {
        getChannel().send(msg, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        getChannel().send(msg, player.connection.getConnection());
    }

    public static void sendToAllClients(Object msg) {
        getChannel().send(msg, PacketDistributor.ALL.noArg());
    }

    public static void sendToAllClientsExcept(ServerPlayer player, Object msg) {
        for (ServerPlayer player1 : player.getServer().getPlayerList().getPlayers()) {
            if (!player.equals(player1))
                sendToPlayer(player1, msg);
        }
    }
}
