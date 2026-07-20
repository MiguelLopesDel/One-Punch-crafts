package com.onepunchcrafts.common.dimension;

import com.onepunchcrafts.common.block.entity.PortalBlockEntity;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.ImmersivePortalsCompat;
import com.onepunchcrafts.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.onepunchcrafts.OnePunchCrafts.IMMERSIVE_PORTALS_MOD;
import static com.onepunchcrafts.OnePunchCrafts.PORTAL_BLOCK;

/**
 * Owns every portal torn open by a Dimensional Punch. A portal stays open for
 * anyone to cross, but closes the moment its own opener steps through it, and
 * always closes on its own after five minutes.
 */
@Mod.EventBusSubscriber
public final class DimensionalPortalManager {

    /** Fist tears space, then the portal settles a beat later. */
    public static final int WINDUP_TICKS = 12;
    private static final int LIFETIME_TICKS = 6000; // 5 minutes
    private static final double OWNER_REACH_SQR = 6.25; // 2.5 blocks

    private static final List<Portal> PORTALS = new ArrayList<>();

    private DimensionalPortalManager() {}

    /** Throw the punch: a short windup, then space breaks and the portal opens. */
    public static void punch(ServerPlayer player, ResourceKey<Level> dimension) {
        ServerLevel level = player.serverLevel();
        Vec3 fist = player.getEyePosition().add(player.getLookAngle().scale(1.2));
        DimensionalPunchPresentation.begin(player, fist, player.getLookAngle(), WINDUP_TICKS);

        UUID owner = player.getUUID();
        ResourceKey<Level> originDim = level.dimension();
        TickScheduler.scheduleFromHere(Duration.of(WINDUP_TICKS * 50L, ChronoUnit.MILLIS),
                () -> open(player, dimension, owner, originDim));
    }

    private static void open(ServerPlayer player, ResourceKey<Level> dimension,
                             UUID owner, ResourceKey<Level> originDim) {
        if (player.isRemoved() || !player.isAlive() || !player.serverLevel().dimension().equals(originDim)) return;
        ServerLevel level = player.serverLevel();
        Vec3 fist = player.getEyePosition().add(player.getLookAngle().scale(1.2));
        DimensionalPunchPresentation.impact(player, fist, player.getLookAngle());
        level.playSound(null, fist.x, fist.y, fist.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.1f, 0.7f);
        level.playSound(null, fist.x, fist.y, fist.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 0.55f);

        if (IMMERSIVE_PORTALS_MOD.isPresent()) {
            Vec3 pos = ImmersivePortalsCompat.openTrackedPortal(player, dimension);
            PORTALS.add(new Portal(owner, originDim, dimension, pos, true, null));
        } else {
            BlockPos pos = HelpUtility.getFrontBlockPosition(player, 2);
            level.setBlockAndUpdate(pos, PORTAL_BLOCK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof PortalBlockEntity blockEntity)
                blockEntity.setDimension(dimension);
            PORTALS.add(new Portal(owner, originDim, dimension, Vec3.atCenterOf(pos), false, pos));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PORTALS.isEmpty()) return;
        MinecraftServer server = event.getServer();
        Iterator<Portal> iterator = PORTALS.iterator();
        while (iterator.hasNext()) {
            Portal portal = iterator.next();
            boolean close = --portal.ticksLeft <= 0;
            if (!close) {
                ServerPlayer owner = server.getPlayerList().getPlayer(portal.owner);
                if (owner != null) {
                    boolean inOrigin = owner.level().dimension().equals(portal.originDim);
                    if (inOrigin && owner.position().distanceToSqr(portal.pos) < OWNER_REACH_SQR) portal.armed = true;
                    // Armed then no longer in the origin dimension = the opener stepped through.
                    if (portal.armed && !inOrigin) close = true;
                }
            }
            if (close) {
                closePortal(server, portal);
                iterator.remove();
            }
        }
    }

    private static void closePortal(MinecraftServer server, Portal portal) {
        ServerLevel origin = server.getLevel(portal.originDim);
        if (origin == null) return;
        origin.sendParticles(ParticleTypes.REVERSE_PORTAL, portal.pos.x, portal.pos.y, portal.pos.z,
                40, 0.6, 1.0, 0.6, 0.1);
        origin.playSound(null, portal.pos.x, portal.pos.y, portal.pos.z, SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.8f, 1.4f);
        if (portal.ip) {
            ImmersivePortalsCompat.closePortalsAt(origin, portal.pos, 3.5);
        } else if (portal.blockPos != null && origin.getBlockState(portal.blockPos).getBlock() == PORTAL_BLOCK.get()) {
            origin.setBlockAndUpdate(portal.blockPos, Blocks.AIR.defaultBlockState());
        }
    }

    private static final class Portal {
        final UUID owner;
        final ResourceKey<Level> originDim;
        final ResourceKey<Level> destDim;
        final Vec3 pos;
        final boolean ip;
        final BlockPos blockPos;
        int ticksLeft = LIFETIME_TICKS;
        boolean armed;
        Portal(UUID owner, ResourceKey<Level> originDim, ResourceKey<Level> destDim,
               Vec3 pos, boolean ip, BlockPos blockPos) {
            this.owner = owner;
            this.originDim = originDim;
            this.destDim = destDim;
            this.pos = pos;
            this.ip = ip;
            this.blockPos = blockPos;
        }
    }
}
