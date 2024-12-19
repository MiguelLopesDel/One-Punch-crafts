package com.onepunchcrafts.common.skills;

import com.onepunchcrafts.common.capability.OnePunchPlayer;
import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

public interface SkillPack {

    void execute(ServerPlayer player);

    void readNBT(Tag tag);

    Tag writeNBT();

    /**
     * Método que compara os campos com outro objeto e retorna um Map contendo as diferenças a chave é o valor original
     *
     * @param otherData
     * @return
     */
    ArrayList<String> compareTo(SkillPack otherData);

    void setCurrentSkill(int currentSkill);

    int getCurrentSkill();

    int getMaxNumSkill();

    void syncVariables(SkillPack serverData);

    void renderSkills(int width, int height, Font font, GuiGraphics guiGraphics);

    void handleTheDifferences(ServerPlayer player, ArrayList<String> differences, SkillPack serverData, SkillPack clientData);

    void adjustAbility(ShortConsumer setter, short currentValue, double scrollDelta);
}
