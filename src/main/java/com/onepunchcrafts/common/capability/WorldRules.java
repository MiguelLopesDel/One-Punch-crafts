package com.onepunchcrafts.common.capability;

import lombok.Getter;
import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class WorldRules {
    private final List<Double> maxStrength = new ArrayList<>(Arrays.asList(12D, 30D, 12D));

    public void changeStrength(List<Double> doubles) {
        if (doubles.size() == 3) {
            maxStrength.clear();
            maxStrength.addAll(doubles);
        }
    }

    public Tag writeNBT() {
        if (maxStrength.isEmpty())
            maxStrength.addAll(Arrays.asList(12D, 30D, 12D));
        CompoundTag tag = new CompoundTag();
        ListTag tags = new ListTag();
        maxStrength.forEach(n -> tags.add(DoubleTag.valueOf(n)));
        tag.put("maxStrength", tags);
        return tag;
    }

    public void readNBT(Tag nbt) {
        if (nbt instanceof CompoundTag tag) {
            maxStrength.clear();
            ListTag maxStrength1 = tag.getList("maxStrength", 6);
            maxStrength1.forEach(n -> maxStrength.add(((DoubleTag) n).getAsDouble()));
        }
    }

    public void resetMaxStrength() {
        maxStrength.clear();
        maxStrength.addAll(Arrays.asList(12D, 30D, 12D));
    }
}
