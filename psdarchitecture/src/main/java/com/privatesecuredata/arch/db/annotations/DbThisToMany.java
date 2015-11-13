package com.privatesecuredata.arch.db.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for referencing a list of other objects
 * (Object A references a list of objects of type B)
 *
 * @see com.privatesecuredata.arch.db.annotations.DbForeignKeyField
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbThisToMany {
	boolean isMandatory() default false;
    boolean deleteChildren() default true;
	Class<?> referencedType();
}