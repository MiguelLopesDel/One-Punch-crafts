package com.onepunchcrafts.common.skills.sync;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Objects;

public class SyncableField {
    private final MethodHandle getter;
    private final MethodHandle setter;
    private final String key;
    private final SyncStrategy strategy;

    public SyncableField(Field field, Syncable annotation) {
        this.key = annotation.key();
        this.strategy = annotation.strategy();

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            this.getter = lookup.unreflectGetter(field);
            this.setter = lookup.unreflectSetter(field);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Falha ao criar MethodHandles para campo: " + field.getName(), e);
        }
    }

    public boolean isDifferent(Object obj1, Object obj2) {
        try {
            Object val1 = getter.invoke(obj1);
            Object val2 = getter.invoke(obj2);
            return !Objects.equals(val1, val2);
        } catch (Throwable t) {
            throw new RuntimeException("Erro ao comparar campo: " + key, t);
        }
    }

    public Object getValue(Object obj) {
        try {
            return getter.invoke(obj);
        } catch (Throwable t) {
            throw new RuntimeException("Erro ao obter valor do campo: " + key, t);
        }
    }

    public void setValue(Object obj, Object value) {
        try {
            setter.invoke(obj, value);
        } catch (Throwable t) {
            throw new RuntimeException("Erro ao definir valor do campo: " + key, t);
        }
    }

    public String getKey() {
        return key;
    }

    public SyncStrategy getStrategy() {
        return strategy;
    }
}