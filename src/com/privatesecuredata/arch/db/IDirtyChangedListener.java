package com.privatesecuredata.arch.db;

public interface IDirtyChangedListener {
	public void onDirtyChanged(DbId<?> id);
	public void removeFromDirtyList(DbId<?> id);
}