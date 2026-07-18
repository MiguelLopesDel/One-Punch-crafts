package com.onepunchcrafts.common.mixin;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.content.SaitamaContent;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl implements ServerPlayerConnection, TickablePacketListener, ServerGamePacketListener {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private boolean clientIsFloating;

    @Shadow
    public abstract boolean isPlayerCollidingWithAnythingNew(LevelReader pLevel, AABB pBox, double pX, double pY, double pZ);

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clientIsFloating:Z"))
    public void tick(CallbackInfo ci) {
        if (HelpUtility.isV3Saitama(this.player)) this.clientIsFloating = false;
        else HelpUtility.getSaitamaPack(this.player).ifPresent(sai -> this.clientIsFloating = false);
    }

    @Redirect(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isChangingDimension()Z", ordinal = 0))
    public boolean isChangingDimension(ServerPlayer player) {
        if (HelpUtility.isV3Saitama(this.player)) return true;
        return HelpUtility.getSaitamaPack(this.player)
                .map(p -> true)
                .orElseGet(player::isChangingDimension);
    }

    @ModifyVariable(method = "handleMovePlayer", at = @At("STORE"), ordinal = 2)
    public boolean modifyFlag2(boolean flag2) {
        if (HelpUtility.hasV3Tag(this.player, SaitamaContent.TAG_EXTREME_SPEED) || HelpUtility.verifyIsSaitamaAndGetCapability(this.player)
                .map(cap -> cap.isExtremeSpeedActive())
                .orElse(false)) {
            return false;
        }
        return flag2;
    }

    @Redirect(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isPlayerCollidingWithAnythingNew(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/phys/AABB;DDD)Z"))
    public boolean isPlayerCollidingWithAnythingNew(ServerGamePacketListenerImpl instance, LevelReader levelReader, AABB pLevel, double pBox, double pX, double pY) {
        if (HelpUtility.hasV3Tag(this.player, SaitamaContent.TAG_EXTREME_SPEED) || HelpUtility.verifyIsSaitamaAndGetCapability(this.player)
                .map(cap -> cap.isExtremeSpeedActive())
                .orElse(false)) {
            return false;
        }

        return this.isPlayerCollidingWithAnythingNew(levelReader, pLevel, pBox, pX, pY);
    }

}
