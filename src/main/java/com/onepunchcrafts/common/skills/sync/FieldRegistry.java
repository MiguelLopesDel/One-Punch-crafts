//
//package com.onepunchcrafts.common.skills.sync;
//
//import com.onepunchcrafts.common.skills.sync.processor.*;
//import net.minecraft.server.level.ServerPlayer;
//
//import java.util.*;
//import java.util.function.BiConsumer;
//import java.util.function.Function;
//
//public class FieldRegistry {
//    private final Map<String, FieldDescriptorImpl> fields = new HashMap<>();
//    private static final Map<SyncStrategy, SyncProcessor> processors = Map.of(
//            SyncStrategy.SIMPLE, new SimpleProcessor(),
//            SyncStrategy.VALIDATED, new ValidatedProcessor(),
//            SyncStrategy.TOGGLE, new ToggleProcessor(),
//            SyncStrategy.SKILL_INDEX, new SkillIndexProcessor(),
//            SyncStrategy.GROUP_INDEX, new GroupIndexProcessor()
//    );
//
//    public <T> void register(String key, Function<SyncableSkillPack, T> getter,
//                             BiConsumer<SyncableSkillPack, T> setter, SyncStrategy strategy) {
//        fields.put(key, new FieldDescriptorImpl<>(getter, setter, strategy));
//    }
//
//    public ArrayList<String> compareFields(SyncableSkillPack obj1, SyncableSkillPack obj2) {
//        ArrayList<String> differences = new ArrayList<>();
//        for (Map.Entry<String, FieldDescriptorImpl> entry : fields.entrySet()) {
//            if (entry.getValue().isDifferent(obj1, obj2)) {
//                differences.add(entry.getKey());
//            }
//        }
//        return differences;
//    }
//
//    public void handleDifferences(ServerPlayer player, ArrayList<String> differences,
//                                  SyncableSkillPack serverData, SyncableSkillPack clientData) {
//        differences.forEach(fieldKey -> {
//            FieldDescriptorImpl descriptor = fields.get(fieldKey);
//            if (descriptor != null) {
//                SyncProcessor processor = processors.get(descriptor.getStrategy());
//                if (processor != null) {
//                    processor.process(player, fieldKey, serverData, clientData, descriptor);
//                }
//            }
//        });
//    }
//
//    public interface FieldDescriptor {
//        Object getValue(SyncableSkillPack obj);
//        void setValue(SyncableSkillPack obj, Object value);
//        SyncStrategy getStrategy();
//        boolean isDifferent(SyncableSkillPack obj1, SyncableSkillPack obj2);
//    }
//
//    private static class FieldDescriptorImpl<T> implements FieldDescriptor {
//        private final Function<SyncableSkillPack, T> getter;
//        private final BiConsumer<SyncableSkillPack, T> setter;
//        private final SyncStrategy strategy;
//
//        public FieldDescriptorImpl(Function<SyncableSkillPack, T> getter,
//                                   BiConsumer<SyncableSkillPack, T> setter,
//                                   SyncStrategy strategy) {
//            this.getter = getter;
//            this.setter = setter;
//            this.strategy = strategy;
//        }
//
//        @Override
//        public boolean isDifferent(SyncableSkillPack obj1, SyncableSkillPack obj2) {
//            T val1 = getter.apply(obj1);
//            T val2 = getter.apply(obj2);
//            return !Objects.equals(val1, val2);
//        }
//
//        @Override
//        public Object getValue(SyncableSkillPack obj) {
//            return getter.apply(obj);
//        }
//
//        @Override
//        public void setValue(SyncableSkillPack obj, Object value) {
//            setter.accept(obj, (T) value);
//        }
//
//        @Override
//        public SyncStrategy getStrategy() {
//            return strategy;
//        }
//    }
//}