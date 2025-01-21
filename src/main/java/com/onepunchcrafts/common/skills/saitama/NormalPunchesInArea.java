package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.List;

public class NormalPunchesInArea implements Skill {

    @Override
    public void execute(Player p) {
        if (!(p instanceof ServerPlayer player))
            return;
        Level level = player.level();
        final int i = 50;
        BlockPos pos = player.blockPosition();
        AABB inflate = new AABB(pos).inflate(i, 3, i);
        AABB boundingBox = inflate.setMaxY(inflate.maxY + i);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> !entity.equals(player) && entity.isAlive());
        final ArrayDeque<Entity> deque = new ArrayDeque<>();
        entities.forEach(deque::push);
        processEntities(deque, player, HelpUtility.verifyIsSaitamaAndGetCapability(player).get());
    }

    private void processEntities(ArrayDeque<Entity> stack, ServerPlayer player, SaitamaPack sai) {
        if (stack.isEmpty()) {
            return;
        }
        Entity entity = stack.pop();
        TickScheduler.scheduleFromHereWithLastExecution(Duration.of(250, ChronoUnit.MILLIS), () -> {
            player.teleportTo(entity.getX(), entity.getY(), entity.getZ());
            sai.setCurrentSkill(0);
            player.attack(entity);
            sai.setCurrentSkill(12);
            processEntities(stack, player, HelpUtility.verifyIsSaitamaAndGetCapability(player).get());
        }, () -> HelpUtility.syncWithPlayer(player, HelpUtility.getSkillData(player)));
    }
}
