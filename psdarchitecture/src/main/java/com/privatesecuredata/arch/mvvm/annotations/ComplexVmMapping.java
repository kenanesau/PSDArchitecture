package com.privatesecuredata.arch.mvvm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ComplexVmMapping {
	Class<?> vmType();
    Class<?> vmProviderType() default Object.class;
	boolean loadLazy() default true;
}