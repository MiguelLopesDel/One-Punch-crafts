package com.onepunchcrafts.common.skills.sync;

import com.onepunchcrafts.common.skills.sync.processor.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SyncHandler {
    private static final Map<SyncStrategy, SyncProcessor> processors = Map.of(
            SyncStrategy.SIMPLE, new SimpleProcessor(),
            SyncStrategy.VALIDATED, new ValidatedProcessor(),
            SyncStrategy.TOGGLE, new ToggleProcessor(),
            SyncStrategy.SKILL_INDEX, new SkillIndexProcessor(),
            SyncStrategy.GROUP_INDEX, new GroupIndexProcessor()
    );

    public static void handleDifferences(ServerPlayer player, ArrayList<String> differences,
                                         SyncableSkillPack serverData, SyncableSkillPack clientData) {

        List<SyncableField> fields = FieldCache.getCachedFields(serverData.getClass());
        Map<String, SyncableField> fieldMap = fields.stream()
                .collect(java.util.stream.Collectors.toMap(SyncableField::getKey, f -> f));

        differences.forEach(fieldKey -> {
            SyncableField field = fieldMap.get(fieldKey);
            if (field != null) {
                SyncProcessor processor = processors.get(field.getStrategy());
                if (processor != null) {
                    FieldDescriptorAdapter adapter = new FieldDescriptorAdapter(field);
                    processor.process(player, fieldKey, serverData, clientData, adapter);
                }
            }
        });
    }

    private static class FieldDescriptorAdapter implements com.onepunchcrafts.common.skills.sync.FieldRegistry.FieldDescriptor {
        private final SyncableField field;

        public FieldDescriptorAdapter(SyncableField field) {
            this.field = field;
        }

        @Override
        public Object getValue(SyncableSkillPack obj) {
            return field.getValue(obj);
        }

        @Override
        public void setValue(SyncableSkillPack obj, Object value) {
            field.setValue(obj, value);
        }

        @Override
        public SyncStrategy getStrategy() {
            return field.getStrategy();
        }

        @Override
        public boolean isDifferent(SyncableSkillPack obj1, SyncableSkillPack obj2) {
            return field.isDifferent(obj1, obj2);
        }
    }
}