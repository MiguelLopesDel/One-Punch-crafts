package com.onepunchcrafts.common.skills.sync;

import java.util.ArrayList;
import java.util.List;

public class FieldComparator {

    public static ArrayList<String> compareFields(SyncableSkillPack obj1, SyncableSkillPack obj2) {
        ArrayList<String> differences = new ArrayList<>();

        List<SyncableField> fields = FieldCache.getCachedFields(obj1.getClass());

        for (SyncableField field : fields) {
            if (field.isDifferent(obj1, obj2)) {
                differences.add(field.getKey());
            }
        }

        return differences;
    }
}