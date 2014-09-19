package com.privatesecuredata.arch.db;

import java.lang.annotation.*;

/**
 * 
 * @author Kenan Esau
 *
 * Annotation which marks a persister and tells the PersistanceManager 
 * which IPersistable-implementation is actually persisted by the Persister.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Persister {
	Class<?> persists();
}