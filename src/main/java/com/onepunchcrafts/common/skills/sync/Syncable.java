package com.onepunchcrafts.common.skills.sync;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Syncable {
    String key();
    SyncStrategy strategy() default SyncStrategy.SIMPLE;
    boolean requiresValidation() default false;
    boolean requiresNetworkSync() default false;
}

