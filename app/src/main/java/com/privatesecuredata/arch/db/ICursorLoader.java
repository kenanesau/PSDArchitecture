package com.privatesecuredata.arch.db;

import android.database.Cursor;

public interface ICursorLoader {
	Cursor getCursor(IPersistable<?> param);
}
