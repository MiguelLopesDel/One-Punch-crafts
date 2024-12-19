package com.onepunchcrafts.common.skills;

import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public class WithoutPack implements SkillPack {

    @Override
    public void execute(ServerPlayer player) {

    }

    @Override
    public void readNBT(Tag tag) {

    }

    @Override
    public Tag writeNBT() {
        return new CompoundTag();
    }

    @Override
    public ArrayList<String> compareTo(SkillPack otherData) {
        return new ArrayList<>();
    }

    @Override
    public void setCurrentSkill(int currentSkill) {

    }

    @Override
    public int getCurrentSkill() {
        return 0;
    }

    @Override
    public int getMaxNumSkill() {
        return 0;
    }

    @Override
    public void syncVariables(SkillPack serverData) {

    }

    @Override
    public void renderSkills(int width, int height, Font font, GuiGraphics guiGraphics) {

    }

    @Override
    public void handleTheDifferences(ServerPlayer player, ArrayList<String> differences, SkillPack serverData, SkillPack clientData) {

    }

    @Override
    public void adjustAbility(ShortConsumer setter, short currentValue, double scrollDelta) {

    }
}
