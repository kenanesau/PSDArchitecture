package com.privatesecuredata.arch.db;

public interface IDirtyChangedProvider {
	public void addDirtyChangedListener(IDirtyChangedListener listener);
}

