package com.privatesecuredata.arch.db;

import android.database.Cursor;

public interface ICursorLoader {
    Cursor getCursor(DbId<?> foreignKey);
    Cursor getCursor(DbId<?> foreignKey, OrderByTerm... orderByTerms);
}
