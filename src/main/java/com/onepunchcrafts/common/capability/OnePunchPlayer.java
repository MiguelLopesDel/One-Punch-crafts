package com.onepunchcrafts.common.capability;

import com.onepunchcrafts.constant.NbtBooleanValues;
import lombok.Data;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.*;

@Data
public class OnePunchPlayer {

    private boolean isSaitama;
    private int actualAbility;

    public OnePunchPlayer(boolean isSaitama) {
        this.isSaitama = isSaitama;
    }

    public Tag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean(NbtBooleanValues.isSaitama.getValue(), this.isSaitama);
        nbt.putInt("actualability", this.actualAbility);
        return nbt;
    }

    public void readNBT(Tag tag) {
        CompoundTag nbt = (CompoundTag) tag;
        this.isSaitama = nbt.getBoolean(NbtBooleanValues.isSaitama.getValue());
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
        return changed;
    }

    public void setActualAbility(int actualAbility) {
        if (actualAbility >= 6 || actualAbility < 0)
            return;
        this.actualAbility = actualAbility;
    }
}
