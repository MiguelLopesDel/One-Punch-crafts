package com.onepunchcrafts.network;

import com.onepunchcrafts.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

public class NetworkRegister {

    private static int id = 0;
    private static final String PROTOCOL_VERSION = "1";
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
        INSTANCE.messageBuilder(SeriousPunchPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SeriousPunchPacket::encode)
                .decoder(SeriousPunchPacket::new)
                .consumerMainThread(SeriousPunchPacket::handle)
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
}
