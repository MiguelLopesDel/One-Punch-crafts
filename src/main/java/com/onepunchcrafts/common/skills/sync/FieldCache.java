package com.onepunchcrafts.common.skills.sync;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FieldCache {
    private static final Map<Class<?>, List<SyncableField>> FIELD_CACHE = new ConcurrentHashMap<>();

    public static List<SyncableField> getCachedFields(Class<?> clazz) {
        FIELD_CACHE.clear();
        return FIELD_CACHE.computeIfAbsent(clazz, FieldCache::extractFields);
    }

    private static List<SyncableField> extractFields(Class<?> clazz) {
        List<SyncableField> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.isAnnotationPresent(Syncable.class)) {
                    f.setAccessible(true);
                    fields.add(new SyncableField(f, f.getAnnotation(Syncable.class)));
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

//    private static List<SyncableField> extractFields(Class<?> clazz) {
//        return Arrays.stream(clazz.getDeclaredFields())
//                .filter(f -> f.isAnnotationPresent(Syncable.class))
//                .map(f -> {
//                    f.setAccessible(true);
//                    return new SyncableField(f, f.getAnnotation(Syncable.class));
//                })
//                .toList();
//    }
}