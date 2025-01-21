package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.common.skills.SkillPassive;
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
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class SaitamaPack implements SkillPack {

    @Setter
    @Getter
    private boolean seriousFartActive;
    @Setter
    @Getter
    private boolean extremeSpeedActive;
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
    private final List<List<Skill>> skills = getSkills();
    @Getter
    private int currentGroupIndex;

    private @NotNull List<List<Skill>> getSkills() {
        List<List<Skill>> groupList = new ArrayList<>();
        List<Skill> list = new ArrayList<>();
        list.add(0, new WeakPunch());
        list.add(1, new NormalPunche());
        list.add(2, new SeriousPunch());
        list.add(3, p -> {
            if (p instanceof ServerPlayer player) {
                setSeriousFartActive(!isSeriousFartActive());
                NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(this));
            }
        });
        list.add(new WeakeningPunch());
        list.add(5, new QuickBackStab());
        list.add(a -> {
        });
        list.add(7, p -> {
            if (p instanceof ServerPlayer player) {
                setBreakBlocksQuickly(!isBreakBlocksQuickly());
                NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(this));
            }
        });
        list.add(a -> {
        });
        list.add(a -> {
        });
        list.add(a -> {
        });
        list.add(a -> {
        });
        list.add(12, new NormalPunchesInArea());

        List<Skill> list2 = new ArrayList<>();
        list2.add(new ExtremeSpeed());

        groupList.add(list);
        groupList.add(list2);
        return groupList;
    }

    @Override
    public void execute(Player player) {
        Skill currentSkill1 = getCurrentSkill();
        if (currentSkill1 != null)
            currentSkill1.execute(player);
    }

    @Override
    public void manageFlux(LivingEvent event) {
        packFlux(event);
        getCurrentSkill().flux(event);
    }

    private void packFlux(LivingEvent event) {
        if (event instanceof LivingDamageEvent damageEvent && damageEvent.getEntity() instanceof ServerPlayer) {
            event.setCanceled(true);
        }
    }

    @Override
    public void nextOrPrevious(int i) {
        int size = skills.size();
        currentGroupIndex = (currentGroupIndex + i % size + size) % size;
        setCurrentSkill(0);
    }

    @Override
    public Tag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("skillPack", this.getClass().getSimpleName());
        nbt.putBoolean(NbtBooleanValues.seriousFartActive.getValue(), this.seriousFartActive);
        nbt.putBoolean(NbtBooleanValues.breakBlocksQuickly.getValue(), this.isBreakBlocksQuickly());
        nbt.putBoolean(NbtBooleanValues.extremeSpeed.getValue(), this.isExtremeSpeedActive());
        nbt.putInt("actualability", this.currentSkill);
        nbt.putShort("weight", this.weight);
        nbt.putShort(NbtBooleanValues.superSpeed.getValue(), this.speed);
        nbt.putShort("saitamaknockbackresistance", this.knockbackResistance);
        nbt.putShort("saitamaattackknockback", this.attackKnockback);
        nbt.putShort("saitamaswimspeed", this.swimSpeed);
        nbt.putInt("currentgroup", this.currentGroupIndex);
        return nbt;
    }

    @Override
    public void readNBT(Tag tag) {
        CompoundTag nbt = (CompoundTag) tag;
        this.seriousFartActive = nbt.getBoolean(NbtBooleanValues.seriousFartActive.getValue());
        this.breakBlocksQuickly = nbt.getBoolean(NbtBooleanValues.breakBlocksQuickly.getValue());
        this.extremeSpeedActive = nbt.getBoolean(NbtBooleanValues.extremeSpeed.getValue());
        this.currentSkill = nbt.getInt("actualability");
        this.weight = nbt.getShort("weight");
        this.speed = nbt.getShort(NbtBooleanValues.superSpeed.getValue());
        this.knockbackResistance = nbt.getShort("saitamaknockbackresistance");
        this.attackKnockback = nbt.getShort("saitamaattackknockback");
        this.swimSpeed = nbt.getShort("saitamaswimspeed");
        this.currentGroupIndex = nbt.getInt("currentgroup");
    }

    @Override
    public void setCurrentSkill(int currentSkill) {
        this.currentSkill = (currentSkill > getLastSkill()) ? 0 : (currentSkill < 0) ? getLastSkill() : currentSkill;
    }

    private int getLastSkill() {
        return skills.get(getCurrentGroupIndex()).size() - 1;
    }

    @Override
    public Skill getCurrentSkill() {
        int index = getCurrentSkillIndex() < skills.get(getCurrentGroupIndex()).size() ? getCurrentSkillIndex() : 0;
        this.currentSkill = index;
        return skills.get(this.currentGroupIndex).get(index);
    }

    @Override
    public int getCurrentSkillIndex() {
        return currentSkill;
    }

    @Override
    public int getMaxNumSkill() {
        return 12;
    }

    @Override
    public void syncVariables(SkillPack serverData) {
        if (!(serverData instanceof SaitamaPack data))
            return;
        this.setCurrentSkill(data.getCurrentSkillIndex());
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
        if (getCurrentGroupIndex() == 1) {
            switch (getCurrentSkillIndex()) {
                case 0 ->
                        guiGraphics.drawString(font, Component.translatable("skill.saitama.extreme_speed"), width / 2 - defaultReduce, height / 2 + defaultAdd, isExtremeSpeedActive() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
            }
            return;
        }

        switch (getCurrentSkillIndex()) {
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
                    guiGraphics.drawString(font, Component.translatable("skill.saitama.normalpuncharmy"), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
        }
    }

    @Override
    public ArrayList<String> compareTo(SkillPack otherData) {
        if (!(otherData instanceof SaitamaPack saitamaPack))
            return new ArrayList<>();
        ArrayList<String> changed = new ArrayList<>();
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
        if (this.currentGroupIndex != saitamaPack.currentGroupIndex) {
            changed.add("currentgroup");
        }
        return changed;
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
                case "currentgroup":
                    updateCurrentGroup(serverData, clientData);
                    break;
            }
        });
    }

    private void updateCurrentGroup(SaitamaPack serverData, SaitamaPack clientData) {
        int diff = Math.abs(serverData.currentGroupIndex - clientData.currentGroupIndex);
        if (diff == 1 || diff == serverData.skills.size() - 1)
            serverData.currentGroupIndex = clientData.currentGroupIndex;
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
        int diff = Math.abs(serverData.currentSkill - clientData.currentSkill);
        boolean b = diff == 1 || diff == serverData.getLastSkill();
        if (b || serverData.getCurrentGroupIndex() != clientData.getCurrentGroupIndex())
            serverData.currentSkill = clientData.currentSkill;
    }

    public void adjustAbility(ShortConsumer setter, short currentValue, double scrollDelta) {
        short newValue = (short) (currentValue + scrollDelta);
        setter.accept(newValue < 0 ? 0 : newValue);
    }

    @Override
    public void tick(TickEvent.PlayerTickEvent event) {
        skills.forEach(p -> p.forEach(sk -> {
            if (sk instanceof SkillPassive currentSkill1)
                currentSkill1.tick(event.player);
        }));
    }
}
