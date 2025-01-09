package com.onepunchcrafts.common.capability;

import com.onepunchcrafts.common.skills.SkillPack;
import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

import static com.onepunchcrafts.util.HelpUtility.syncDataWithServer;

@Data
public class OnePunchPlayer {

    @NonNull
    @Setter
    private SkillPack skillPack;

    public OnePunchPlayer(@NotNull SkillPack skillPack) {
        this.skillPack = skillPack;
    }

    public Tag writeNBT() {
        return skillPack.writeNBT();
    }

    public void readNBT(Tag tag) {
        skillPack.readNBT(tag);
    }

    /**
     * Método que compara os campos com outro objeto e retorna um Map contendo as diferenças a chave é o valor original
     *
     * @param otherData
     * @return
     */
    public ArrayList<String> compareTo(SkillPack otherData) {
        return skillPack.compareTo(otherData);
    }

    public void setCurrentSkill(int currentSkill) {
        this.skillPack.setCurrentSkill(currentSkill);
    }

    public void decideCurrentSkill(LocalPlayer player) {
        setCurrentSkill(getActualAbility() + (player.isShiftKeyDown() ? -1 : 1));
    }

    public void adjustAbilityAndSyncWithServer(ShortConsumer setter, short currentValue, double scrollDelta) {
        getSkillPack().adjustAbility(setter, currentValue, scrollDelta);
        syncDataWithServer(this);
    }

    public int getMaxNumSkill() {
        return this.skillPack.getMaxNumSkill();
    }

    public int getActualAbility() {
        return this.skillPack.getCurrentSkill();
    }
}
