package com.onepunchcrafts.common.skills;

import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;

public class WithoutPack implements SkillPack {

    @Override
    public void execute(Player player) {

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
    public Skill getCurrentSkill() {
        return new Skill() {
            @Override
            public void execute(Player player) {}
            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {}
        };
    }

    @Override
    public int getCurrentSkillIndex() {
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

    @Override
    public void tick(TickEvent.PlayerTickEvent player) {

    }

    @Override
    public void nextOrPrevious(int i) {

    }

    @Override
    public void manageFlux(LivingEvent event) {

    }

    @Override
    public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {

    }
}
