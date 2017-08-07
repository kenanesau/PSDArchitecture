package com.privatesecuredata.arch.db.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for "normal" object relations (reference from object a to object b)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbThisToOne {
	boolean isMandatory() default false;
    boolean isComposition() default false;
    boolean deleteChildren() default true;
}