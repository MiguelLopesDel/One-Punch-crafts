package com.onepunchcrafts.common.skills;

import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.event.LivingDamageEventHandler;
import com.onepunchcrafts.constant.NbtBooleanValues;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.onepunchcrafts.util.HelpUtility.syncDataWithServer;

public class SaitamaPack implements SkillPack {

    //    private boolean isSaitama;
    @Setter
    @Getter
    private boolean seriousFartActive;
    @Setter
    @Getter
    private short speed;
    @Setter
    @Getter
    private boolean breakBlocksQuickly;
    @Setter
    @Getter
    private short weight;
    @Setter
    @Getter
    private short knockbackResistance;
    @Setter
    @Getter
    private short attackKnockback;
    @Setter
    @Getter
    private short swimSpeed;
    private int currentSkill;

    @Override
    public void execute(ServerPlayer player) {
        switch (getCurrentSkill()) {
            case 2:
                LivingDamageEventHandler.seriousPunchWithoutSpecificTargetWithClientEffects(player, player.serverLevel());
                break;
            case 3:
                setSeriousFartActive(!isSeriousFartActive());
                NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(this));
                break;
            case 5:
                quickBackstab(player);
                break;
            case 7:
                setBreakBlocksQuickly(!isBreakBlocksQuickly());
                NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(this));
                break;
        }
    }

    @Override
    public Tag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean(NbtBooleanValues.seriousFartActive.getValue(), this.seriousFartActive);
        nbt.putBoolean(NbtBooleanValues.breakBlocksQuickly.getValue(), this.isBreakBlocksQuickly());
        nbt.putInt("actualability", this.currentSkill);
        nbt.putShort("weight", this.weight);
        nbt.putShort(NbtBooleanValues.superSpeed.getValue(), this.speed);
        nbt.putShort("saitamaknockbackresistance", this.knockbackResistance);
        nbt.putShort("saitamaattackknockback", this.attackKnockback);
        nbt.putShort("saitamaswimspeed", this.swimSpeed);
        return nbt;
    }

    @Override
    public void readNBT(Tag tag) {
        CompoundTag nbt = (CompoundTag) tag;
        this.seriousFartActive = nbt.getBoolean(NbtBooleanValues.seriousFartActive.getValue());
        this.breakBlocksQuickly = nbt.getBoolean(NbtBooleanValues.breakBlocksQuickly.getValue());
        this.currentSkill = nbt.getInt("actualability");
        this.weight = nbt.getShort("weight");
        this.speed = nbt.getShort(NbtBooleanValues.superSpeed.getValue());
        this.knockbackResistance = nbt.getShort("saitamaknockbackresistance");
        this.attackKnockback = nbt.getShort("saitamaattackknockback");
        this.swimSpeed = nbt.getShort("saitamaswimspeed");
    }

    @Override
    public ArrayList<String> compareTo(SkillPack otherData) {
        if (!(otherData instanceof SaitamaPack saitamaPack))
            return new ArrayList<>();
        ArrayList<String> changed = new ArrayList<>();
//        if (this.isSaitama != saitamaPack.isSaitama) {
//            changed.add(NbtBooleanValues.isSaitama.getValue());
//        }
        if (this.currentSkill != saitamaPack.currentSkill) {
            changed.add("actualability");
        }
        if (this.speed != saitamaPack.speed) {
            changed.add(NbtBooleanValues.superSpeed.getValue());
        }
        if (this.breakBlocksQuickly != saitamaPack.breakBlocksQuickly) {
            changed.add(NbtBooleanValues.breakBlocksQuickly.getValue());
        }
        if (this.getWeight() != saitamaPack.getWeight()) {
            changed.add("weight");
        }
        if (this.getKnockbackResistance() != saitamaPack.getKnockbackResistance()) {
            changed.add("saitamaknockbackresistance");
        }
        if (this.getAttackKnockback() != saitamaPack.getAttackKnockback()) {
            changed.add("saitamaattackknockback");
        }
        if (this.getSwimSpeed() != saitamaPack.getSwimSpeed()) {
            changed.add("saitamaswimspeed");
        }
        return changed;
    }

    @Override
    public void setCurrentSkill(int currentSkill) {
        this.currentSkill = (currentSkill > getMaxNumSkill()) ? 0 : (currentSkill < 0) ? getMaxNumSkill() : currentSkill;
    }

    @Override
    public int getCurrentSkill() {
        return currentSkill;
    }

    @Override
    public int getMaxNumSkill() {
        return 11;
    }

    @Override
    public void syncVariables(SkillPack serverData) {
        if (!(serverData instanceof SaitamaPack data))
            return;
        this.setCurrentSkill(data.getCurrentSkill());
        this.setSeriousFartActive(data.isSeriousFartActive());
        this.setSpeed(data.getSpeed());
        this.setBreakBlocksQuickly(data.isBreakBlocksQuickly());
        this.setWeight(data.getWeight());
        this.setKnockbackResistance(data.getKnockbackResistance());
        this.setAttackKnockback(data.getAttackKnockback());
        this.setSwimSpeed(data.getSwimSpeed());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderSkills(int width, int height, Font font, GuiGraphics guiGraphics) {
        int defaultReduce = (int) (width * 0.05);
        int defaultAdd = (int) (height * 0.25);
        switch (getCurrentSkill()) {
            case 0 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.weak_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 1 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.normal_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 2 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.serious_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 3 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.serious_fart"), width / 2 - defaultReduce, height / 2 + defaultAdd, isSeriousFartActive() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
            case 4 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.weakening_punch"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 5 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.quick_backstab"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 6 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.super_speed", getSpeed()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 7 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.break_blocks_quickly"), width / 2 - defaultReduce, height / 2 + defaultAdd, isBreakBlocksQuickly() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
            case 8 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.set_weight", getWeight()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 9 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.knockback_resistance", getKnockbackResistance()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 10 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.attack_knockback", getAttackKnockback()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 11 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.swim_speed", getSwimSpeed()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            case 12 ->
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.teleport", getSwimSpeed()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
        }
    }

    @Override
    public void handleTheDifferences(ServerPlayer player, ArrayList<String> differences, SkillPack serverLayer, SkillPack clientLayer) {
        if (!(serverLayer instanceof SaitamaPack serverData) || !(clientLayer instanceof SaitamaPack clientData))
            return;
        differences.forEach(item -> {
            switch (item) {
                case "actualability":
                    updateCurrentSkill(serverData, clientData);
                    break;
                case "seriousfart":
                    updateSeriousFart(player, serverData, clientData);
                    break;
                case "superspeedsaitama":
                    updateFieldWithValidation(serverData::setSpeed, clientData.getSpeed(), player, serverData);
                    break;
                case "breakblocksquickly":
                    updateBreakBlocksQuickly(player, serverData, clientData);
                    break;
                case "weight":
                    updateFieldWithValidation(serverData::setWeight, clientData.getWeight(), player, serverData);
                    break;
                case "saitamaknockbackresistance":
                    updateFieldWithValidation(serverData::setKnockbackResistance, clientData.getKnockbackResistance(), player, serverData);
                    break;
                case "saitamaattackknockback":
                    updateFieldWithValidation(serverData::setAttackKnockback, clientData.getAttackKnockback(), player, serverData);
                    break;
                case "saitamaswimspeed":
                    updateFieldWithValidation(serverData::setSwimSpeed, clientData.getSwimSpeed(), player, serverData);
                    break;
            }
        });
    }

    private void quickBackstab(ServerPlayer sender) {
        Vec3 startVec = sender.getEyePosition();
        int distance = 300;
        Vec3 lookVec = sender.getLookAngle().scale(distance);
        Vec3 endVec = startVec.add(lookVec);
        Level level = sender.level();
        HitResult hitResult = level.clip(new ClipContext(startVec, endVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, sender));
        if (hitResult.getType() == HitResult.Type.MISS) {
            endVec = hitResult.getLocation();
        }
        AABB boundingBox = new AABB(startVec, endVec).inflate(1.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> entity != sender && entity.isAlive());
        LivingEntity closestEntity = null;
        double closestDistance = distance * distance;
        for (LivingEntity entity : entities) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
            EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(sender, startVec, endVec, entityBox, entity1 -> !entity1.isSpectator() && entity1.isPickable(), closestDistance);
            if (entityHitResult != null) {
                double distanceToEntity = startVec.distanceToSqr(entityHitResult.getLocation());
                if (distanceToEntity < closestDistance) {
                    closestEntity = entity;
                    closestDistance = distanceToEntity;
                }
            }
        }
        if (closestEntity != null) {
            sender.teleportTo(closestEntity.getX(), closestEntity.getY(), closestEntity.getZ());
            setCurrentSkill(1);
            sender.attack(closestEntity);
            setCurrentSkill(5);
//            if (closestEntity instanceof ServerPlayer target)
//                NetworkRegister.sendToPlayer(target, new SettingRenderPacket("quick_backstab"));
        }
    }

    private static void updateBreakBlocksQuickly(ServerPlayer player, SaitamaPack serverData, SaitamaPack clientData) {
        serverData.setBreakBlocksQuickly(clientData.isBreakBlocksQuickly());
        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
    }

    private void updateFieldWithValidation(Consumer<Short> setter, short clientValue, ServerPlayer player, SaitamaPack serverData) {
        boolean isInvalid = clientValue < 0;
        setter.accept(isInvalid ? 0 : clientValue);
        if (isInvalid)
            NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
    }

    private static void updateSeriousFart(ServerPlayer player, SaitamaPack serverData, SaitamaPack clientData) {
        serverData.setSeriousFartActive(clientData.isSeriousFartActive());
        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
    }

    private static void updateCurrentSkill(SaitamaPack serverData, SaitamaPack clientData) {
        if (Math.abs(serverData.getCurrentSkill() - clientData.getCurrentSkill()) == 1)
            serverData.setCurrentSkill(clientData.getCurrentSkill());
        else if ((serverData.getCurrentSkill() == serverData.getMaxNumSkill() && clientData.getCurrentSkill() == 0) || (
                serverData.getCurrentSkill() == 0 && clientData.getCurrentSkill() == serverData.getMaxNumSkill()))
            serverData.setCurrentSkill(clientData.getCurrentSkill());
    }

    public void adjustAbility(ShortConsumer setter, short currentValue, double scrollDelta) {
        short newValue = (short) (currentValue + scrollDelta);
        setter.accept(newValue < 0 ? 0 : newValue);
    }
}
