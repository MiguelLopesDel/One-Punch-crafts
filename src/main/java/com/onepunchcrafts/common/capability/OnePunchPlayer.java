package com.onepunchcrafts.common.capability;

import com.onepunchcrafts.constant.NbtBooleanValues;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.*;

@Data
public class OnePunchPlayer {

    private boolean isSaitama;
    @Setter
    @Getter
    private boolean seriousFartActive;
    @Setter
    @Getter
    private boolean superSpeed;
    @Setter
    @Getter
    private boolean breakBlocksQuickly;
    private int actualAbility;

    public OnePunchPlayer(boolean isSaitama) {
        this.isSaitama = isSaitama;
    }

    public Tag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean(NbtBooleanValues.isSaitama.getValue(), this.isSaitama);
        nbt.putBoolean(NbtBooleanValues.seriousFartActive.getValue(), this.seriousFartActive);
        nbt.putBoolean(NbtBooleanValues.superSpeed.getValue(), this.isSuperSpeed());
        nbt.putBoolean(NbtBooleanValues.breakBlocksQuickly.getValue(), this.isBreakBlocksQuickly());
        nbt.putInt("actualability", this.actualAbility);
        return nbt;
    }

    public void readNBT(Tag tag) {
        CompoundTag nbt = (CompoundTag) tag;
        this.isSaitama = nbt.getBoolean(NbtBooleanValues.isSaitama.getValue());
        this.seriousFartActive = nbt.getBoolean(NbtBooleanValues.seriousFartActive.getValue());
        this.superSpeed = nbt.getBoolean(NbtBooleanValues.superSpeed.getValue());
        this.breakBlocksQuickly = nbt.getBoolean(NbtBooleanValues.breakBlocksQuickly.getValue());
        this.actualAbility = nbt.getInt("actualability");
    }

    /**
     * Método que compara os campos com outro objeto e retorna um Map contendo as diferenças a chave é o valor original
     *
     * @param otherData
     * @return
     */
    public ArrayList<String> compareTo(OnePunchPlayer otherData) {
        ArrayList<String> changed = new ArrayList<>();
        if (this.isSaitama != otherData.isSaitama) {
            changed.add(NbtBooleanValues.isSaitama.getValue());
        }
        if (this.actualAbility != otherData.actualAbility) {
            changed.add("actualability");
        }
        if (this.superSpeed != otherData.superSpeed) {
            changed.add(NbtBooleanValues.superSpeed.getValue());
        }
        if (this.breakBlocksQuickly != otherData.breakBlocksQuickly) {
            changed.add(NbtBooleanValues.breakBlocksQuickly.getValue());
        }
        return changed;
    }

    public void setActualAbility(int actualAbility) {
        if (actualAbility > 7 || actualAbility < 0)
            return;
        this.actualAbility = actualAbility;
    }
}
