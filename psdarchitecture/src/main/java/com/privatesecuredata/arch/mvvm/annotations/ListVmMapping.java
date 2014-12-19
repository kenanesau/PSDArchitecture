package com.privatesecuredata.arch.mvvm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ListVmMapping {
    Class<?> parentType();
	Class<?> modelType();
	Class<?> vmType();
	boolean loadLazy() default true;
}