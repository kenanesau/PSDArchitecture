package com.privatesecuredata.arch.db;

/**
 * Created by kenan on 12/16/14.
 */
public interface ICursorChangedProvider {
    boolean addCursorChangedListener(ICursorChangedListener listener);
    boolean removeCursorChangedListener(ICursorChangedListener listener);
}
