package com.onepunchcrafts.common.skills;

import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;

public interface SkillPack {

    void execute(Player player);

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

    Skill getCurrentSkill();
    int getCurrentSkillIndex();

    /**
     * Isso é equivalente ao index maximo da lista e não a quantidade de itens nela
     * @return
     */
    int getMaxNumSkill();

    void syncVariables(SkillPack serverData);
    @OnlyIn(Dist.CLIENT)
    void renderSkills(int width, int height, Font font, GuiGraphics guiGraphics);

    void handleTheDifferences(ServerPlayer player, ArrayList<String> differences, SkillPack serverData, SkillPack clientData);

    void adjustAbility(double scrollDelta);

    void tick(TickEvent.PlayerTickEvent event);

    void nextOrPrevious(int i);

    void manageFlux(LivingEvent event);

    void playerRespawn(PlayerEvent.PlayerRespawnEvent event);
}
