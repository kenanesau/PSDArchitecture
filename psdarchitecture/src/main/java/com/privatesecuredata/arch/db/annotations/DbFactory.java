package com.privatesecuredata.arch.db.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking the extends-relationship to the father-class
 *
 * Created by kenan on 3/13/15.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbFactory {
    Class factoryType();
}
