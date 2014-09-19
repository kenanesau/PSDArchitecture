package com.privatesecuredata.arch.db;

import java.util.Collection;
import android.database.Cursor;

public interface ILoadCollection<T> {
	Cursor getLoadAllCursor() throws Exception;
	Collection<T> loadAll() throws Exception;
}
