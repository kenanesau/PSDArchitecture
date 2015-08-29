package com.privatesecuredata.arch.db.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add field to the type which is referenced by object A
 * (Object A references a list of Object B)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)

public @interface DbMultipleForeignKeyFields {
    /**
     *
     * @return
     */
    DbForeignKeyField[] value();
}