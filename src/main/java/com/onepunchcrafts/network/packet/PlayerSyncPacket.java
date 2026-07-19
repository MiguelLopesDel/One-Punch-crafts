package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.client.packet.HandlerClientPacket;
import com.onepunchcrafts.common.skills.boros.BorosPack;
import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.common.skills.SkillPack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.function.Supplier;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;
import static com.onepunchcrafts.OnePunchCrafts.WITHOUT_PACK;

public class PlayerSyncPacket {

    private final SkillPack data;

    public PlayerSyncPacket(final SkillPack data) {
        this.data = data;
    }

    public void encode(FriendlyByteBuf buffer) {
        String name = data.getClass().getSimpleName();
        buffer.writeInt(name.length());
        buffer.writeCharSequence(name, Charset.defaultCharset());
        buffer.writeNbt((CompoundTag) data.writeNBT());
    }

    public PlayerSyncPacket(final FriendlyByteBuf buffer) {
        int i = buffer.readInt();
        CharSequence charSequence = buffer.readCharSequence(i, Charset.defaultCharset());
        SkillPack skillPack = switch (charSequence.toString()) {
            case "SaitamaPack" -> new SaitamaPack();
            case "BorosPack" -> new BorosPack();
            default -> WITHOUT_PACK;
        };
        (this.data = skillPack).readNBT(buffer.readNbt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> serverLogic(ctx));
        } else {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> HandlerClientPacket.playerSyncLogic(data)));
        }
        context.setPacketHandled(true);
    }

    private void serverLogic(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null)
            return;
        player.getCapability(ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
            ArrayList<String> differences = cap.compareTo(data);
            cap.getSkillPack().handleTheDifferences(player, differences, cap.getSkillPack(), data);
        });
    }
}
