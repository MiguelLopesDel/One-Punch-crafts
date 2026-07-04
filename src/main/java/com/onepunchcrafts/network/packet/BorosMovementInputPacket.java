package com.onepunchcrafts.network.packet;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.skills.boros.BorosPack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BorosMovementInputPacket {
    private final boolean forward;
    private final boolean backward;
    private final boolean left;
    private final boolean right;
    private final boolean jump;
    private final boolean sneak;
    private final boolean sprint;

    public BorosMovementInputPacket(boolean forward, boolean backward, boolean left, boolean right,
                                    boolean jump, boolean sneak, boolean sprint) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.sneak = sneak;
        this.sprint = sprint;
    }

    public BorosMovementInputPacket(FriendlyByteBuf buffer) {
        this.forward = buffer.readBoolean();
        this.backward = buffer.readBoolean();
        this.left = buffer.readBoolean();
        this.right = buffer.readBoolean();
        this.jump = buffer.readBoolean();
        this.sneak = buffer.readBoolean();
        this.sprint = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(forward);
        buffer.writeBoolean(backward);
        buffer.writeBoolean(left);
        buffer.writeBoolean(right);
        buffer.writeBoolean(jump);
        buffer.writeBoolean(sneak);
        buffer.writeBoolean(sprint);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            sender.getCapability(OnePunchCrafts.ONE_PLAYER_CAPABILITY).ifPresent(cap -> {
                if (cap.getSkillPack() instanceof BorosPack boros) {
                    boros.setMovementInput(forward, backward, left, right, jump, sneak, sprint);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
