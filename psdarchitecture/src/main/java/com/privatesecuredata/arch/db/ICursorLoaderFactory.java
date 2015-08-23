package com.privatesecuredata.arch.db;

/**
 * ICursorLoaderFactory is needed so that you can register a CursorLoader during the init-phase.
 * The actual ICursorLoader is the added by the PersistanceManager at the very end of the
 * initialization.
 *
 * e.g. IdCursorLoader needs the Persister but the IPersister is not yet registered during init...
 */
public interface ICursorLoaderFactory {
    public ICursorLoader create();
    public Class getReferencingType();
    public Class getReferencedType();
}
