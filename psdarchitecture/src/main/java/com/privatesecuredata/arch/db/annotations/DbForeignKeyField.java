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
public @interface DbForeignKeyField {
	/**
	 * If this is true, then a foreign key constraint is added 
	 * otherwise only a field is added
	 * 
	 * @return true if the field is mandatory
	 */
	boolean isMandatory() default true;
	Class<?> foreignType();
}