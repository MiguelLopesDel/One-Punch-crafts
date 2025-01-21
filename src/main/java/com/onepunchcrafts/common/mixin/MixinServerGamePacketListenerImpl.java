package com.onepunchcrafts.common.mixin;

import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl implements ServerPlayerConnection, TickablePacketListener, ServerGamePacketListener {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private boolean clientIsFloating;

    @Shadow
    public abstract void teleport(double pX, double pY, double pZ, float pYaw, float pPitch);

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clientIsFloating:Z"))
    public void tick(CallbackInfo ci) {
        HelpUtility.getSaitamaPack(this.player).ifPresent(sai -> this.clientIsFloating = false);
    }

    @Redirect(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V", ordinal = 2))
    public void teleport(ServerGamePacketListenerImpl instance, double pX, double pY, double pZ, float pYaw, float pPitch) {
        HelpUtility.getSaitamaPack(this.player).ifPresentOrElse(a -> {}, () -> instance.teleport(pX, pY, pZ, pYaw, pPitch));
    }
}
