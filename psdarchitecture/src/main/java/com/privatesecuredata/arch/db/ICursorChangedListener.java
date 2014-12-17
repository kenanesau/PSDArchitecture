package com.privatesecuredata.arch.db;

import android.database.Cursor;

/**
 * Created by kenan on 12/16/14.
 */
public interface ICursorChangedListener {
    void notifyCursorChanged(Cursor csr);
}