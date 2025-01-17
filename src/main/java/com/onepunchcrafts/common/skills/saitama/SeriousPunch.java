package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import com.onepunchcrafts.util.TickUtilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import static com.onepunchcrafts.common.event.LivingDamageEventHandler.markBlocksToClear;

public class SeriousPunch implements Skill {

    @Override
    public void execute(Player p) {
        if (!(p.level() instanceof ServerLevel serverLevel) || !(p instanceof ServerPlayer player))
            return;
        HelpUtility.clientEffects(player);
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        Vec3 cylinderStartPos = playerPos.add(lookVec.scale(3));

        ArrayList<BlockPos> blockPos = markBlocksToClear(serverLevel, 15, 1000, (int) Math.floor(cylinderStartPos.x), (int) Math.floor(cylinderStartPos.y), (int) Math.floor(cylinderStartPos.z), lookVec);
        final TickUtilities tickU = new TickUtilities();
        TickScheduler.scheduleWithCondition(Duration.of(50, ChronoUnit.MILLIS), () -> tickU.fillCylinderAndEmuleEffects(player, serverLevel, 1000, blockPos));
    }

}
