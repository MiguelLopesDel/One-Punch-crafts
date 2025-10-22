package com.onepunchcrafts.common.skills;

import com.onepunchcrafts.common.skills.sync.SyncStrategy;
import com.onepunchcrafts.common.skills.sync.Syncable;
import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractSkillPack implements SkillPack {
    @Getter
    @Syncable(key = "currentSkillIndex", strategy = SyncStrategy.SKILL_INDEX)
    protected int currentSkillIndex;
    @Getter
    @Syncable(key = "currentGroupIndex", strategy = SyncStrategy.GROUP_INDEX)
    protected int currentGroupIndex;
    protected final List<List<Skill>> skills;

    public AbstractSkillPack() {
        this.skills = initializeSkills();
    }

    @NotNull
    protected abstract List<List<Skill>> initializeSkills();

    @Override
    public void execute(Player player) {
        Skill skill = getCurrentSkill();
        if (skill != null) {
            skill.execute(player);
        }
    }

    @Override
    public void renderSkills(int width, int height, Font font, GuiGraphics guiGraphics) {
        getCurrentSkill().renderName(width, height, font, guiGraphics, (int) (width * 0.05), (int) (height * 0.25));
    }

    @Override
    public Skill getCurrentSkill() {
        if (skills.isEmpty() || currentGroupIndex >= skills.size() || skills.get(currentGroupIndex).isEmpty()) {
            return null;
        }
        List<Skill> currentGroup = skills.get(currentGroupIndex);
        int index = getCurrentSkillIndex() < currentGroup.size() ? getCurrentSkillIndex() : 0;
        this.currentSkillIndex = index;
        return currentGroup.get(index);
    }

    @Override
    public void setCurrentSkill(int currentSkill) {
        int lastSkill = skills.get(currentGroupIndex).size() - 1;
        this.currentSkillIndex = (currentSkill > lastSkill) ? 0 : (currentSkill < 0) ? lastSkill : currentSkill;
    }

    @Override
    public void nextOrPrevious(int i) {
        int size = skills.size();
        if (size > 0) {
            currentGroupIndex = (currentGroupIndex + i % size + size) % size;
            setCurrentSkill(0);
        }
    }

    @Override
    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            skills.forEach(group -> group.forEach(skill -> {
                if (skill instanceof SkillPassive passiveSkill) {
                    passiveSkill.tick(event.player);
                }
            }));
        }
    }

    @Override
    public Tag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("skillPack", this.getClass().getSimpleName());
        nbt.putInt("currentSkillIndex", this.currentSkillIndex);
        nbt.putInt("currentGroupIndex", this.currentGroupIndex);
        return nbt;
    }

    @Override
    public void readNBT(Tag tag) {
        if (tag instanceof CompoundTag nbt) {
            this.currentSkillIndex = nbt.getInt("currentSkillIndex");
            this.currentGroupIndex = nbt.getInt("currentGroupIndex");
        }
    }
}
