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
    private short speed;
    @Setter
    @Getter
    private boolean breakBlocksQuickly;
    private int actualAbility;
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

    public OnePunchPlayer(boolean isSaitama) {
        this.isSaitama = isSaitama;
    }

    public Tag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean(NbtBooleanValues.isSaitama.getValue(), this.isSaitama);
        nbt.putBoolean(NbtBooleanValues.seriousFartActive.getValue(), this.seriousFartActive);
        nbt.putBoolean(NbtBooleanValues.breakBlocksQuickly.getValue(), this.isBreakBlocksQuickly());
        nbt.putInt("actualability", this.actualAbility);
        nbt.putShort("weight", this.weight);
        nbt.putShort(NbtBooleanValues.superSpeed.getValue(), this.speed);
        nbt.putShort("saitamaknockbackresistance", this.knockbackResistance);
        nbt.putShort("saitamaattackknockback", this.attackKnockback);
        nbt.putShort("saitamaswimspeed", this.swimSpeed);
        return nbt;
    }

    public void readNBT(Tag tag) {
        CompoundTag nbt = (CompoundTag) tag;
        this.isSaitama = nbt.getBoolean(NbtBooleanValues.isSaitama.getValue());
        this.seriousFartActive = nbt.getBoolean(NbtBooleanValues.seriousFartActive.getValue());
        this.breakBlocksQuickly = nbt.getBoolean(NbtBooleanValues.breakBlocksQuickly.getValue());
        this.actualAbility = nbt.getInt("actualability");
        this.weight = nbt.getShort("weight");
        this.speed = nbt.getShort(NbtBooleanValues.superSpeed.getValue());
        this.knockbackResistance = nbt.getShort("saitamaknockbackresistance");
        this.attackKnockback = nbt.getShort("saitamaattackknockback");
        this.swimSpeed = nbt.getShort("saitamaswimspeed");
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
        if (this.speed != otherData.speed) {
            changed.add(NbtBooleanValues.superSpeed.getValue());
        }
        if (this.breakBlocksQuickly != otherData.breakBlocksQuickly) {
            changed.add(NbtBooleanValues.breakBlocksQuickly.getValue());
        }
        if (this.getWeight() != otherData.getWeight()) {
            changed.add("weight");
        }
        if (this.getKnockbackResistance() != otherData.getKnockbackResistance()) {
            changed.add("saitamaknockbackresistance");
        }
        if (this.getAttackKnockback() != otherData.getAttackKnockback()) {
            changed.add("saitamaattackknockback");
        }
        if (this.getSwimSpeed() != otherData.getSwimSpeed()) {
            changed.add("saitamaswimspeed");
        }
        return changed;
    }

    public void setActualAbility(int actualAbility) {
        this.actualAbility = (actualAbility > getMaxNumSkill()) ? 0 : (actualAbility < 0) ? getMaxNumSkill() : actualAbility;
    }

    public static int getMaxNumSkill(){
        return 11;
    }
}
